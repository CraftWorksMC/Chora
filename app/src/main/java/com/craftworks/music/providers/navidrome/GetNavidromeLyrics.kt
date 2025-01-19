package com.craftworks.music.providers.navidrome

import com.craftworks.music.data.Lyric
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.toLyric
import com.craftworks.music.data.toLyrics
import com.craftworks.music.player.SongHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class LyricsList(
    val structuredLyrics: List<MediaData.StructuredLyrics>? = null
)

@Serializable
data class SyncedLyrics(
    val start: Int, val value: String
)

suspend fun getNavidromePlainLyrics(): List<Lyric> {
    // Get plain lyrics from navidrome, if it doesn't exist, return an empty list
    return sendNavidromeGETRequest("getLyrics.view?artist=${SongHelper.currentSong.artist}&title=${SongHelper.currentSong.title}&f=json").filterIsInstance<MediaData.PlainLyrics>().getOrNull(0)?.toLyric()?.takeIf { it.content.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
}

suspend fun getNavidromeSyncedLyrics(): List<Lyric> {
    return sendNavidromeGETRequest("getLyricsBySongId.view?id=${SongHelper.currentSong.navidromeID}&f=json")
        .filterIsInstance<MediaData.StructuredLyrics>().flatMap { it.toLyrics() }
}

fun parseNavidromePlainLyricsJSON(
    response: String
): MediaData {

    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val mediaDataPlainLyrics = subsonicResponse.lyrics!!

    return mediaDataPlainLyrics
}

fun parseNavidromeSyncedLyricsJSON(
    response: String
): List<MediaData.StructuredLyrics> {

    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val mediaDataSyncedLyrics = subsonicResponse.lyricsList?.structuredLyrics ?: emptyList()

    return mediaDataSyncedLyrics
}