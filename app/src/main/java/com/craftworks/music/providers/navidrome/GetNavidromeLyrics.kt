package com.craftworks.music.legacy.providers.navidrome

import com.craftworks.music.data.model.MediaModel
import kotlinx.serialization.Serializable

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
@Serializable
data class LyricsList(
    val structuredLyrics: List<MediaModel.StructuredLyrics>? = null
)

@Serializable
data class SyncedLyrics(
    val start: Int? = null,
    val value: String
)

fun parseNavidromePlainLyricsJSON(
    response: String
): MediaModel.PlainLyrics {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataPlainLyrics = subsonicResponse.lyrics!!

    return mediaDataPlainLyrics
}

fun parseNavidromeSyncedLyricsJSON(
    response: String
): List<MediaModel.StructuredLyrics> {
    val subsonicResponse = parseSubsonicResponse(response)

    val mediaDataSyncedLyrics = subsonicResponse.lyricsList?.structuredLyrics ?: emptyList()

    return mediaDataSyncedLyrics
}