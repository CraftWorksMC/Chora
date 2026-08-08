package com.craftworks.music.utils

import java.security.MessageDigest

object StringUtils {
    fun md5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateSalt(length: Int): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { allowedChars.random() }.joinToString("")
    }
}