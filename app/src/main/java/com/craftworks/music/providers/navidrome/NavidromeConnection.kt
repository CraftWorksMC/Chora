package com.craftworks.music.providers.navidrome

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

fun sendNavidromeGETRequest(baseUrl: String, username: String, password: String, endpoint: String) {

    // Generate a random password salt and MD5 hash.
    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(password + passwordSalt)
    Log.d("NAVIDROME", "baseUrl: $baseUrl, passwordSalt: $passwordSalt, endpoint: $endpoint")

    // All get requests come from this file.
    val url = URL("$baseUrl/rest/$endpoint&u=$username&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

    // Use HTTPS connection for allowing self-signed ssl certificates.
    val connection = if (url.protocol == "https") {
        url.openConnection() as HttpsURLConnection
    } else {
        url.openConnection() as HttpURLConnection
    }

    val thread = Thread {
        with(connection) {
            requestMethod = "GET"
            instanceFollowRedirects = true // Might fix issues with reverse proxies.

            Log.d("NAVIDROME", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

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
                when {
                    endpoint.startsWith("ping")         -> parseNavidromeStatusXML   (it.readLine())
                    endpoint.startsWith("search3")      -> parseNavidromeSongXML     (it.readLine(), baseUrl, username, password)
                    endpoint.startsWith("getAlbumList") -> parseNavidromeAlbumXML    (it.readLine(), baseUrl, username, password)

                    // Artists
                    endpoint.startsWith("getArtists")   -> parseNavidromeArtistsXML  (it.readLine(), baseUrl, username, password)
                    endpoint.startsWith("getArtistInfo")-> parseNavidromeArtistXML   (it.readLine())

                    // Playlists
                    endpoint.startsWith("getPlaylists") -> parseNavidromePlaylistsXML(it.readLine(), baseUrl, username, password)
                    endpoint.startsWith("getPlaylist.") -> parseNavidromePlaylistXML (it.readLine())

                    // Radios
                    endpoint.startsWith("getInternetRadioStations") -> parseNavidromeRadioXML (it.readLine())
                }
            }

        }
    }
    thread.start()
}

fun reloadNavidrome(){
    getNavidromeSongs()
    getNavidromeAlbums()
    getNavidromeArtists()
    getNavidromePlaylists()
    getNavidromeRadios()
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