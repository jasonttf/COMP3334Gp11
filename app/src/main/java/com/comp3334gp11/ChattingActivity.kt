package com.comp3334gp11

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64.encodeToString
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chatting.*
import kotlinx.android.synthetic.main.from_message.view.*
import kotlinx.android.synthetic.main.to_messaage.view.*
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class ChattingActivity : AppCompatActivity() {
    private val messagesAdapter = GroupAdapter<GroupieViewHolder>()
    private var chatID = ""
    private var toUserD: Int = 0
    private var toUserN: Int = 0
    private var toUserE: Int = 0
    private var fromUserD: Int = 0
    private var fromUserN: Int = 0
    private var seckey = SecretKeySpec(ByteArray(1), "HMACSHA256")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatting)

        val toUser = intent.getParcelableExtra<User>(ContactActivity.USER)
        val fromID = FirebaseAuth.getInstance().uid?: ""
        if (toUser == null) {
            finish()
            Toast.makeText(this, "Cannot find user", Toast.LENGTH_SHORT).show()
        }
        supportActionBar?.title =  toUser.username
        toUserD = toUser.d
        toUserE = toUser.e
        toUserN = toUser.n

        chatting_messages.adapter = messagesAdapter

        getKey(fromID, toUser.uid)

        chatting_sendButton.setOnClickListener {
            val text = chatting_enterMassage.text.toString()//.trim()
            if (text.isNotEmpty())
                sendMessage(fromID, text, toUser.uid, toUser.userPicUrl, toUser.username)
            chatting_enterMassage.text.clear()
            chatting_messages.scrollToPosition(messagesAdapter.itemCount - 1)
        }
    }

    private fun getKey(fromID: String, toID: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("/users")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("ChattingActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (i in p0.children) {
                    val temp = i.getValue(User::class.java)
                    if (temp != null) {
                        if (temp.uid == fromID) {
                            fromUserN = temp.n
                            fromUserD = temp.d
                            break
                        }
                    }
                }
                getChatID(fromID, toID)
            }
        })
    }

    private fun getChatID(fromID: String, toID: String) {
        val chatRef = FirebaseDatabase.getInstance().getReference("/chat_room")
        var cID = ""
        var foundFrom = false
        var foundTo = false
        val users =  mutableListOf(fromID, toID)
        chatRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("ChattingActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
               for (child in p0.children) {
                    val chatRoom = child.getValue(ChatRoom::class.java)
                    if (chatRoom != null) {
                        for (i in chatRoom.users) {
                            if (i == fromID)
                                foundFrom = true
                            if (i == toID)
                                foundTo = true
                            if (foundFrom && foundTo) {
                                cID = chatRoom.chatID
                                chatID = cID
                                break
                            }
                        }
                    }
                   if (foundFrom && foundTo)
                       break
                   Log.e("df", chatID)
                }
                if (!foundFrom || !foundTo) {
                    Log.e("ChattingActivity", "new chat room: $cID")
                    val newRoomRef = FirebaseDatabase.getInstance().getReference("/chat_room").push()
                    val users =  mutableListOf(fromID, toID)
                    cID = newRoomRef.key.toString()
                    chatID = cID
                    var kgen = KeyGenerator.getInstance("HMACSHA256")
                    var skey = kgen.generateKey()
                    var tempKey = skey.getEncoded()
                    var keyString = tempKey.toString()
                    val chatRoom = ChatRoom(cID, users, keyString)
                    Log.e("MACTag",  "New Gen" + keyString)
                    Log.e("ChattingActivity", chatID)
                    newRoomRef.setValue(chatRoom)
                }
                getMessages(cID, toID)
            }
        })
    }

    private fun getMessages(cID:String, toID: String) {
        val messagesFromRef = FirebaseDatabase.getInstance().getReference("/messages/$cID")
        messagesFromRef.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val messageEncrypt = MessageEncrypt()
                val chatMessage = p0.getValue(ChatWithUser::class.java)
                if (chatMessage != null) {
                    var message = chatMessage.text
                    if (chatMessage.fromID == FirebaseAuth.getInstance().uid && chatMessage.toID == toID) {
                        message = messageEncrypt.decryption(message, toUserD, toUserN)
                        messagesAdapter.add(MessageToItem(message))
                    }
                    else if (chatMessage.fromID == toID && chatMessage.toID == FirebaseAuth.getInstance().uid) {
                        message = messageEncrypt.decryption(message, fromUserD, fromUserN)
                        // MAC Tag verification
                        val mac = Mac.getInstance("HMACSHA256")
                        mac.init(seckey)
                        var tempBA = chatMessage.text.toByteArray()
                        var temp2 = encodeToString(mac.doFinal(tempBA), 0)

                        if(temp2.equals(chatMessage.macKey))
                            messagesAdapter.add(MessageFromItem(message))
                        else {
                            messagesAdapter.add(MessageFromItem("* This message has been modified *"))
                            Log.e("Verify","macKey of Chatroom" + seckey.toString())
                        }
                    }
                }
                chatting_messages.scrollToPosition(messagesAdapter.itemCount - 1)
            }

            override fun onCancelled(p0: DatabaseError) {
                Log.e("ChattingActivity", "Error: ${p0.message}")
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                Log.e("ChattingActivity", "DataSnapshot: $p0, String: $p1")
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                Log.e("ChattingActivity", "DataSnapshot: $p0, String: $p1")
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                Log.e("ChattingActivity", "DataSnapshot: $p0")
            }
        })
    }

    private fun sendMessage(fromID: String, text: String, toID: String, toUserPicUrl: String, toUsername: String) {
        val messageRef = FirebaseDatabase.getInstance().getReference("/messages/$chatID").push()
        val messageKey = messageRef.key ?: return
        val time = System.currentTimeMillis()
        val messageEncrypt = MessageEncrypt()
        val message = messageEncrypt.encryption(text, toUserE, toUserN)

        // MAC Tag generating for each message
        val mac = Mac.getInstance("HMACSHA256")
        mac.init(seckey)
        var bytesArr = message.toByteArray()
        var macRes = encodeToString(mac.doFinal(bytesArr), 0)

        val chatWithUser = ChatWithUser(messageKey, message, fromID, toID, time, macRes)
        messageRef.setValue(chatWithUser).addOnSuccessListener {
            Log.e("ChattingActivity", "message sent")
            Log.e("ChattingActivity", macRes)
            updateLatestMessages(fromID, message, toID, toUserPicUrl, toUsername, time)
        }.addOnFailureListener {
            Log.e("ChattingActivity", "Error: ${it.message}")
        }
    }

    private fun updateLatestMessages(fromID: String, text: String, toID: String, toUserPicUrl: String, toUsername: String, time: Long) {
        val fromUserRef = FirebaseDatabase.getInstance().getReference("/users")
        fromUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("ChattingActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (i in p0.children) {
                    val fromUser = i.getValue(User::class.java)
                    if (fromUser != null) {
                        if (fromUser.uid == fromID) {
                            val fromUserPicUrl = fromUser.userPicUrl
                            val fromUsername = fromUser.username
                            val receiverLatestMessagesRef = FirebaseDatabase.getInstance().getReference("/latest_messages/$toID")
                            receiverLatestMessagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onCancelled(p0: DatabaseError) {
                                    Log.e("ChattingActivity", "Error: $p0")
                                }

                                override fun onDataChange(p0: DataSnapshot) {
                                    var receiverlatestMessages = mutableListOf<ChatMessages>()
                                    p0.children.forEach {
                                        val temp = it.getValue(ChatMessages::class.java)
                                        if (temp != null) {
                                            receiverlatestMessages.add(temp)
                                        }
                                    }
                                    for (l in receiverlatestMessages) {
                                        if (l.chatID == chatID) {
                                            receiverlatestMessages.remove(l)
                                            break
                                        }
                                    }
                                    val receiver = ChatMessages(chatID, fromID, fromUserPicUrl, fromUsername, text, time, fromID, toID)
                                    receiverlatestMessages.add(receiver)
                                    receiverLatestMessagesRef.setValue(receiverlatestMessages).addOnSuccessListener {
                                        Log.e("ChattingActivity", "receiver latest messages updated")
                                    }.addOnFailureListener {
                                        Log.e("ChattingActivity", "Error: ${it.message}")
                                    }
                                }
                            })
                            break
                        }
                    }
                }
                val senderLatestMessagesRef = FirebaseDatabase.getInstance().getReference("/latest_messages/$fromID")
                senderLatestMessagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        Log.e("ChattingActivity", "Error: $p0")
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        var senderlatestMessages = mutableListOf<ChatMessages>()
                        p0.children.forEach {
                            val temp = it.getValue(ChatMessages::class.java)
                            if (temp != null) {
                                senderlatestMessages.add(temp)
                            }
                        }
                        for (l in senderlatestMessages) {
                            Log.e("dd", l   .chatID)
                            Log.e("ss", chatID)
                            if (l.chatID == chatID) {
                                senderlatestMessages.remove(l)
                                break
                            }
                        }
                        val sender = ChatMessages(chatID, toID, toUserPicUrl, toUsername, text, time, fromID, toID)
                        senderlatestMessages.add(sender)
                        Log.e("s", senderlatestMessages.size.toString())
                        senderLatestMessagesRef.setValue(senderlatestMessages).addOnSuccessListener {
                            Log.e("ChattingActivity", "sender latest messages updated")
                        }.addOnFailureListener {
                            Log.e("ChattingActivity", "Error: ${it.message}")
                        }
                    }
                })
            }
        })
    }
}
 class MessageToItem(val message: String): Item<GroupieViewHolder>() {
     override fun getLayout(): Int {
         return R.layout.to_messaage
     }

     override fun bind(viewHolder: GroupieViewHolder, position: Int) {
         viewHolder.itemView.chatting_toMessage.text = message
     }

 }

class MessageFromItem(val message: String): Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.from_message
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.chatting_fromMessage.text = message
    }

}