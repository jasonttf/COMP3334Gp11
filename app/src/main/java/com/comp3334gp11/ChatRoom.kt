package com.comp3334gp11

class ChatRoom (val chatID: String, val users: MutableList<String>) {
    constructor(): this("", listOf<String>("") as MutableList<String>)
}