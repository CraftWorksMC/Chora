package com.craftworks.music.lyrics

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.craftworks.music.lyrics.songLyrics.SongLyrics
import com.craftworks.music.playingSong
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


var SyncedLyric = mutableStateListOf<Lyric>(Lyric(0, "GETTING LYRICS", false))
object songLyrics{
    var SongLyrics by mutableStateOf("Getting Lyrics... / No Lyrics Found")
}


data class Lyric(
    val timestamp: Int,
    val content: String,
    var isCurrentLyric: Boolean)


fun getLyrics(){
    val thread = Thread {
        try {
            if (playingSong.selectedSong?.title.isNullOrBlank() ||
                playingSong.selectedSong?.title == "Song Title")
                throw IllegalArgumentException("Song Empty!")

            val url = URL("https://lrclib.net/api/get?artist_name=${playingSong.selectedSong!!.artist.replace(" ", "+")}&track_name=${playingSong.selectedSong!!.title.replace(" ", "+")}&album_name=${playingSong.selectedSong!!.album.replace(" ", "+")}&duration=${playingSong.selectedSong!!.duration / 1000}")

            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        println(line)
                        val jsonObject = JSONObject(line)

                        /* TRY GETTING SYNCED. IF NOT FALLBACK TO PLAIN */
                        if (jsonObject.getString("syncedLyrics") != "null") {
                            val syncedLyrics = jsonObject.getString("syncedLyrics")
                            syncedLyrics.lines().forEach { lyric ->
                                val timeStampsRaw = getTimeStamps(lyric)[0]
                                val time = mmssToMilliseconds(timeStampsRaw)
                                val lyricText: String = lyric.drop(11)
                                Log.d("Lyric: ", "$time: $lyricText")
                                SyncedLyric.add(Lyric(time.toInt(),lyricText, false))
                            }
                        }else {
                            /* FALLBACK PLAIN LYRICS */
                            SongLyrics = jsonObject.getString("plainLyrics")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    thread.start()
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
    val regex = Regex("\\[(.*?)\\]")
    val matches = regex.findAll(input)

    val result = mutableListOf<String>()
    for (match in matches) {
        result.add(match.groupValues[1])
    }

    return result
}
