package com.craftworks.music.data.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.craftworks.music.R
import com.craftworks.music.utils.getTimeStamps
import com.craftworks.music.utils.mmssToMilliseconds
import com.craftworks.music.utils.separateBackgroundLyrics
import kotlinx.serialization.Serializable
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

enum class LyricsRole {
    MAIN, BG
}

enum class SyncType {
    NONE, LINE, WORD
}

enum class LyricSource{
    MEDIA_PROVIDER, LRCLIB, BINI_LYRICS, UNISON, NETEASE;

    val displayName: String
        @Composable
        get() = when (this) {
            MEDIA_PROVIDER -> stringResource(R.string.settings_media_providers)
            LRCLIB -> "LRCLIB"
            BINI_LYRICS -> "BiniLyrics"
            UNISON -> "Unison"
            NETEASE -> "NetEase"
        }

    val icon: Int
        get() = when (this) {
            MEDIA_PROVIDER -> R.drawable.s_m_media_providers
            LRCLIB -> R.drawable.lrclib_logo
            BINI_LYRICS -> R.drawable.binilyrics_logo
            UNISON -> R.drawable.unison_logo
            NETEASE -> R.drawable.netease_cloud_music
        }
}

@Serializable
data class LyricsProvider(
    val source: LyricSource,
    val enabled: Boolean = true
)

// Universal Lyric object
@Stable
data class Lyrics(
    val syncType: SyncType,
    val source: LyricSource,
    val lines: List<LyricsLine>,
    val agents: List<LyricsAgent> = emptyList()
)
@Stable
data class LyricsLine(
    val startMs: Int,
    val endMs: Int? = null,
    val lines: List<Lyric>,
    val agentId: String? = null,
)
@Stable
data class Lyric(
    val text: String,
    val words: List<SyncedWord>? = null,
    val startMs: Int? = null,
    val endMs: Int? = null,
    val role: LyricsRole = LyricsRole.MAIN
)
@Stable
data class SyncedWord(
    val text: String,
    val startMs: Int,
    val endMs: Int?
)
enum class LyricsAgentType {
    PERSON, GROUP, OTHER
}
@Stable
data class LyricsAgent(
    val id: String,
    val type: LyricsAgentType = LyricsAgentType.PERSON,
    val name: String? = null
)

// LRCLIB Lyrics
@Serializable
data class LrcLibLyrics(
    val id: Int,
    val instrumental: Boolean,
    val plainLyrics: String? = "",
    val syncedLyrics: String? = "",
    val lyricsfile: String? = "",
)

// NetEase Lyrics
@Serializable
data class NeteaseLyricsResponse(
    val pureMusic: Boolean? = false,
    val lrc: NeteaseLrc? = null,
    val tlyric: NeteaseLrc? = null   // translation, may be absent or empty
)
@Serializable
data class NeteaseLrc(
    val lyric: String? = null
)

// Binimum Lyrics
@Serializable
data class BiniLyricsResponse(
    val results: List<BiniLyricsResult>
)
@Serializable
data class BiniLyricsResult(
    val timing_type: String,
    val lyricsUrl: String
)

// Unison Lyrics
@Serializable
data class UnisonLyricsResponse(
    val success: Boolean,
    val data: UnisonLyricsData? = null
)
@Serializable
data class UnisonSearchResponse(
    val success: Boolean,
    val data: List<UnisonLyricsData>? = null
)

@Serializable
data class UnisonLyricsData(
    val id: Int,
    val lyrics: String? = null,
    val format: String
)

