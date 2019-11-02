package com.comp3334gp11

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_loginButton.setOnClickListener {
            login()
        }

        login_toRegister.setOnClickListener {
            finish()
        }
    }

    private fun login() {
        val email = login_email.text.toString()
        val password = login_password.text.toString()
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Login successfully", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MessengerActivity::class.java))
            }
        }.addOnFailureListener {
            Log.e("Login", "Error: ${it.message}")
        }
    }
}