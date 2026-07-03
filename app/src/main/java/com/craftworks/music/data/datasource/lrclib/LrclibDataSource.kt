package com.craftworks.music.data.datasource.lrclib

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.LrcLibLyrics
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.toLyrics
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LrclibDataSource @Inject constructor(
    private val settingsManager: MediaProviderSettingsManager,
    @ApplicationContext context: Context
) {
    @OptIn(InternalAPI::class)
    private val client = HttpClient(OkHttp) {
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

        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.SIMPLE
        }

        expectSuccess = true
    }

    suspend fun getLrcLibLyrics(metadata: MediaMetadata?, ignoreCachedResponse: Boolean = false): List<Lyric> = withContext(Dispatchers.IO) {
        val baseUrl = settingsManager.lrcLibEndpointFlow.first()

        val artist = metadata?.extras?.getString("lyricsArtist") ?: metadata?.artist.toString()
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

                if (ignoreCachedResponse)
                    header(HttpHeaders.CacheControl, "no-cache")
                else
                    header(HttpHeaders.CacheControl, "max-stale=2592000")
            }

            val mediaDataPlainLyrics: LrcLibLyrics = response.body()
            Log.d("LRCLIB", response.bodyAsText())
            Log.d("LRCLIB", response.headers.entries().joinToString("\n") { "${it.key}: ${it.value}" })
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
}