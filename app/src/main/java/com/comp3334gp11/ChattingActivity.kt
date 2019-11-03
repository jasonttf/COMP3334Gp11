package com.comp3334gp11

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item

class ChattingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatting)

        val toUser = intent.getParcelableExtra<User>(ContactActivity.USER)
        supportActionBar?.title = toUser.username

        showMessages()
    }

    private fun showMessages() {
        val messagesAdapter = GroupAdapter<GroupieViewHolder>()

    }
}
 class MessageToItem: Item<GroupieViewHolder>() {
     override fun getLayout(): Int {
         TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
         return R.layout.to_messaage
     }

     override fun bind(viewHolder: GroupieViewHolder, position: Int) {
         TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
     }

 }

class MessageFromItem: Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return R.layout.from_message
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}