package com.craftworks.music.data.datasource.lrclib

import android.content.Context
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.LrcLibLyrics
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.SyncedWord
import com.craftworks.music.data.model.toLyrics
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LrclibDataSource @Inject constructor(
    private val settingsManager: MediaProviderSettingsManager,
    @ApplicationContext context: Context
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(HttpCache) {
            val cacheDir = File(context.cacheDir, "lrclib_http_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            publicStorage(FileStorage(cacheDir))
        }

        expectSuccess = true
    }

    suspend fun getLrcLibLyrics(metadata: MediaMetadata?): List<Lyric> = withContext(Dispatchers.IO) {
        val baseUrl = settingsManager.lrcLibEndpointFlow.first()

        val artist = metadata?.extras?.getString("lyricsArtist")
        val title = metadata?.title
        val album = metadata?.albumTitle
        val duration = metadata?.durationMs?.div(1000)

        try {
            val response = client.get(baseUrl) {
                url {
                    appendPathSegments("api", "get")
                }

                parameter("artist_name", artist)
                parameter("track_name", title)
                parameter("album_name", album)
                parameter("duration", duration)

                header(HttpHeaders.UserAgent, "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)")
            }

            val mediaDataPlainLyrics: LrcLibLyrics = response.body()
            return@withContext mediaDataPlainLyrics.toLyrics()

        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                return@withContext emptyList()
            }
            e.printStackTrace()
            return@withContext emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun lyricsFileParser(lyricsFile: String) : List<Lyric> {
        val settings = LoadSettings.builder().build()
        val raw = Load(settings).loadFromString(lyricsFile) as? Map<*, *>
            ?: throw IllegalArgumentException("Invalid YAML format")

        // 2. Map Lines
        val linesList = raw["lines"] as? List<*> ?: emptyList<Any>()
        val lines = linesList.map { lineItem ->
            val lineMap = lineItem as? Map<*, *> ?: emptyMap<Any, Any>()

            // Map the nested Words list inside the line
            val wordsList = lineMap["words"] as? List<*> ?: emptyList<Any>()
            val words = wordsList.map { wordItem ->
                val wordMap = wordItem as? Map<*, *> ?: emptyMap<Any, Any>()
                SyncedWord(
                    text = wordMap["text"]?.toString() ?: "",
                    startMs = wordMap["start_ms"]?.toString()?.toInt() ?: 0
                )
            }

            Lyric(
                text = listOf(lineMap["text"]?.toString() ?: ""),
                words = words,
                startMs = lineMap["start_ms"]?.toString()?.toInt() ?: 0,
                endMs = lineMap["end_ms"]?.toString()?.toInt() ?: 0
            )
        }

        return lines
    }
}