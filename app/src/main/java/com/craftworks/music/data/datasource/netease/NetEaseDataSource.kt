package com.craftworks.music.data.datasource.netease

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.NeteaseLyricsResponse
import com.craftworks.music.data.model.toLyrics
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NeteaseSearchResponse(
    val result: NeteaseSearchResult? = null
)

@Serializable
data class NeteaseSearchResult(
    val songs: List<NeteaseSong>? = null
)

@Serializable
data class NeteaseSong(
    val id: Long,
    val name: String,
    val artists: List<NeteaseArtist> = emptyList()
)

@Serializable
data class NeteaseArtist(
    val name: String
)

@Singleton
class NeteaseDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.SIMPLE
        }
        install(HttpCache) {
            val cacheDir = File(context.cacheDir, "netease_http_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            publicStorage(FileStorage(cacheDir))
        }
    }

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    suspend fun getNeteaseLyrics(metadata: MediaMetadata?): List<Lyric> = withContext(Dispatchers.IO) {
        try {
            val title  = metadata?.title?.toString() ?: return@withContext emptyList()
            val artist = metadata.extras?.getString("lyricsArtist") ?: ""

            val songId = searchSongId(title, artist) ?: return@withContext emptyList()
            val lyricsResponse = fetchLyrics(songId)

            lyricsResponse.toLyrics()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun searchSongId(title: String, artist: String): Long? {
        val query = if (artist.isNotBlank()) "$title $artist" else title
        val response: NeteaseSearchResponse = client.get(
            "https://music.163.com/api/search/get"
        ) {
            parameter("s", query)
            parameter("type", 1)      // 1 = songs
            parameter("limit", 1)
            header(HttpHeaders.UserAgent, userAgent)
        }.body()
        return response.result?.songs?.firstOrNull()?.id
    }

    private suspend fun fetchLyrics(songId: Long): NeteaseLyricsResponse {
        val response = client.get("https://music.163.com/api/song/lyric") {
            parameter("os", "pc")
            parameter("id", songId)
            parameter("lv", -1)
            parameter("kv", -1)
            parameter("tv", -1)
            header(HttpHeaders.UserAgent, userAgent)
        }
        Log.d("LYRICS", "NETEASE: ${response.bodyAsText()}")
        return response.body<NeteaseLyricsResponse>()
    }
}