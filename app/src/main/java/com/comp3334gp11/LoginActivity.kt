package com.comp3334gp11

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_loginButton.setOnClickListener {
            getTime()
        }

        login_toRegister.setOnClickListener {
            finish()
        }
    }

    private fun getTime() {
        val email = login_email.text.toString()
        var password = login_password.text.toString()
        var time: Long = -1
        val getUserInfo = FirebaseDatabase.getInstance().getReference("/users")
        getUserInfo.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("LoginActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (i in p0.children) {
                    val temp = i.getValue(User::class.java)
                    if (temp != null) {
                        if (temp.email == email) {
                            time = temp.time.toLong()
                            break
                        }
                    }
                }
                val passwordSha = PasswordSha()

                password = passwordSha.shaOnce(password, time)
                login(email, password)
            }
        })
    }
    private fun login(email: String, password: String) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Login successfully", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MessengerActivity::class.java))
            }
        }.addOnFailureListener {
            Log.e("LoginActivity", "Error: ${it.message}")
        }
    }
}