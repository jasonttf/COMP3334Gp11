package com.comp3334gp11

class ChatMessage(val messageID:String, val text:String, val fromID:String, val toID:String, val timeSent: Long) {
    constructor() : this("", "", "", "", -1)
}