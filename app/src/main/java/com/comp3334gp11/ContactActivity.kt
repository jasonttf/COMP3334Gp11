package com.comp3334gp11

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

    private fun showContact() {
        val adapter = GroupAdapter<GroupieViewHolder>()
        val usersRef = FirebaseDatabase.getInstance().getReference("/users")
        usersRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("ContactActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach {
                    val user = it.getValue(User::class.java)
                    Log.e("ContactActivity", user.toString())
                    if (user != null) {
                        adapter.add(UsersItem(user))
                    }
                }
            }
        })
        contact_allUser.adapter = adapter
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


