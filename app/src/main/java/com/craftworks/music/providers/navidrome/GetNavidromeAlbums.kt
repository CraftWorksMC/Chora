package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class albumList(val album: List<MediaData.Album>? = listOf())

fun parseNavidromeAlbumListJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
) : List<MediaData.Album> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    // Generate password salt and hash for coverArt
    val passwordSaltArt = generateSalt(8)
    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)

    val baseCoverArtUrl = "$navidromeUrl/rest/getCoverArt.view?u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora"

    val mediaDataAlbums = subsonicResponse.albumList?.album ?: emptyList()

    return mediaDataAlbums
        .asSequence()
        .filterNot { newAlbum ->
            albumList.any { existingAlbum ->
                existingAlbum.navidromeID == newAlbum.navidromeID
            }
        }
        .onEach { album ->
            album.coverArt = "$baseCoverArtUrl&id=${album.navidromeID}"
        }
        .toList()
        .also {
            Log.d("NAVIDROME", "Added albums. Total: ${it.size}")
        }
}

@OptIn(UnstableApi::class)
fun parseNavidromeAlbumJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
): List<MediaData.Album> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(navidromePassword + passwordSalt)

    val selectedAlbum = subsonicResponse.album

    selectedAlbum?.songs?.map {
        it.media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"
        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora"
    }

    return if (selectedAlbum != null) listOf(selectedAlbum)
    else emptyList()
}
