package com.craftworks.music.data.model

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import java.util.Collections

/**
 * @deprecated Global mutable state is deprecated. Use PlaylistRepository instead.
 * This is kept for backwards compatibility with legacy code.
 * TODO: Remove once all usages are migrated to repository pattern.
 */
@Deprecated("Use PlaylistRepository instead of global mutable state")
val playlistList: MutableList<MediaData.Playlist> = Collections.synchronizedList(mutableListOf())

@Stable
data class Playlist (
    val name: String,
    val coverArt: Uri,
    val songs: List<MediaData.Song> = emptyList(),
    val navidromeID: String? = ""
)

@OptIn(UnstableApi::class)
fun MediaData.Playlist.toMediaItem(): MediaItem {
    val mediaMetadata = MediaMetadata.Builder()
        .setTitle(this@toMediaItem.name)
        .setDescription(this@toMediaItem.comment)
        .setArtworkUri(this@toMediaItem.coverArt?.toUri())
        .setArtworkData(this@toMediaItem.coverArt?.toByteArray(), MediaMetadata.PICTURE_TYPE_OTHER)
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