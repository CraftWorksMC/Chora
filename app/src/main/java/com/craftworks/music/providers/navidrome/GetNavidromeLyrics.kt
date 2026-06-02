package com.craftworks.music.providers.navidrome

import com.craftworks.music.data.model.MediaData
import kotlinx.serialization.Serializable

@Serializable
data class LyricsList(
    val structuredLyrics: List<MediaData.StructuredLyrics>? = null
)

@Serializable
data class SyncedLyrics(
    val start: Int? = null,
    val value: String
)

fun parseNavidromePlainLyricsJSON(
    response: String
): MediaData.PlainLyrics {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataPlainLyrics = subsonicResponse.lyrics!!

    return mediaDataPlainLyrics
}

fun parseNavidromeSyncedLyricsJSON(
    response: String
): List<MediaData.StructuredLyrics> {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataSyncedLyrics = subsonicResponse.lyricsList?.structuredLyrics ?: emptyList()

    return mediaDataSyncedLyrics
}