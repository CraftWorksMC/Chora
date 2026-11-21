package com.craftworks.music.data.providers.lyrics.unison

import android.content.Context
import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.LyricSourceCredit
import com.craftworks.music.data.model.Lyrics
import com.craftworks.music.data.model.LyricsLine
import com.craftworks.music.data.model.SyncType
import com.craftworks.music.data.model.UnisonLyricsResponse
import com.craftworks.music.data.model.UnisonSearchResponse
import com.craftworks.music.utils.getTimeStamps
import com.craftworks.music.utils.mmssToMilliseconds
import com.craftworks.music.utils.parseTtml
import com.craftworks.music.utils.separateBackgroundLyrics
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private data class CachedUnisonEntry(
    val etag: String?,
    val lyrics: Lyrics
)

@Singleton
class UnisonLyricsDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.SIMPLE
        }

        expectSuccess = false
    }

    private class UnisonCache(val etag: String, val body: String)
    private val cacheDir = File(context.cacheDir, "unison_lyrics").apply { mkdirs() }

    private fun cacheFile(key: String) = File(
        cacheDir,
        MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
    )

    private fun readCache(key: String): UnisonCache? = runCatching {
        val text = cacheFile(key).readText()
        UnisonCache(text.substringBefore('\n'), text.substringAfter('\n'))
    }.getOrNull()

    private fun writeCache(key: String, etag: String, body: String) {
        runCatching { cacheFile(key).writeText("$etag\n$body") }
    }

    suspend fun getLyrics(
        metadata: MediaMetadata?,
        ignoreCachedResponse: Boolean = false
    ): Lyrics? = withContext(Dispatchers.IO) {
        val artist = metadata?.extras?.getString("lyricsArtist") ?: metadata?.artist.toString()
        val title = metadata?.title
        val album = metadata?.albumTitle
        val duration = metadata?.durationMs?.div(1000)
        val isrc = metadata?.extras?.getString("isrc")?.split(",")

        val cacheKey = "${isrc?.first()}|$title|$artist|$duration"
        val cached = if (ignoreCachedResponse) null else readCache(cacheKey)

        try {
            val response = client.get("https://unison.boidu.dev/lyrics") {
                if (isrc != null)
                    parameter("isrc", isrc.first())
                parameter("song", title)
                parameter("artist", artist)
                parameter("duration", duration)

                header(HttpHeaders.UserAgent, "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)")

                cached?.let { header(HttpHeaders.IfNoneMatch, it.etag) }
            }

            var data = if (response.status == HttpStatusCode.NotModified && cached != null) {
                json.decodeFromString<UnisonLyricsResponse>(cached.body).data
            } else {
                val body = response.bodyAsText()
                val parsed = json.decodeFromString<UnisonLyricsResponse>(body)
                val etag = response.headers[HttpHeaders.ETag]
                if (etag != null && parsed.data != null) writeCache(cacheKey, etag, body)
                parsed.data
            }

            // Search Fallback
            if (data == null && isrc != null)  {
                val isrcSearchResponse = client.get("https://unison.boidu.dev/lyrics/search") {
                    parameter("q", "$title $artist")

                    header(HttpHeaders.UserAgent, "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)")
                }.body<UnisonSearchResponse>()

                if (isrcSearchResponse.data.isNullOrEmpty())
                    return@withContext null

                val matchedLyric = isrcSearchResponse.data.firstOrNull { it.matchScore!! > 0.9 }

                if (matchedLyric == null)
                    return@withContext null

                data = client.get("https://unison.boidu.dev/lyrics/${matchedLyric.id}") {
                    header(HttpHeaders.UserAgent, "Chora - Navidrome Client (https://github.com/CraftWorksMC/Chora)")
                }.body()
            }

            if (data?.lyrics == null)
                return@withContext null

            val credit = LyricSourceCredit(
                songUrl = "https://unison.betterlyrics.org/song/${data.videoId}",
                curatorUrl = "https://unison.betterlyrics.org/curator/${data.submitter.keyId}",
                curatorName = data.submitter.displayName,
                curatorAvatar = data.submitter.avatarUrl
            )

            when (data.format) {
                "ttml" -> return@withContext parseTtml(data.lyrics, LyricSource.UNISON).copy(credit = credit)
                "lrc" -> {
                    val lines = mutableListOf<LyricsLine>()

                    data.lyrics.lines().forEach { lyric ->
                        if (lyric.isBlank()) return@forEach
                        val timeStampRaw = getTimeStamps(lyric).firstOrNull() ?: return@forEach
                        val time = mmssToMilliseconds(timeStampRaw) ?: 0
                        val text = lyric.substringAfter("]").trim()

                        lines.add(separateBackgroundLyrics(text, time))
                    }

                    return@withContext Lyrics(
                        syncType = SyncType.LINE,
                        source = LyricSource.UNISON,
                        credit = credit,
                        lines = lines
                    )
                }
                "plain" -> {
                    return@withContext Lyrics(
                        syncType = SyncType.NONE,
                        source = LyricSource.UNISON,
                        credit = credit,
                        lines = listOf(
                            LyricsLine(
                                startMs = -1,
                                lines = listOf(Lyric(data.lyrics))
                            )
                        )
                    )
                }
                else -> return@withContext null
            }
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