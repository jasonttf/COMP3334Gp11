package com.comp3334gp11

import android.util.Log
import java.security.MessageDigest

class PasswordSha {
    fun shaOnce (input:String, time:Long): String {
        var salt = time.toString()
        val salt_pw = input + salt
        Log.e("password", salt_pw)
        var bytes = salt_pw.toByteArray()
        var md = MessageDigest.getInstance("SHA-512")
        var digest = md.digest(bytes)
        return digest.fold("", {str, it-> str +"%02x".format(it)})
    }
}