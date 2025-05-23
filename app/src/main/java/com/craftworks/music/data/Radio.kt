package com.craftworks.music.data

import android.os.Bundle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.craftworks.music.R

var radioList:SnapshotStateList<MediaData.Radio> = mutableStateListOf()

fun MediaData.Radio.toMediaItem(): MediaItem {
    val mediaMetadata =
        MediaMetadata.Builder()
            .setStation(this@toMediaItem.name)
            .setArtist(this@toMediaItem.name)
            .setArtworkUri(
                ("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder).toUri()
            )
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(Bundle().apply {
                putString("navidromeID", this@toMediaItem.navidromeID)
            }).build()

    return MediaItem.Builder()
        .setMediaId(this@toMediaItem.media.toString())
        .setUri(this@toMediaItem.media)
        .setMediaMetadata(mediaMetadata)
        .build()
}