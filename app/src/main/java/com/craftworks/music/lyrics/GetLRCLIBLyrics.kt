package com.craftworks.music.lyrics

import com.craftworks.music.data.LrcLibLyrics
import com.craftworks.music.data.Lyric
import com.craftworks.music.data.toLyrics
import com.craftworks.music.player.SongHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL
import javax.net.ssl.HttpsURLConnection

suspend fun getLrcLibLyrics(): List<Lyric> {
    var lyrics: List<Lyric>

    withContext(Dispatchers.IO) {
        val url = URL(
            "https://lrclib.net/api/get?" +
                    "artist_name=${SongHelper.currentSong.artist.replace(" ", "+")}&" +
                    "track_name=${SongHelper.currentSong.title.replace(" ", "+")}&" +
                    "album_name=${SongHelper.currentSong.album.replace(" ", "+")}" +
                    "&duration=${SongHelper.currentSong.duration}"
        )

        val connection = url.openConnection() as HttpsURLConnection

        try {
            with(connection) {
                requestMethod = "GET"

                // Set User-Agent as per LRCLIB documentation: https://lrclib.net/docs
                setRequestProperty(
                    "User-Agent",
                    "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)"
                )

                println("\nSent 'GET' request to URL : $url; Response Code : $responseCode")

                if (responseCode == 404) {
                    lyrics = listOf(Lyric(-1, "No Lyrics Found"))
                    return@withContext
                }

                inputStream.bufferedReader().use {
                    withContext(Dispatchers.Default) {
                        lyrics = parseLrcLibLyrics(it.readText())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            lyrics = listOf(Lyric(-1, e.localizedMessage?.toString() ?: "Unknown Error"))
        }
    }

    return lyrics
}

fun parseLrcLibLyrics(
    response: String
): List<Lyric> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val mediaDataPlainLyrics = jsonParser.decodeFromString<LrcLibLyrics>(response)

    return mediaDataPlainLyrics.toLyrics()
}