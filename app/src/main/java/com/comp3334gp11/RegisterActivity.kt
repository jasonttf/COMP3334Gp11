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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.crypto.KeyGenerator

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
        var password = register_password.text.toString()
        val passwordAgain = register_password_again.text.toString()
        val time = System.currentTimeMillis()
        if (email.isEmpty() || password.isEmpty() || passwordAgain.isEmpty()) {
            Toast.makeText(this, "Please enter the email and password", Toast.LENGTH_LONG).show()
        }
        else if (password != passwordAgain) {
            Toast.makeText(this, "Both passwords are not the same", Toast.LENGTH_LONG).show()
        }
        else if (!(checkPassword(password))) {
            Toast.makeText(this, "Please enter a password with at least 1 capital letter, small letter, number and symbol.", Toast.LENGTH_LONG).show()
        }
        else {
            Log.e("RegisterActivity", password)
            val passwordSha = PasswordSha()
            password = passwordSha.shaOnce(password, time)
            Log.e("RegisterActivity", password)
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                if(it.isSuccessful) {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_LONG).show()
                    generateRSAkeys(username, time, email)
                    initChatRooms()
                    startMessenger()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Account not created", Toast.LENGTH_LONG).show()
                Log.e("RegisterActivity", "Error: ${it.message}")
            }
        }
    }

    private fun generateRSAkeys(username: String, time: Long, email: String){
        val messageEncrypt = MessageEncrypt()
        var check = false
        var a1 = 0
        var a2 = 0
        var E = 0
        var N = 0
        var ND = 0
        while(check == false){
            a1 = messageEncrypt.generator()
            a2 = messageEncrypt.generator()
            E = messageEncrypt.generator()
            check = !(E == a2 || E ==a1)
        }
        N = a1*a2
        ND = (a1-1) * (a2-1)
        val D = messageEncrypt.generatord(ND,E)
        val fileName = filesDir.path + "key.txt"
        Log.e("RegisterActivity", fileName)
        File(fileName).printWriter().use { out -> out.println(D) }
        val uid = FirebaseAuth.getInstance().uid
        val keyRef =  FirebaseStorage.getInstance().getReference("/key/$uid")
        val file = Uri.fromFile(File(fileName))
        keyRef.putFile(file).addOnSuccessListener {
            Log.e("RegisterActivity", "key saved")
        }.removeOnFailureListener {
            Log.e("RegisterActivity", "key not saved")
        }
        uploadUserPic(username, time, email, E, N)
    }

    private fun uploadUserPic(username: String, time: Long, email: String, e: Int, n: Int) {
        if (userPicUri != null) {
            val filename = UUID.randomUUID().toString()
            val picRef = FirebaseStorage.getInstance().getReference("/images/$filename")
            picRef.putFile(userPicUri!!).addOnSuccessListener {
                picRef.downloadUrl.addOnSuccessListener {
                    recordUser(username, it.toString(), time, email, e, n) //if no photo, no record in db
                }.addOnFailureListener {
                    Log.e("RegisterActivity", "Error: ${it.message}")
                }.addOnFailureListener {
                    Log.e("RegisterActivity", "Error: ${it.message}")
                }
            }.addOnFailureListener {
                Log.e("RegisterActivity", "Error: ${it.message}")
            }
        } else {
            val userPicUrl = "https://firebasestorage.googleapis.com/v0/b/comp3334-gp-project.appspot.com/o/images%2Fdownload.png?alt=media&token=b21e76c7-7b0e-4441-826a-1a46c572e028"
            recordUser(username, userPicUrl, time, email, e, n)
        }
    }

    private fun recordUser(username: String, picUrl: String, time: Long, email: String, e: Int, n: Int) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, email, username, picUrl, time.toString(), e, n)
        userRef.setValue(user).addOnSuccessListener {
            Log.e("RegisterActivity", "Saved user: $user")
        }.addOnFailureListener {
            Log.e("RegisterActivity", "Error: ${it.message}")
        }
    }

    private fun initChatRooms() {
        val chatRoomRef = FirebaseDatabase.getInstance().getReference("/chat_room")
        val fromID =FirebaseAuth.getInstance().uid.toString()
        val userRef = FirebaseDatabase.getInstance().getReference("/users")
        var cID: String
        var toID: String
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("RegisterActivity", "Error: $p0")
            }

            override fun onDataChange(p0: DataSnapshot) {
               for (i in p0.children) {
                   val temp = i.getValue(User::class.java)
                   if (temp != null) {
                       toID = temp.uid
                       if (toID != fromID) {
                           val newRoomRef = chatRoomRef.push()
                           val users = mutableListOf(fromID, toID)
                           cID = newRoomRef.key.toString()
                           var kgen = KeyGenerator.getInstance("HMACSHA256")
                           var skey = kgen.generateKey()
                           var tempKey = skey.getEncoded()
                           var keyString = tempKey.toString()
                           val chatRoom = ChatRoom(cID, users, keyString)
                           newRoomRef.setValue(chatRoom)
                           Log.e("RegisterActivity", "new chat room: $cID")
                       }
                   }
               }
            }
        })
    }

    private fun startMessenger() {
        val intent = Intent(this, MessengerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun checkPassword(pw: String): Boolean {
        val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{4,}$"
        val pattern = Pattern.compile(PASSWORD_PATTERN)
        val matcher = pattern.matcher(pw)
        return matcher.matches()
    }
}