package com.craftworks.music.legacy.providers.navidrome

import com.craftworks.music.data.model.MediaItem
import kotlinx.serialization.Serializable

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
@Serializable
data class LyricsList(
    val structuredLyrics: List<MediaItem.StructuredLyrics>? = null
)

@Serializable
data class SyncedLyrics(
    val start: Int? = null,
    val value: String
)

fun parseNavidromePlainLyricsJSON(
    response: String
): MediaItem.PlainLyrics {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataPlainLyrics = subsonicResponse.lyrics!!

    return mediaDataPlainLyrics
}

fun parseNavidromeSyncedLyricsJSON(
    response: String
): List<MediaItem.StructuredLyrics> {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataSyncedLyrics = subsonicResponse.lyricsList?.structuredLyrics ?: emptyList()

    return mediaDataSyncedLyrics
}