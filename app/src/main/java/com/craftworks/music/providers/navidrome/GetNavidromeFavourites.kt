package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

@Serializable
@SerialName("starred")
data class Starred(
    val song: List<MediaData.Song>? = null,
    val album: List<MediaData.Album>? = null,
    val artist: List<MediaData.Artist>? = null
)

fun parseNavidromeFavouritesJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    val passwordSaltMedia = generateSalt(8)
    val passwordHashMedia = md5Hash(navidromePassword + passwordSaltMedia)
    val encodedUsername = URLEncoder.encode(navidromeUsername, "UTF-8")

    val mediaDataFavouriteSongs = mutableListOf<MediaItem>()

    mediaDataFavouriteSongs.addAll(subsonicResponse.starred?.song?.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora",
            imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
        ).toMediaItem()
    } ?: emptyList())

    Log.d("NAVIDROME", "Got favourite songs. Total: ${mediaDataFavouriteSongs.size}")

    return mediaDataFavouriteSongs
}