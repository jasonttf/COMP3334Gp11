package com.comp3334gp11

class ChatMessages(val userID: String, val userPicUrl: String, val username: String, val message: String, val time: Long) {
    constructor(): this("", "", "", "", -1)
}