fun LrcLibLyrics.toLyrics(): Lyrics? {
    if (instrumental) return null

    if (lyricsfile.toString() != "null") {
        val settings = LoadSettings.builder().build()
        val raw = Load(settings).loadFromString(lyricsfile) as? Map<*, *>
            ?: throw IllegalArgumentException("Invalid YAML format")

        val linesList = raw["lines"] as? List<*> ?: emptyList<Any>()
        val plainText = raw["plain"] as? String
        var wordSynced = false

        val lines: List<LyricsLine> = if (linesList.isNotEmpty()) {
            linesList.mapNotNull { lineItem ->
                val lineMap = lineItem as? Map<*, *> ?: return@mapNotNull null

                val startMs = lineMap["start_ms"]?.toString()?.toIntOrNull() ?: 0
                val endMs = lineMap["end_ms"]?.toString()?.toIntOrNull() ?: 0

                val wordsList = lineMap["words"] as? List<*> ?: emptyList<Any>()
                val words = wordsList.mapNotNull { wordItem ->
                    val wordMap = wordItem as? Map<*, *> ?: return@mapNotNull null
                    SyncedWord(
                        text = wordMap["text"]?.toString() ?: "",
                        startMs = wordMap["start_ms"] as? Int ?: 0,
                        endMs = wordMap["end_ms"] as? Int
                    )
                }

                if (words.isEmpty()) {
                    val rawText = lineMap["text"]?.toString()?.trim() ?: return@mapNotNull null
                    separateBackgroundLyrics(rawText, startMs, endMs)
                } else {
                    wordSynced = true
                    separateBackgroundLyrics(words, startMs, endMs)
                }
            }
        } else if (!plainText.isNullOrBlank()) {
            listOf(LyricsLine(
                startMs = -1,
                lines = listOf(
                    Lyric(plainText)
                )
            ))
        } else {
            emptyList()
        }

        val syncType = when {
            wordSynced -> SyncType.WORD
            linesList.isNotEmpty() -> SyncType.LINE
            else -> SyncType.NONE
        }
        return Lyrics(
            syncType = syncType,
            source = LyricSource.LRCLIB,
            lines = lines
        )
    }
    else if (syncedLyrics != null) {
        val lines = mutableListOf<LyricsLine>()

        syncedLyrics.lines().forEach { lyric ->
            if (lyric.isBlank()) return@forEach
            val timeStampRaw = getTimeStamps(lyric).firstOrNull() ?: return@forEach
            val time = mmssToMilliseconds(timeStampRaw) ?: 0
            val text = lyric.substringAfter("]").trim()

            lines.add(separateBackgroundLyrics(text, time))
        }

        return Lyrics(
            syncType = SyncType.LINE,
            source = LyricSource.LRCLIB,
            lines = lines
        )
    }
    else if (plainLyrics != null) {
        return Lyrics(
            syncType = SyncType.NONE,
            source = LyricSource.LRCLIB,
            lines = listOf(
            LyricsLine(
                startMs = -1,
                lines = listOf(Lyric(text = plainLyrics)))
            )
        )
    }
    else
        return null
}

fun NeteaseLyricsResponse.toLyrics(): Lyrics? {
    if (pureMusic == true)
        return null

    val originalMap = mutableMapOf<Int, String>()
    val translationMap = mutableMapOf<Int, String>()

    lrc?.lyric?.lines()?.forEach { line ->
        val tags = getTimeStamps(line)
        if (tags.isEmpty()) return@forEach
        val text = line.substringAfter("]").trim()
        tags.forEach { tag ->
            val time = mmssToMilliseconds(tag) ?: 0
            originalMap[time] = text
        }
    }
    if (!tlyric?.lyric.isNullOrEmpty()) {
        tlyric.lyric.lines().forEach { line ->
            val tags = getTimeStamps(line)
            if (tags.isEmpty()) return@forEach
            // Group lines sharing the same timestamp
            val text = line.substringAfter("]").trim()
            tags.forEach { tag ->
                val time = mmssToMilliseconds(tag) ?: 0
                translationMap[time] = text
            }
        }
    }

    return Lyrics(
        syncType = SyncType.LINE,
        source = LyricSource.NETEASE,
        lines = originalMap
            .map { (timestamp, origLine) ->
                separateBackgroundLyrics(origLine, timestamp)
            }
    )
}