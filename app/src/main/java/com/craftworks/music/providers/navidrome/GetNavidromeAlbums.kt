package com.craftworks.music.providers.navidrome

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

@Serializable
data class albumList(val album: List<MediaData.Album>? = listOf())

fun parseNavidromeAlbumListJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
) : List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    // Generate password salt and hash for coverArt
    val passwordSaltArt = generateSalt(8)
    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)
    val encodedUsername = URLEncoder.encode(navidromeUsername, "UTF-8")

    val baseCoverArtUrl = "$navidromeUrl/rest/getCoverArt.view?u=$encodedUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora&size=128"

    val mediaDataAlbums = subsonicResponse.albumList?.album?.map {
        val mediaItem = it.toMediaItem()
        mediaItem.buildUpon().setMediaMetadata(
            mediaItem.mediaMetadata.buildUpon()
                .setArtworkUri("$baseCoverArtUrl&id=${it.navidromeID}".toUri())
                .build()
        ).build()
    } ?: emptyList()

    return mediaDataAlbums
}

@OptIn(UnstableApi::class)
fun parseNavidromeAlbumJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
): List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val jsonElement = jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]
        ?: return emptyList()
    val subsonicResponse = try {
        jsonParser.decodeFromJsonElement<SubsonicResponse>(jsonElement)
    } catch (e: Exception) {
        return emptyList()
    }

    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(navidromePassword + passwordSalt)
    val encodedUsername = URLEncoder.encode(navidromeUsername, "UTF-8")

    val selectedAlbum = subsonicResponse.album ?: return emptyList()

    val album = mutableListOf<MediaItem>()

    val updatedSongs = selectedAlbum.songs?.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora",
            imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$encodedUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora&size=128"
        )
    }

    // Create the Album MediaItem
    album.add(selectedAlbum.toMediaItem())

    album.addAll(updatedSongs?.map {
        it.toMediaItem()
    } ?: emptyList())

    return album
}