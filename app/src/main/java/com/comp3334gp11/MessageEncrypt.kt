package com.comp3334gp11

import java.math.BigInteger

class MessageEncrypt {
    fun generator():Int {
        val max = 999
        val min = 100
        val range = max - min + 1
        var check = false
        while(check ==false){
            val rand = (Math.random() * range).toInt() + min
            if ((rand % 2 ==0 || rand % 3 ==0 || rand % 5 ==0 ||rand % 7 ==0|| rand % 11 ==0 || rand % 13 ==0 || rand % 17 ==0
                        || rand % 19 ==0 || rand % 23 ==0 || rand % 29 ==0 || rand % 31 ==0 || rand % 37 ==0 || rand % 41 ==0
                        || rand % 43 ==0 || rand % 47 ==0 || rand % 53 ==0 || rand % 59 ==0 || rand % 61 ==0 || rand % 67 ==0
                        || rand % 71 ==0 || rand % 73 ==0 || rand % 79 ==0 || rand % 83 ==0 || rand % 89 ==0 || rand % 97 ==0))
            {
                check = false
            }
            else
            {
                //check = true
                return rand
            }
        }
        return 1
    }

    fun generatord(N:Int, e:Int):Int {
        var A1 = 1
        var A2 = 0
        var A3 = N
        var B1 = 0
        var B2 = 1
        var B3 = e
        var T1:Int
        var T2:Int
        var T3:Int
        var d = 1 //B2 = d
        var a = true
        while (a == true)
        {
            if (B3 == 1)
            {
                if (B2 < 0)
                {
                    B2 += N
                }
                d = B2
                a = false
                break
            }
            else
            {
                val Q = A3 / B3
                T1 = A1 - Q * B1
                T2 = A2 - Q * B2
                T3 = A3 - Q * B3
                A1 = B1
                A2 = B2
                A3 = B3
                B1 = T1
                B2 = T2
                B3 = T3
            }
        }
        return d
    }

    fun encryption(message:String, E:Int, N:Int):String {
        fun Long.toBigInteger() = BigInteger.valueOf(this)
        fun Int.toBigInteger() = BigInteger.valueOf(toLong())
        val length = message.length
        var emessage = ""
        for (i in 0 until length)
        {
            val CHAR = message.get(i)
            var power = BigInteger (CHAR.toInt().toString()).pow(E)
            val result = (power.mod(BigInteger(N.toString())).toInt())
            emessage = "$emessage$result,"
        }
        return emessage
    }

    fun decryption(message:String, D:Int, N:Int):String{
        fun Long.toBigInteger() = BigInteger.valueOf(this)
        fun Int.toBigInteger() = BigInteger.valueOf(toLong())
        val m = message.split(",")
        val length = m.size-1
        var plaintext = ""
        for (i in 0 until length)
        {
            val str = m[i]
            var power = BigInteger (str).pow(D)
            val result = (power.mod(BigInteger(N.toString())).toInt())
            val mess = result.toChar()
            plaintext += mess.toString()
        }
        return plaintext
    }
}