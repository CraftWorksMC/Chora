package com.craftworks.music.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable

var SyncedLyric = mutableStateListOf<Lyric>()
var PlainLyrics by mutableStateOf("Getting Lyrics...")

// Universal Lyric object
data class Lyric(
    val timestamp: Int,
    val content: String
)


// LRCLIB Lyrics
@Serializable
data class LrcLibLyrics(
    val id: Int,
    val instrumental: Boolean,
    val plainLyrics: String? = "",
    val syncedLyrics: String? = ""
)

//region Convert proprietary lyric format to app format.

fun MediaData.PlainLyrics.toLyric(): Lyric {
    return Lyric(
        timestamp = -1,
        content = value
    )
}

fun MediaData.StructuredLyrics.toLyrics(): List<Lyric> {
    return line.map { syncedLyric ->
        Lyric(
            // If not synced lyrics, set timestamp to -1
            timestamp = if (synced) syncedLyric.start + (offset ?: 0) else -1,
            content = syncedLyric.value
        )
    }
}

fun LrcLibLyrics.toLyrics(): List<Lyric> {
    if (instrumental) return listOf(Lyric(-1, ""))
    else if (syncedLyrics.toString() != "null") {
        val result = mutableListOf<Lyric>()

        syncedLyrics?.lines()?.forEach { lyric ->
            val timeStampsRaw = getTimeStamps(lyric)[0]
            val time = mmssToMilliseconds(timeStampsRaw)
            val lyricText: String = lyric.drop(11)

            result.add(Lyric(time.toInt(), lyricText))
        }

        return result
    }
    else if (plainLyrics.toString() != "null")
        return listOf(Lyric(-1, plainLyrics.toString()))
    else
        return listOf(Lyric(-1, "No Lyrics Found"))
}

//endregion

fun mmssToMilliseconds(mmss: String): Long {
    val parts = mmss.split(":", ".")
    if (parts.size == 3) {
        try {
            val minutes = parts[0].toLong()
            val seconds = parts[1].toLong()
            val ms = parts[2].toLong()
            return (minutes * 60 + seconds) * 1000 + ms * 10
        } catch (e: NumberFormatException) {
            // Handle the case where the input is not in the expected format
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