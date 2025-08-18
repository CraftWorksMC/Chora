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

@Serializable
data class albumList(val album: List<MediaData.Album>? = listOf())

fun parseNavidromeAlbumListJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
) : List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    // Generate password salt and hash for coverArt
    val passwordSaltArt = generateSalt(8)
    val passwordHashArt = md5Hash(navidromePassword + passwordSaltArt)

    val baseCoverArtUrl = "$navidromeUrl/rest/getCoverArt.view?u=$navidromeUsername&t=$passwordHashArt&s=$passwordSaltArt&v=1.16.1&c=Chora&size=128"

    val mediaDataAlbums = subsonicResponse.albumList?.album?.map {
        val mediaItem = it.toMediaItem()
        mediaItem.buildUpon().setMediaMetadata(
            mediaItem.mediaMetadata.buildUpon()
                .setArtworkUri("$baseCoverArtUrl&id=${it.navidromeID}".toUri())
                .build()
        ).build()
    } ?: emptyList()

    return mediaDataAlbums
        //.asSequence()
//        .filterNot { newAlbum ->
//            albumList.any { existingAlbum ->
//                existingAlbum.navidromeID == newAlbum.mediaMetadata.extras?.getString("navidromeID")
//            }
//        }
        //.toList()
}

@OptIn(UnstableApi::class)
fun parseNavidromeAlbumJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String,
): List<MediaItem> {
    val jsonParser = Json { ignoreUnknownKeys = true }
    val subsonicResponse = jsonParser.decodeFromJsonElement<SubsonicResponse>(
        jsonParser.parseToJsonElement(response).jsonObject["subsonic-response"]!!
    )

    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(navidromePassword + passwordSalt)

    val selectedAlbum = subsonicResponse.album
    val album = mutableListOf<MediaItem>()

    selectedAlbum?.songs?.map {
        it.media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.12.0&c=Chora"
        it.imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora&size=128"
    }

    // Create the Album MediaItem
    album.add(selectedAlbum?.toMediaItem() ?: MediaItem.EMPTY)

    println("Added album: ${selectedAlbum?.navidromeID}")

    album.addAll(selectedAlbum?.songs?.map {
        println("Added song to album: ${it.title}")
        it.toMediaItem()
    } ?: emptyList())

    return if (selectedAlbum != null) album
    else emptyList()
}