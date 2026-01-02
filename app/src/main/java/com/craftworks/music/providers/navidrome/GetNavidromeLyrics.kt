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
import java.net.URLEncoder

@Serializable
data class LyricsList(
    val structuredLyrics: List<MediaData.StructuredLyrics>? = null
)

@Serializable
data class SyncedLyrics(
    val start: Int, val value: String
)

suspend fun getNavidromePlainLyrics(metadata: MediaMetadata?): List<Lyric> {
    val artist = URLEncoder.encode(metadata?.artist?.toString() ?: "", "UTF-8")
    val title = URLEncoder.encode(metadata?.title?.toString() ?: "", "UTF-8")
    return sendNavidromeGETRequest("getLyrics.view?artist=$artist&title=$title&f=json").filterIsInstance<MediaData.PlainLyrics>().getOrNull(0)?.toLyric()?.takeIf { it.content.isNotEmpty() }?.let { listOf(it) } ?: emptyList()
}

suspend fun getNavidromeSyncedLyrics(navidromeId: String): List<Lyric> {
    return sendNavidromeGETRequest("getLyricsBySongId.view?id=${navidromeId}&f=json")
        .filterIsInstance<MediaData.StructuredLyrics>().flatMap { it.toLyrics() }
}

fun parseNavidromePlainLyricsJSON(
    response: String
): MediaData.PlainLyrics {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return MediaData.PlainLyrics(value = "", artist = "", title = "")
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return MediaData.PlainLyrics(value = "", artist = "", title = "")
    }

    return subsonicResponse.lyrics ?: MediaData.PlainLyrics(value = "", artist = "", title = "")
}

fun parseNavidromeSyncedLyricsJSON(
    response: String
): List<MediaData.StructuredLyrics> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    return subsonicResponse.lyricsList?.structuredLyrics ?: emptyList()
}