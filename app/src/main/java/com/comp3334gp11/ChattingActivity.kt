package com.comp3334gp11

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

class ChattingActivity : AppCompatActivity() {
    private val messagesAdapter = GroupAdapter<GroupieViewHolder>()
    private var chatID = ""
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

        chatting_messages.adapter = messagesAdapter

        getChatID(fromID, toUser.uid)

        chatting_sendButton.setOnClickListener {
            val text = chatting_enterMassage.text.toString()//.trim()
            if (text.isNotEmpty())
                sendMessage(fromID, text, toUser.uid, toUser.userPicUrl, toUser.username)
            chatting_enterMassage.text.clear()
            chatting_messages.scrollToPosition(messagesAdapter.itemCount - 1)
        }
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
                }
                if (!foundFrom || !foundTo) {
                    Log.e("ChattingActivity", "new chat room")
                    val newRoomRef = FirebaseDatabase.getInstance().getReference("/chat_room").push()
                    val users =  mutableListOf(fromID, toID)
                    cID = newRoomRef.key.toString()
                    val chatRoom = ChatRoom(cID, users)
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
                val chatMessage = p0.getValue(ChatWithUser::class.java)
                if (chatMessage != null) {
                    if (chatMessage.fromID == FirebaseAuth.getInstance().uid && chatMessage.toID == toID) {
                        messagesAdapter.add(MessageToItem(chatMessage.text))
                    }
                    else if (chatMessage.fromID == toID && chatMessage.toID == FirebaseAuth.getInstance().uid) {
                        messagesAdapter.add(MessageFromItem(chatMessage.text))
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
        val chatWithUser = ChatWithUser(messageKey, text, fromID, toID, time)
        messageRef.setValue(chatWithUser).addOnSuccessListener {
            Log.e("ChattingActivity", "message sent")
            updateLatestMessages(fromID, text, toID, toUserPicUrl, toUsername, time)
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
                            val receiverLatestMessagesRef = FirebaseDatabase.getInstance().getReference("/latest_messages/$toID/$chatID")
                            receiverLatestMessagesRef.removeValue()
                            val receiver = ChatMessages(fromID, fromUserPicUrl, fromUsername, text, time)
                            receiverLatestMessagesRef.setValue(receiver).addOnSuccessListener {
                                Log.e("ChattingActivity", "receiever latest messages updated")
                            }.addOnFailureListener {
                                Log.e("ChattingActivity", "Error: ${it.message}")
                            }
                            break
                        }
                    }
                }
                val senderLatestMessagesRef = FirebaseDatabase.getInstance().getReference("/latest_messages/$fromID/$chatID")
                senderLatestMessagesRef.removeValue()
                val sender = ChatMessages(toID, toUserPicUrl, toUsername, text, time)
                senderLatestMessagesRef.setValue(sender).addOnSuccessListener {
                    Log.e("ChattingActivity", "sender latest messages updated")
                }.addOnFailureListener {
                    Log.e("ChattingActivity", "Error: ${it.message}")
                }
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