package com.craftworks.music.providers.navidrome

import android.content.Context
import android.util.Log
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import com.craftworks.music.data.artistList
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.songsList
import com.craftworks.music.providers.local.getSongsOnDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.NoRouteToHostException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

var navidromeSyncInProgress = AtomicBoolean(false)

@Serializable
@SerialName("subsonic-response")
data class SubsonicResponse(
    val status: String,
    val version: String,
    val type: String,
    val serverVersion: String,
    val openSubsonic: Boolean,

    // Songs
    val searchResult3: SearchResult3? = null,

    // Albums
    val albumList: albumList? = null,
    val album: MediaData.Album? = null,

    // Artists
    val artists: Artists? = null,
    val artist: MediaData.Artist? = null,
    val artistInfo: MediaData.ArtistInfo? = null,

    // Playlists
    val playlist: MediaData.Playlist? = null,
    val playlists: PlaylistContainer? = null
)

suspend fun sendNavidromeGETRequest(
    baseUrl: String,
    username: String,
    password: String,
    endpoint: String) : List<MediaData> {

    val parsedData = mutableListOf<MediaData>()

    withContext(Dispatchers.IO) {
        navidromeSyncInProgress.set(true)

        // Generate a random password salt and MD5 hash.
        val passwordSalt = generateSalt(8)
        val passwordHash = md5Hash(password + passwordSalt)
        //Log.d("NAVIDROME", "baseUrl: $baseUrl, passwordSalt: $passwordSalt, endpoint: $endpoint")

        // All get requests come from this file.
        val url = URL("$baseUrl/rest/$endpoint&u=$username&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

        if (url.protocol.isEmpty() && url.host.isEmpty()){
            navidromeStatus.value = "Invalid URL"
            return@withContext
        }

        // Use HTTPS connection for allowing self-signed ssl certificates.
        val connection = if (url.protocol == "https") {
            url.openConnection() as HttpsURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }

        try {
            with(connection) {
                requestMethod = "GET"
                instanceFollowRedirects = true // Might fix issues with reverse proxies.

                Log.d("NAVIDROME", "\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode != 200){
                    Log.d("NAVIDROME", responseCode.toString())
                    navidromeStatus.value = "HTTP Error $responseCode"
                    navidromeSyncInProgress.set(false)
                    return@withContext
                }



                inputStream.bufferedReader().use {
                    val responseContent = it.readText()
                    when {
                        endpoint.startsWith("ping")         -> parseNavidromeStatusXML   (responseContent)
                        endpoint.startsWith("search3")      -> parsedData.addAll(parseNavidromeSearch3JSON    (responseContent, baseUrl, username, password))

                        // Albums
                        endpoint.startsWith("getAlbumList") -> parsedData.addAll(parseNavidromeAlbumListJSON(responseContent, baseUrl, username, password))
                        endpoint.startsWith("getAlbum.")    -> parseNavidromeAlbumSongsJSON(responseContent, baseUrl, username, password)


                        // Artists
                        endpoint.startsWith("getArtists")   -> parsedData.addAll(parseNavidromeArtistsJSON(responseContent))
                        endpoint.startsWith("getArtist.")   -> parsedData.addAll(listOf(parseNavidromeArtistAlbumsJSON(responseContent, baseUrl, username, password)))
                        endpoint.startsWith("getArtistInfo")-> parsedData.addAll(listOf(parseNavidromeArtistBiographyJSON(responseContent)))

                        // Playlists
                        endpoint.startsWith("getPlaylists") -> parsedData.addAll(parseNavidromePlaylistsJSON(responseContent, baseUrl, username, password))
                        endpoint.startsWith("getPlaylist.") -> parsedData.addAll(listOf(parseNavidromePlaylistJSON(responseContent, baseUrl, username, password)))
                        endpoint.startsWith("updatePlaylist") -> getNavidromePlaylists()
                        endpoint.startsWith("createPlaylist") -> getNavidromePlaylists()
                        endpoint.startsWith("deletePlaylist") -> getNavidromePlaylists()

                        // Radios
                        endpoint.startsWith("getInternetRadioStations") -> parseNavidromeRadioXML (responseContent)
                        else -> { navidromeSyncInProgress.set(false) }
                    }
                }
                inputStream.close()
            }
        }
        catch (e: ConnectException) {
            navidromeStatus.value = "Network Unreachable"
            Log.d("NAVIDROME", "Exception: $e")
        } catch (e: SocketTimeoutException) {
            navidromeStatus.value = "Timed out"
            Log.d("NAVIDROME", "Exception: $e")
        } catch (e: NoRouteToHostException) {
            navidromeStatus.value = "Host Unreachable"
            Log.d("NAVIDROME", "Exception: $e")
        } catch (e: UnknownHostException) {
            navidromeStatus.value = "Unknown Host"
            Log.d("NAVIDROME", "Exception: $e")
        } catch (e: ProtocolException) {
            navidromeStatus.value = "Invalid URL"
            Log.d("NAVIDROME", "Exception: $e")
        } catch (e: MalformedURLException) {
            navidromeStatus.value = "Invalid URL"
            Log.d("NAVIDROME", "Exception: $e")
        }
        navidromeSyncInProgress.set(false)
    }

    return parsedData
}


suspend fun reloadNavidrome(context: Context){
    songsList.clear()
    albumList.clear()
    artistList.clear()
    playlistList.clear()
    radioList.clear()

    if (localProviderList.isNotEmpty())
        getSongsOnDevice(context)

    //songsList.addAll(getNavidromeSongs())
    //albumList.addAll(getNavidromeAlbums())
    //getNavidromeArtists()
    //getNavidromePlaylists()
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

