package com.craftworks.music.data.providers.lyrics.binimum

import android.content.Context
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.BiniLyricsResponse
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.Lyrics
import com.craftworks.music.utils.parseTtml
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
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiniLyricsDataSource @Inject constructor(
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
            val cacheDir = File(context.cacheDir, "binimum_http_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            publicStorage(FileStorage(cacheDir))
        }

        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.SIMPLE
        }

        expectSuccess = true
    }

    suspend fun getLyrics(
        metadata: MediaMetadata?,
        ignoreCachedResponse: Boolean = false
    ): Lyrics? = withContext(Dispatchers.IO) {
        val isrc = metadata?.extras?.getString("isrc")?.split(",")
        val artist = metadata?.extras?.getString("lyricsArtist") ?: metadata?.artist.toString()
        val title = metadata?.title
        val album = metadata?.albumTitle
        val duration = metadata?.durationMs?.div(1000)

        try {
            val response = client.get("https://lyrics-api.binimum.org/") {
                if (isrc != null) {
                    parameter("isrc", isrc.first())
                }
                else {
                    parameter("artist", artist)
                    parameter("track", title)
                    parameter("album", album)
                    parameter("duration", duration)
                }

                header(HttpHeaders.UserAgent, "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)")

                if (ignoreCachedResponse)
                    header(HttpHeaders.CacheControl, "no-cache")
                else
                    header(HttpHeaders.CacheControl, "max-stale=2592000")
            }

            val lyricsUrl = response.body<BiniLyricsResponse>().results.first().lyricsUrl

            val ttml = parseTtml(client.get(lyricsUrl).bodyAsText(), LyricSource.BINI_LYRICS)
            return@withContext ttml
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                return@withContext null
            }
            e.printStackTrace()
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}