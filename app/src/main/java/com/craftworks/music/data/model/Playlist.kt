package com.craftworks.music.data.model

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi

var playlistList:MutableList<MediaData.Playlist> = mutableStateListOf()

data class Playlist (
    val name: String,
    var coverArt: Uri,
    var songs: List<MediaData.Song> = emptyList(),
    val navidromeID: String? = ""
)

@OptIn(UnstableApi::class)
fun MediaData.Playlist.toMediaItem(): MediaItem {
    val mediaMetadata = MediaMetadata.Builder()
        .setTitle(this@toMediaItem.name)
        .setDescription(this@toMediaItem.comment)
        .setArtworkUri(this@toMediaItem.coverArt?.toUri())
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
        .setDurationMs(this@toMediaItem.duration.toLong())
        .setExtras(Bundle().apply {
            putString("navidromeID", this@toMediaItem.navidromeID)
        })
        .build()

    return MediaItem.Builder()
        .setMediaId(this@toMediaItem.navidromeID.toString())
        .setMediaMetadata(mediaMetadata)
        .build()
}