package com.craftworks.music.lyrics

import android.util.Log
import com.craftworks.music.data.Lyric
import com.craftworks.music.data.PlainLyrics
import com.craftworks.music.data.SyncedLyric
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.generateSalt
import com.craftworks.music.providers.navidrome.getNavidromePlaylists
import com.craftworks.music.providers.navidrome.md5Hash
import com.craftworks.music.providers.navidrome.navidromeStatus
import com.craftworks.music.providers.navidrome.navidromeSyncInProgress
import com.craftworks.music.providers.navidrome.parseNavidromeAlbumListJSON
import com.craftworks.music.providers.navidrome.parseNavidromeAlbumSongsJSON
import com.craftworks.music.providers.navidrome.parseNavidromeArtistXML
import com.craftworks.music.providers.navidrome.parseNavidromeArtistsXML
import com.craftworks.music.providers.navidrome.parseNavidromePlaylistXML
import com.craftworks.music.providers.navidrome.parseNavidromePlaylistsXML
import com.craftworks.music.providers.navidrome.parseNavidromeRadioXML
import com.craftworks.music.providers.navidrome.parseNavidromeSongJSON
import com.craftworks.music.providers.navidrome.parseNavidromeStatusXML
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection

private var isGetLyricsRunning = false

fun getLyrics(){
    Log.d("LYRICS", "is lyrics running? $isGetLyricsRunning")

    if (isGetLyricsRunning)
        return

    isGetLyricsRunning = true

    val thread = Thread {
        try {
            if (SongHelper.currentSong.title.isBlank() ||
                SongHelper.currentSong.title == "Song Title" ||
                SongHelper.currentSong.isRadio == true)
                return@Thread

            PlainLyrics = "Getting Lyrics..."
            SyncedLyric.clear()

            val url = URL("https://lrclib.net/api/get?artist_name=${SongHelper.currentSong.artist.replace(" ", "+")}&track_name=${SongHelper.currentSong.title.replace(" ", "+")}&album_name=${SongHelper.currentSong.album.replace(" ", "+")}&duration=${SongHelper.currentSong.duration}")

            with(url.openConnection() as HttpsURLConnection) {
                requestMethod = "GET"

                // Set User-Agent as per LRCLIB documentation: https://lrclib.net/docs
                setRequestProperty("User-Agent", "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)")

                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode == 404){
                    PlainLyrics = "No Lyrics / Instrumental"
                    return@Thread
                }

                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        //println(line)
                        val jsonObject = JSONObject(line)

                        /* TRY GETTING SYNCED. IF NOT FALLBACK TO PLAIN */
                        if (jsonObject.getString("syncedLyrics") != "null") {
                            val syncedLyrics = jsonObject.getString("syncedLyrics")
                            syncedLyrics.lines().forEach { lyric ->
                                val timeStampsRaw = getTimeStamps(lyric)[0]
                                val time = mmssToMilliseconds(timeStampsRaw)
                                val lyricText: String = lyric.drop(11)

                                // Check for duplicates
                                val syncedLyric = Lyric(time.toInt(),lyricText, false)
                                if (!SyncedLyric.contains(syncedLyric))
                                    SyncedLyric.add(syncedLyric)
                            }
                        }else {
                            /* FALLBACK PLAIN LYRICS */
                            PlainLyrics = jsonObject.getString("plainLyrics")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            Log.d("LYRICS", "Reset isGetLyricsRunning")
            isGetLyricsRunning = false
        }

    }

    thread.start()
}


suspend fun requestLyrics() {
    if (isGetLyricsRunning)
        return

    Log.d("LYRICS", "Getting Lyrics for song ${SongHelper.currentSong.title}")

    withContext(Dispatchers.IO) {

        if (SongHelper.currentSong.title.isBlank() ||
            SongHelper.currentSong.isRadio == true)
            return@withContext

        isGetLyricsRunning = true
        PlainLyrics = "Getting Lyrics..."
        SyncedLyric.clear()

        // All get requests come from this file.
        val url = URL("https://lrclib.net/api/get?artist_name=${SongHelper.currentSong.artist.replace(" ", "+")}&track_name=${SongHelper.currentSong.title.replace(" ", "+")}&album_name=${SongHelper.currentSong.album.replace(" ", "+")}&duration=${SongHelper.currentSong.duration}")

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

                // Set User-Agent as per LRCLIB documentation: https://lrclib.net/docs
                setRequestProperty("User-Agent", "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)")

                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode == 404){
                    PlainLyrics = "No Lyrics / Instrumental"
                    return@withContext
                }

                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        //println(line)
                        val jsonObject = JSONObject(line)

                        /* TRY GETTING SYNCED. IF NOT FALLBACK TO PLAIN */
                        if (jsonObject.getString("syncedLyrics") != "null") {
                            val syncedLyrics = jsonObject.getString("syncedLyrics")
                            syncedLyrics.lines().forEach { lyric ->
                                val timeStampsRaw = getTimeStamps(lyric)[0]
                                val time = mmssToMilliseconds(timeStampsRaw)
                                val lyricText: String = lyric.drop(11)

                                // Check for duplicates
                                val syncedLyric = Lyric(time.toInt(),lyricText, false)
                                if (!SyncedLyric.contains(syncedLyric))
                                    SyncedLyric.add(syncedLyric)
                            }
                        }else {
                            /* FALLBACK PLAIN LYRICS */
                            PlainLyrics = jsonObject.getString("plainLyrics")
                        }
                    }
                }
                inputStream.close()
            }
        }
        catch (e: UnknownHostException){
            PlainLyrics = "Cannot contact LRCLIB. \n Please check your connection."
        }
        finally {
            Log.d("LYRICS", "Reset isGetLyricsRunning")
            isGetLyricsRunning = false
        }
    }
}


/* FUNCTIONS */
fun mmssToMilliseconds(mmss: String): Long {
    val parts = mmss.split(":", ".")
    if (parts.size == 3) {
        try {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            val ms = parts[2].toLong()
            return (minutes * 60 + seconds) * 1000 + ms * 10
        } catch (e: NumberFormatException) {
            // Handle the case where the input is not in the expected format
            e.printStackTrace()
        }
    }
    return 0L
}
fun getTimeStamps(input: String): List<String> {
    val regex = Regex("\\[(.*?)]")
    val matches = regex.findAll(input)

    val result = mutableListOf<String>()
    for (match in matches) {
        result.add(match.groupValues[1])
    }

    return result
}
