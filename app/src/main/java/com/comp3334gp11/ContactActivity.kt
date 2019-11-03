package com.comp3334gp11

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.android.synthetic.main.user_row.view.*

class ContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        supportActionBar?.title = "Select User"
        showContact()
    }

    companion object {
        const val USER = "USER"
    }

    private fun showContact() {
        val contactAdapter = GroupAdapter<GroupieViewHolder>()
        val usersRef = FirebaseDatabase.getInstance().getReference("/users")
        usersRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("ContactActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach {
                    val user = it.getValue(User::class.java)
                    if (user != null) {
                        if (user.uid  != FirebaseAuth.getInstance().uid)
                            contactAdapter.add(UsersItem(user))
                    }
                }
                contactAdapter.setOnItemClickListener { item, view ->
                    val selected = item as UsersItem
                    val intent = Intent(view.context, ChattingActivity::class.java)
                    intent.putExtra(USER, selected.user)
                    startActivity(intent)
                    finish()
                }
            }
        })
        contact_allUser.adapter = contactAdapter
    }
}

class UsersItem(val user: User):Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.user_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if (user.userPicUrl.isNotEmpty())
            Picasso.get().load(user.userPicUrl).into(viewHolder.itemView.userRow_userPic)
        viewHolder.itemView.userRow_username.text = user.username
    }

}


