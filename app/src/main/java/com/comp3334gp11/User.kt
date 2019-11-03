package com.comp3334gp11

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val uid: String, val username: String, val userPicUrl: String): Parcelable {
    constructor(): this("", "", "")
}