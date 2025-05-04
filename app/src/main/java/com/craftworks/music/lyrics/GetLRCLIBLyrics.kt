package com.craftworks.music.lyrics

import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.LrcLibLyrics
import com.craftworks.music.data.Lyric
import com.craftworks.music.data.toLyrics
import com.craftworks.music.player.SongHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

suspend fun getLrcLibLyrics(metadata: MediaMetadata?): List<Lyric> = withContext(Dispatchers.IO) {
    val url = URL(
        "https://lrclib.net/api/get?" +
                "artist_name=${URLEncoder.encode(metadata?.artist.toString(), "UTF-8")}&" +
                "track_name=${URLEncoder.encode(metadata?.title.toString(), "UTF-8")}&" +
                "album_name=${URLEncoder.encode(metadata?.albumTitle.toString(), "UTF-8")}" +
                "&duration=${SongHelper.currentSong.duration}"
    )

    val connection = url.openConnection() as HttpsURLConnection

    try {
        connection.requestMethod = "GET"

        // Set User-Agent as per LRCLIB documentation: https://lrclib.net/docs
        connection.setRequestProperty(
            "User-Agent",
            "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)"
        )

        println("Sent 'GET' request to URL : $url; Response Code : ${connection.responseCode}")

        if (connection.responseCode == 404) {
            return@withContext emptyList()
        }

        connection.inputStream.bufferedReader().use {
            val jsonParser = Json { ignoreUnknownKeys = true }
            val mediaDataPlainLyrics = jsonParser.decodeFromString<LrcLibLyrics>(it.readText())
            return@withContext mediaDataPlainLyrics.toLyrics()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext emptyList()
    } finally {
        connection.disconnect()
    }
}