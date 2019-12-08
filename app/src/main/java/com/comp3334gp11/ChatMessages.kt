package com.comp3334gp11

class ChatMessages(val chatID:String, val userID: String, val userPicUrl: String, val username: String, val message: String, val time: Long, val fromID: String, val toID: String) {
    constructor(): this("", "", "", "", "", -1, "", "")
}
