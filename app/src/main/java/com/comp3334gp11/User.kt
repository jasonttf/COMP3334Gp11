package com.comp3334gp11

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val uid: String, val email:String, val username: String, val userPicUrl: String, val time: String, val e: Int, val n: Int, val d: Int): Parcelable {
    constructor(): this("", "", "", "", "", -1, -1, -1)
}