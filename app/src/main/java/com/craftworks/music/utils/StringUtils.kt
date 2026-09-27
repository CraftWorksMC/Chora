package com.craftworks.music.utils

import java.security.MessageDigest
import java.util.Locale

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

    fun makeValidFilename(segment: String): String {
        return segment
            .replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F]"), "_")
            .trim()
            .trimEnd('.', ' ')
            .ifBlank { "_" }
    }

    fun makeValidFilepath(segment: String): String {
        return segment
            .replace(Regex("[:*?\"<>|\\x00-\\x1F]"), "_")
            .trim()
            .trimEnd('.', ' ')
            .ifBlank { "_" }
    }

    fun formatSeconds(seconds: Int): String {
        if (seconds >= 3600) return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
    }
}