package com.craftworks.music.data.providers.lyrics.lrcred

import android.content.Context
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.Lyrics
import com.craftworks.music.utils.parseTtml
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LrcRedDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
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
        val isrc = metadata?.extras?.getString("isrc")?.split(",")?.firstOrNull()
        val artist = metadata?.extras?.getString("lyricsArtist") ?: metadata?.artist.toString()
        val title = metadata?.title
        val album = metadata?.albumTitle
        val duration = metadata?.durationMs?.div(1000)

        try {
            if (!isrc.isNullOrEmpty()) {
                val response = client.get("https://lrc.red/s/$isrc.ttml").bodyAsText()
                val ttml = parseTtml(response, LyricSource.LRCRED)
                return@withContext ttml
            } else {
//                client.get("https://lrc.red/s/") {
//                    parameter("artist", artist)
//                    parameter("track", title)
//                    parameter("album", album)
//                    parameter("duration", duration)
//
//                    header(
//                        HttpHeaders.UserAgent,
//                        "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)"
//                    )
//
//                    if (ignoreCachedResponse)
//                        header(HttpHeaders.CacheControl, "no-cache")
//                    else
//                        header(HttpHeaders.CacheControl, "max-stale=2592000")
//                }
            }

            return@withContext null
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