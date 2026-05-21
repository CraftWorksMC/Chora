package com.craftworks.music.data.model

import android.util.Log
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

// Universal Lyric object
@Stable
data class Lyric(
    val timestamp: Int,
    val content: List<String>
)

// LRCLIB Lyrics
@Serializable
data class LrcLibLyrics(
    val id: Int,
    val instrumental: Boolean,
    val plainLyrics: String? = "",
    val syncedLyrics: String? = ""
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

//region Convert lyric format to app format.
fun MediaData.PlainLyrics.toLyric(): Lyric {
    return Lyric(
        timestamp = -1,
        content = listOf(value)
    )
}

fun MediaData.StructuredLyrics.toLyrics(): List<Lyric> {
    return line
        .groupBy { if (synced) it.start + (offset ?: 0) else -1 }
        .map { (timestamp, lines) ->
            Lyric(
                timestamp = timestamp,
                content = lines.map { it.value }
            )
        }
        .sortedBy { it.timestamp }
}

fun LrcLibLyrics.toLyrics(): List<Lyric> {
    if (instrumental) return listOf()

    else if (syncedLyrics.toString() != "null") {
        val raw = mutableListOf<Pair<Int, String>>()
        syncedLyrics?.lines()?.forEach { lyric ->
            val timeStampsRaw = getTimeStamps(lyric)[0]
            val time = mmssToMilliseconds(timeStampsRaw).toInt()
            val lyricText = lyric.substringAfter("]").trim()
            raw.add(Pair(time, lyricText))
        }

        return raw
            .groupBy { it.first }
            .map { (time, lines) -> Lyric(time, lines.map { it.second }) }
            .sortedBy { it.timestamp }
    }
    else if (plainLyrics.toString() != "null") {
        Log.d("LYRICS", "Got LRCLIB plain lyrics: $plainLyrics")
        return listOf(Lyric(-1, listOf(plainLyrics.toString())))
    }
    else
        return listOf()
}

fun NeteaseLyricsResponse.toLyrics(): List<Lyric> {
    if (pureMusic == true)
        return emptyList()

    val originalMap = mutableMapOf<Int, String>()
    val translationMap = mutableMapOf<Int, String>()

    lrc?.lyric?.lines()?.forEach { line ->
        val tags = getTimeStamps(line)
        if (tags.isEmpty()) return@forEach
        val text = line.substringAfter("]").trim()
        tags.forEach { tag ->
            val time = mmssToMilliseconds(tag).toInt()
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
                val time = mmssToMilliseconds(tag).toInt()
                translationMap[time] = text
            }
        }
    }

    // Group lines sharing the same timestamp
    return originalMap
        .map { (timestamp, origLine) ->
            val lines = buildList {
                add(origLine)
                translationMap[timestamp]?.let { add(it) }
            }
            Lyric(timestamp = timestamp, content = lines)
        }
        .sortedBy { it.timestamp }
}

//endregion

fun mmssToMilliseconds(mmss: String): Long {
    val parts = mmss.split(":", ".")
    if (parts.size == 3) {
        try {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            val ms = parts[2].substring(0,2).toLong()
            return (minutes * 60 + seconds) * 1000 + ms * 10
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }
    return 0L
}

fun getTimeStamps(input: String): List<String> {
    val regex = Regex("\\[(.*?)]")
    val matches = regex.findAll(input)

    val result = mutableListOf<String>()
    for (match in matches) {
        result.add(match.groupValues[1])
    }

    return result
}