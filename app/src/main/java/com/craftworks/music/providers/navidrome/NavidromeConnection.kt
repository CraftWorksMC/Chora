package com.craftworks.music.providers.navidrome

import android.net.Uri
import android.util.Log
import com.craftworks.music.data.Album
import com.craftworks.music.data.albumList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.gitlab.mvysny.konsumexml.konsumeXml
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
                    endpoint.startsWith("ping") -> parseNavidromeStatusXML(it, "/subsonic-response", "/subsonic-response/error")
                    //"search3.view?query=''&songCount=500" -> parseNavidromeSongXML(it, "/subsonic-response/searchResult3/song", songsList)
                    endpoint.startsWith("search3")      -> parseNavidromeSongXML    (it.readLine(), baseUrl, username, password)
                    endpoint.startsWith("getAlbumList") -> parseNavidromeAlbumXML   (it.readLine(), baseUrl, username, password)
                    endpoint.startsWith("getArtists")   -> parseNavidromeArtistsXML (it.readLine(), baseUrl, username, password)
                    endpoint.startsWith("getArtistInfo")-> parseNavidromeArtistXML  (it.readLine())
                    endpoint.startsWith("getPlaylists") -> parseNavidromePlaylistsXML(it.readLine(), baseUrl, username, password, playlistList)
                    endpoint.startsWith("getPlaylist.") -> parseNavidromePlaylistXML(it.readLine())
                    endpoint.startsWith("getInternetRadioStations") -> parseRadioXML(it, "/subsonic-response/internetRadioStations/internetRadioStation", radioList)
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

fun parseNavidromeAlbumXML(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String) {

    // Avoid crashing by removing some useless tags.
    val newResponse = response
        .replace("xmlns=\"http://subsonic.org/restapi\" ", "")

    newResponse.konsumeXml().apply {
        child("subsonic-response"){
            child("albumList"){
                children("album"){
                    val albumTitle = attributes.getValue("title")
                    val albumArtist = attributes.getValue("artist")
                    val albumYear = attributes.getValueOrNull("year") ?: "0"
                    val albumID = attributes.getValue("id")

                    val passwordSalt = generateSalt(8)
                    val passwordHash = md5Hash(navidromePassword + passwordSalt)

                    val albumArtUri = Uri.parse("$navidromeUrl/rest/getCoverArt.view?&id=$albumID&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

                    val album = Album(
                        name = albumTitle,
                        artist = albumArtist,
                        year = albumYear,
                        coverArt = albumArtUri,
                        navidromeID = albumID
                    )
                    if (albumList.none { it.name == albumTitle })
                        albumList.add(album)

                    skipContents()
                    finish()
                }.apply {

                    // Get albums 100 at a time.
                    if (size == 100){
                        val albumOffset = (albumList.size + 1)
                        sendNavidromeGETRequest(
                            navidromeUrl,
                            navidromeUsername,
                            navidromePassword,
                            "getAlbumList.view?type=newest&size=100&offset=$albumOffset"
                        )
                    }
                }
            }
        }
    }

    Log.d("NAVIDROME", "Added albums! Total: ${albumList.size}")
}