package com.craftworks.music.providers.navidrome

import android.util.Log
import androidx.media3.common.MediaItem
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.data.model.MediaItem
import com.craftworks.music.data.model.toMediaItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
@Serializable
@SerialName("starred")
data class Starred(
    val song: List<com.craftworks.music.data.model.MediaItem.Song>? = null,
    val album: List<com.craftworks.music.data.model.MediaItem.Album>? = null,
    val artist: List<com.craftworks.music.data.model.MediaItem.Artist>? = null
)

fun parseNavidromeFavouritesJSON(
    response: String,
    navidromeUrl: String,
    navidromeUsername: String,
    navidromePassword: String
) : List<MediaItem> {
    val subsonicResponse = parseSubsonicResponse(response)

    val passwordSaltMedia = NavidromeDataSource.generateSalt(8)
    val passwordHashMedia = NavidromeDataSource.md5Hash(navidromePassword + passwordSaltMedia)

    val mediaDataFavouriteSongs = mutableListOf<MediaItem>()

    mediaDataFavouriteSongs.addAll(subsonicResponse.starred?.song?.map {
        it.copy(
            media = "$navidromeUrl/rest/stream.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.12.0&c=Chora",
            imageUrl = "$navidromeUrl/rest/getCoverArt.view?&id=${it.navidromeID}&u=$navidromeUsername&t=$passwordHashMedia&s=$passwordSaltMedia&v=1.16.1&c=Chora&size=128"
        ).toMediaItem()
    } ?: emptyList())

    Log.d("NAVIDROME", "Got favourite songs. Total: ${mediaDataFavouriteSongs.size}")

    return mediaDataFavouriteSongs
}