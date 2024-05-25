package com.craftworks.music.providers.navidrome

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

fun sendNavidromeGETRequest(baseUrl: String, username: String, password: String, endpoint: String) {


    // Generate a random password salt and MD5 hash.
    val passwordSalt = generateSalt(5)
    val passwordHash = md5Hash(password + passwordSalt)
    Log.d("NAVIDROME", "baseUrl: $baseUrl, passwordSalt: $passwordSalt, endpoint: $endpoint")

    // All get requests come from this file.
    val url = URL("$baseUrl/rest/$endpoint.view?&u=$username&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

    val connection = if (url.protocol == "https") {
        url.openConnection() as HttpsURLConnection
    } else {
        url.openConnection() as HttpURLConnection
    }
    Log.d("NAVIDROME", "opened connection as $connection, protocol: ${url.protocol}")

    val thread = Thread {
        with(connection) {
            requestMethod = "GET"
            instanceFollowRedirects = true

            Log.d("GET", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

            // region Response Codes
            if (responseCode == 404) {
                Log.d("NAVIDROME", "404")
                navidromeStatus.value = "Invalid URL"
                return@Thread
            }
            if (responseCode == 503) {
                Log.d("NAVIDROME", "503")
                navidromeStatus.value = "Access Denied, 503"
                return@Thread
            }
            // endregion

            inputStream.bufferedReader().use {
                when (endpoint) {
                    "ping" -> parseNavidromeStatusXML(it, "/subsonic-response", "/subsonic-response/error")
                }
            }
        }
    }
    thread.start()
}

fun md5Hash(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val hashBytes = md.digest(input.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}
fun generateSalt(length: Int): String {
    val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}