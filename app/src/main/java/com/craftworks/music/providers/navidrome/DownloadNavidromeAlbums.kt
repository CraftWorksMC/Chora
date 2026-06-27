package com.craftworks.music.legacy.providers.navidrome

import android.content.Context
import androidx.media3.common.MediaItem

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
fun downloadNavidromeAlbum(context: Context, albumTitle: String, songs: List<MediaItem>) {
    songs.forEach { song ->
        downloadNavidromeSong(context, song.mediaMetadata, albumTitle)
    }
}