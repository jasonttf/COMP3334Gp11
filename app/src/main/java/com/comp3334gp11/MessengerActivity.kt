package com.comp3334gp11

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_messenger.*
import kotlinx.android.synthetic.main.chat_row.view.*
import java.io.File
import java.text.SimpleDateFormat

class MessengerActivity : AppCompatActivity() {
    private var fromUserD: Int = 0
    private var fromUserN: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        verifyUserStatus()
        FirebaseAuth.getInstance().uid?.let { getKey(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.navbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_newChat -> {
                startActivity(Intent(this, ContactActivity::class.java))
            }
            R.id.menu_signOut -> {
                FirebaseAuth.getInstance().signOut()
                verifyUserStatus()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun verifyUserStatus() {
        val uid = FirebaseAuth.getInstance().uid
        if (uid == null) {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun getKey(userID: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("/users")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("ChattingActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach {
                    val temp = it.getValue(User::class.java)
                    if (temp != null) {
                        if (temp.uid == userID) {
                            fromUserN = temp.n
                        }
                    }
                }
                val fileName = filesDir.path + "key.txt"
                fromUserD = File(fileName).readText().trim().toInt()
                showChats(userID)
            }
        })
    }

    private fun showChats(userID: String) {
        val chatMessageAdapter = GroupAdapter<GroupieViewHolder>()
        val messageRef = FirebaseDatabase.getInstance().getReference("/latest_messages/$userID")
        messageRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("MessengerActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
                var allChatMessages = mutableListOf<ChatMessages>()
                p0.children.forEach {
                    Log.e("messenger", it.toString())
                    val temp = it.getValue(ChatMessages::class.java)
                    if (temp != null) {
                        val messageEncrypt = MessageEncrypt()
                        var message = temp.message
                        //message = messageEncrypt.decryption(message, fromUserD, fromUserN)
                        val decryptedChatMessage = ChatMessages(temp.chatID, temp.userID, temp.userPicUrl, temp.username, message, temp.time)
                        allChatMessages.add(decryptedChatMessage)
                    }
                }
                for (i in allChatMessages.size-1 downTo 0)
                    chatMessageAdapter.add(ChatRow(allChatMessages[i]))
                chatMessageAdapter.setOnItemClickListener { item, view ->
                    val selected = item as ChatRow
                    val toUserID = selected.chatMessage.userID
                    val toUserRef = FirebaseDatabase.getInstance().getReference("/users")
                    toUserRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                            Log.e("MessengerActivity", "Error: $p0")
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            for (i in p0.children) {
                                val user = i.getValue(User::class.java)
                                if (user != null) {
                                    if (user.uid == toUserID) {
                                        val intent = Intent(view.context, ChattingActivity::class.java)
                                        intent.putExtra(ContactActivity.USER, user)
                                        startActivity(intent)
                                    }
                                }
                            }
                        }
                    })
                }
            }
        })
        messenger_allMessage.adapter = chatMessageAdapter
    }
}

class ChatRow(val chatMessage: ChatMessages):Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_row
    }

    @SuppressLint("SimpleDateFormat")
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if (chatMessage.userPicUrl.isNotEmpty())
            Picasso.get().load(chatMessage.userPicUrl).into(viewHolder.itemView.chatRow_userPic)
        viewHolder.itemView.chatRow_username.text = chatMessage.username
        viewHolder.itemView.chatRow_text.text = chatMessage.message
        viewHolder.itemView.chatRow_time.text = SimpleDateFormat("yyyy.MM.dd HH:mm").format(chatMessage.time)
    }

}