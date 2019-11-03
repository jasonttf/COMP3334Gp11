package com.comp3334gp11

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chatting.*
import kotlinx.android.synthetic.main.from_message.view.*
import kotlinx.android.synthetic.main.to_messaage.view.*

class ChattingActivity : AppCompatActivity() {
    private val messagesAdapter = GroupAdapter<GroupieViewHolder>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatting)

        val toUser = intent.getParcelableExtra<User>(ContactActivity.USER)
        if (toUser == null) {
            finish()
            Toast.makeText(this, "Cannot find user", Toast.LENGTH_SHORT).show()
        }
        supportActionBar?.title = toUser.username

        chatting_messages.adapter = messagesAdapter

        getMessages(toUser.uid)

        chatting_sendButton.setOnClickListener {
            val text = chatting_enterMassage.text.toString()//.trim()
            if (text.isNotEmpty())
                sendMessage(text, toUser.uid)
            chatting_enterMassage.text.clear()
        }
    }

    private fun getMessages(toID: String) {
        val messagesRef = FirebaseDatabase.getInstance().getReference("/messages")
        messagesRef.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)
                if (chatMessage != null) {
                    if (chatMessage.fromID == FirebaseAuth.getInstance().uid && chatMessage.toID == toID) {
                        messagesAdapter.add(MessageToItem(chatMessage.text))
                    }
                    else if (chatMessage.fromID == toID && chatMessage.toID == FirebaseAuth.getInstance().uid) {
                        messagesAdapter.add(MessageFromItem(chatMessage.text))
                    }
                }
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

    private fun sendMessage(text: String, toID: String){
        val messageRef = FirebaseDatabase.getInstance().getReference("/messages").push()
        val messageKey = messageRef.key
        val fromID = FirebaseAuth.getInstance().uid
        if (messageKey == null || fromID == null)
            return
        val chatMessage = ChatMessage(messageKey, text, fromID, toID, System.currentTimeMillis())
        messageRef.setValue(chatMessage).addOnSuccessListener {
            Log.e("ChattingActivity", "message sent")
        }.addOnFailureListener {
            Log.e("ChattingActivity", "Error: ${it.message}")
        }
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