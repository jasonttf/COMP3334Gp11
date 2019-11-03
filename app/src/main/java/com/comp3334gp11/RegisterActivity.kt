package com.comp3334gp11

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {
    var userPicUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_selectPic.setOnClickListener {
            selectPic()
        }

        register_regButton.setOnClickListener {
            registerUser()
        }

        register_toLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

    }

    private fun selectPic() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            userPicUri = data.data
            register_userPic.setImageURI(userPicUri)
            register_selectPic.visibility = View.INVISIBLE
            register_selectPic.isClickable = false
        }
    }

    private fun registerUser() {
        val username = register_username.text.toString()
        val email = register_email.text.toString()
        val password = register_password.text.toString()
        val passwordAgain = register_password_again.text.toString()
        if (email.isEmpty() || password.isEmpty() || passwordAgain.isEmpty()) {
            Toast.makeText(this, "Please enter the email and password", Toast.LENGTH_LONG).show()
        }
        else if (password != passwordAgain) {
            Toast.makeText(this, "Both passwords are not the same", Toast.LENGTH_LONG).show()
        }
        else {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                if(it.isSuccessful) {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_LONG).show()
                    uploadUserPic(username)
                    startMessenger()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Account not created", Toast.LENGTH_LONG).show()
                Log.e("RegisterActivity", "Error: ${it.message}")
            }
        }
    }

    private fun uploadUserPic(username: String) {
        if (userPicUri != null) { 
            val filename = UUID.randomUUID().toString()
            val picRef = FirebaseStorage.getInstance().getReference("/images/$filename")
            picRef.putFile(userPicUri!!).addOnSuccessListener { 
                picRef.downloadUrl.addOnSuccessListener {
                    recordUser(username, it.toString())
                }.addOnFailureListener { 
                    Log.e("RegisterActivity", "Error: ${it.message}")
                }.addOnFailureListener {
                    Log.e("RegisterActivity", "Error: ${it.message}")
                }
            }.addOnFailureListener {
                Log.e("RegisterActivity", "Error: ${it.message}")
            }
        }
    }

    private fun recordUser(username: String, picUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, username, picUrl)
        userRef.setValue(user).addOnSuccessListener {
            Log.e("RegisterActivity", "Saved user: $user")
        }.addOnFailureListener {
            Log.e("RegisterActivity", "Error: ${it.message}")
        }
    }
    
    private fun startMessenger() {
        val intent = Intent(this, MessengerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}