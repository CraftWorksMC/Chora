package com.craftworks.music.data.datasource.lrclib

import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.LrcLibLyrics
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.toLyrics
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class LrclibDataSource @Inject constructor(
    private val settingsManager: MediaProviderSettingsManager
) {
    suspend fun getLrcLibLyrics(metadata: MediaMetadata?): List<Lyric> = withContext(Dispatchers.IO) {
        val baseUrl = settingsManager.lrcLibEndpointFlow.first()

        val url = URL(
            "$baseUrl/api/get?" +
                    "artist_name=${URLEncoder.encode(metadata?.extras?.getString("lyricsArtist").toString(), "UTF-8")}&" +
                    "track_name=${URLEncoder.encode(metadata?.title.toString(), "UTF-8")}&" +
                    "album_name=${URLEncoder.encode(metadata?.albumTitle.toString(), "UTF-8")}" +
                    "&duration=${metadata?.durationMs?.div(1000)?.toInt()}"
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
}