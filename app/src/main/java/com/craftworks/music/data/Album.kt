package com.craftworks.music.data

import android.os.Bundle
import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

var albumList:MutableList<MediaData.Album> = mutableStateListOf()

fun MediaData.Album.toMediaItem(): MediaItem {
    val mediaMetadata = MediaMetadata.Builder()
        .setTitle(this@toMediaItem.name)
        .setArtist(this@toMediaItem.artist)
        .setAlbumTitle(this@toMediaItem.name)
        .setDisplayTitle(this@toMediaItem.name)
        .setAlbumArtist(this@toMediaItem.artist)
        .setArtworkUri(this@toMediaItem.coverArt?.toUri())
        .setRecordingYear(this@toMediaItem.year)
        .setDurationMs(this@toMediaItem.duration.times(1000).toLong())
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .setGenre(this@toMediaItem.genres?.joinToString() { it.name ?: "" })
        .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
        .setExtras(
            Bundle().apply {
                putString("navidromeID", this@toMediaItem.navidromeID)
            }
        )
        .build()

    return MediaItem.Builder()
        .setMediaId(
            if (this@toMediaItem.navidromeID.startsWith("Local_"))
                "folder_album_" + this@toMediaItem.navidromeID
            else
                this@toMediaItem.navidromeID
        )
        .setMediaMetadata(mediaMetadata)
        .build()
}

fun MediaItem.toAlbum(): MediaData.Album {
    val mediaMetadata = this.mediaMetadata
    val extras = mediaMetadata.extras

    return MediaData.Album(
        navidromeID = extras?.getString("navidromeID") ?: "",
        name = mediaMetadata.albumTitle.toString(),
        artist = mediaMetadata.artist.toString(),
        year = mediaMetadata.releaseYear ?: 0,
        coverArt = mediaMetadata.artworkUri.toString(),
        duration = extras?.getInt("Duration") ?: 0,
        songs = mutableListOf(),
        songCount = 0,
        artistId = ""
    )
}