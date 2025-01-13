package com.craftworks.music.providers.navidrome

import android.annotation.SuppressLint
import android.util.Log
import com.craftworks.music.data.MediaData
import com.craftworks.music.managers.NavidromeManager.getCurrentServer
import com.craftworks.music.managers.NavidromeManager.setSyncingStatus
import com.craftworks.music.providers.getPlaylists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.NoRouteToHostException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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

    // Radios
    val internetRadioStations: internetRadioStations? = null,

    // Playlists
    val playlist: MediaData.Playlist? = null,
    val playlists: PlaylistContainer? = null,

    //Lyrics
    val lyrics: MediaData.PlainLyrics? = null,
    val lyricsList: LyricsList? = null
)

suspend fun sendNavidromeGETRequest(endpoint: String) : List<MediaData> {
    val parsedData = mutableListOf<MediaData>()

    val server = getCurrentServer() ?: throw IllegalArgumentException("Could not get current server.")

    setSyncingStatus(true)
    withContext(Dispatchers.IO) {
        // Generate a random password salt and MD5 hash.
        // This is kinda slow, but needed.
        val passwordSalt = generateSalt(8)
        val passwordHash = md5Hash(server.password + passwordSalt)

        // Return if link isn't valid (just checking if HTTP is specified)
        if (!server.url.startsWith("http")){
            navidromeStatus.value = "Invalid URL"
            return@withContext
        }

        // All get requests come from this file. Use Subsonic link template.
        val url = URL("${server.url}/rest/$endpoint&u=${server.username}&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

        val connection = if (url.protocol == "https") {
            (url.openConnection() as HttpsURLConnection).apply {
                if (server.allowSelfSignedCert == true) {
                    // Allow every single cert. Not the best way to do this but eh
                    val trustAllCerts =
                        arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager") object :
                            X509TrustManager {
                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { }

                            @SuppressLint("TrustAllX509TrustManager")
                            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) { }

                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        })

                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                    val allHostsValid = HostnameVerifier { _, _ -> true }

                    // Apply cert authenticity changes.
                    sslSocketFactory = sslContext.socketFactory
                    hostnameVerifier = allHostsValid
                }
            }
        } else {
            url.openConnection() as HttpURLConnection
        }

        try {
            with(connection) {
                requestMethod = "GET"
                instanceFollowRedirects = true // Might fix issues with reverse proxies.

                Log.d("NAVIDROME", "\nSent 'GET' request to URL : ${url.host}; Response Code : $responseCode")

                if (responseCode != 200){
                    Log.d("NAVIDROME", responseCode.toString())
                    navidromeStatus.value = "HTTP Error $responseCode"
                    setSyncingStatus(false)
                    return@withContext
                }

                inputStream.bufferedReader().use {
                    withContext(Dispatchers.Default) {
                        val responseContent = it.readText()
                        when {
                            endpoint.startsWith("ping")         -> parseNavidromeStatusXML   (responseContent)
                            endpoint.startsWith("search3")      -> parsedData.addAll(parseNavidromeSearch3JSON    (responseContent, server.url, server.username, server.password))

                            // Albums
                            endpoint.startsWith("getAlbumList") -> parsedData.addAll(parseNavidromeAlbumListJSON(responseContent, server.url, server.username, server.password))
                            endpoint.startsWith("getAlbum.")    -> parsedData.addAll(parseNavidromeAlbumJSON(responseContent, server.url, server.username, server.password))


                            // Artists
                            endpoint.startsWith("getArtists")   -> parsedData.addAll(parseNavidromeArtistsJSON(responseContent))
                            endpoint.startsWith("getArtist.")   -> parsedData.addAll(listOf(parseNavidromeArtistAlbumsJSON(responseContent, server.url, server.username, server.password)))
                            endpoint.startsWith("getArtistInfo")-> parsedData.addAll(listOf(parseNavidromeArtistBiographyJSON(responseContent)))

                            // Playlists
                            endpoint.startsWith("getPlaylists") -> parsedData.addAll(parseNavidromePlaylistsJSON(responseContent, server.url, server.username, server.password))
                            endpoint.startsWith("getPlaylist.") -> parsedData.addAll(listOf(parseNavidromePlaylistJSON(responseContent, server.url, server.username, server.password)))
                            endpoint.startsWith("updatePlaylist") -> getPlaylists()
                            endpoint.startsWith("createPlaylist") -> getPlaylists()
                            endpoint.startsWith("deletePlaylist") -> getPlaylists()

                            // Radios
                            endpoint.startsWith("getInternetRadioStations") -> parsedData.addAll(parseNavidromeRadioJSON(responseContent))

                            // Lyrics
                            endpoint.startsWith("getLyrics.") -> parsedData.addAll(listOf(parseNavidromePlainLyricsJSON(responseContent)))
                            endpoint.startsWith("getLyricsBySongId.") -> parsedData.addAll(parseNavidromeSyncedLyricsJSON(responseContent))

                            else -> { setSyncingStatus(false) }
                        }
                    }
                }
                inputStream.close()
            }
        } // Peak code right here, try catch EVERYTHING.
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
        } catch (e: IOException) {
            navidromeStatus.value = "Unknown Error"
            Log.d("NAVIDROME", "Exception: $e")
        } catch (e: Exception) {
            navidromeStatus.value = "Fatal Error. Report immediately!"
            Log.d("NAVIDROME", "Exception: $e")
        } finally {
            setSyncingStatus(false)
        }
    }

    return parsedData
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