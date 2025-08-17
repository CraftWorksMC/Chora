package com.craftworks.music.providers.navidrome

import androidx.media3.common.MediaMetadata
import com.craftworks.music.data.model.Lyric
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toLyric
import com.craftworks.music.data.model.toLyrics
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

suspend fun getNavidromePlainLyrics(metadata: MediaMetadata?): List<Lyric> {
    return sendNavidromeGETRequest("getLyrics.view?artist=${metadata?.artist}&title=${metadata?.title}&f=json").filterIsInstance<MediaData.PlainLyrics>().getOrNull(0)?.toLyric()?.takeIf { it.content.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
}

suspend fun getNavidromeSyncedLyrics(navidromeId: String): List<Lyric> {
    return sendNavidromeGETRequest("getLyricsBySongId.view?id=${navidromeId}&f=json")
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