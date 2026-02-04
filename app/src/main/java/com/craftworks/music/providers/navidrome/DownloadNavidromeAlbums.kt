package com.craftworks.music.providers.navidrome

import android.content.Context
import androidx.media3.common.MediaItem

fun downloadNavidromeAlbum(context: Context, albumTitle: String, songs: List<MediaItem>) {
    songs.forEach { song ->
        downloadNavidromeSong(context, song.mediaMetadata, albumTitle)
    }
}