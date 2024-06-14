package com.craftworks.music.providers.navidrome

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.craftworks.music.data.SearchResult3
import com.craftworks.music.data.Song
import com.craftworks.music.data.albumList
import com.craftworks.music.data.artistList
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.songsList
import com.craftworks.music.providers.local.getSongsOnDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

var navidromeSyncInProgress = mutableStateOf(false)

@Serializable
@SerialName("subsonic-response")
data class SubsonicResponse(
    val status: String,
    val version: String,
    val type: String,
    val serverVersion: String,
    val openSubsonic: Boolean,
    val searchResult3: SearchResult3
)


fun sendNavidromeGETRequest(baseUrl: String, username: String, password: String, endpoint: String) {
    navidromeSyncInProgress.value = true
    // Generate a random password salt and MD5 hash.
    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(password + passwordSalt)
    //Log.d("NAVIDROME", "baseUrl: $baseUrl, passwordSalt: $passwordSalt, endpoint: $endpoint")

    // All get requests come from this file.
    val url = URL("$baseUrl/rest/$endpoint&u=$username&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

    if (url.protocol.isEmpty() && url.host.isEmpty()){
        navidromeStatus.value = "Invalid URL"
        return
    }

    // Use HTTPS connection for allowing self-signed ssl certificates.
    val connection = if (url.protocol == "https") {
        url.openConnection() as HttpsURLConnection
    } else {
        url.openConnection() as HttpURLConnection
    }

    val coroutine = CoroutineScope(Dispatchers.Default)
    coroutine.launch {
        try {
            with(connection) {
                requestMethod = "GET"
                instanceFollowRedirects = true // Might fix issues with reverse proxies.

                Log.d("NAVIDROME", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode != 200){
                    Log.d("NAVIDROME", responseCode.toString())
                    navidromeStatus.value = "HTTP Error $responseCode"
                    navidromeSyncInProgress.value = false
                    return@launch
                }

                inputStream.bufferedReader().use {
                    when {
                        endpoint.startsWith("ping")         -> parseNavidromeStatusXML   (it.readLine())
                        endpoint.startsWith("search3")      -> parseNavidromeSongJSON    (it.readLine(), baseUrl, username, password, endpoint)
                        endpoint.startsWith("getAlbumList") -> parseNavidromeAlbumXML    (it.readLine(), baseUrl, username, password)

                        // Artists
                        endpoint.startsWith("getArtists")   -> parseNavidromeArtistsXML  (it.readLine(), baseUrl, username, password)
                        endpoint.startsWith("getArtistInfo")-> parseNavidromeArtistXML   (it.readLine())

                        // Playlists
                        endpoint.startsWith("getPlaylists") -> parseNavidromePlaylistsXML(it.readLine(), baseUrl, username, password)
                        endpoint.startsWith("getPlaylist.") -> parseNavidromePlaylistXML (it.readLine())
                        endpoint.startsWith("updatePlaylist") -> getNavidromePlaylists()
                        endpoint.startsWith("createPlaylist") -> getNavidromePlaylists()
                        endpoint.startsWith("deletePlaylist") -> getNavidromePlaylists()

                        // Radios
                        endpoint.startsWith("getInternetRadioStations") -> parseNavidromeRadioXML (it.readLine())
                    }
                }
                inputStream.close()
            }
        }
        catch (e: UnknownHostException){
            navidromeStatus.value = "Invalid URL"
            Log.d("NAVIDROME", "Unknown Host.")
        }
        catch (e: MalformedURLException){
            navidromeStatus.value = "Invalid URL"
            Log.d("NAVIDROME", "Malformed URL.")
        }

        navidromeSyncInProgress.value = false
    }
//    val thread = Thread {
//
//    }
//    thread.start()
}

fun reloadNavidrome(context: Context){
    songsList.clear()
    albumList.clear()
    artistList.clear()
    playlistList.clear()
    radioList.clear()

    if (localProviderList.isNotEmpty())
        getSongsOnDevice(context)

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