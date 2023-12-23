package com.craftworks.music.data

import android.net.Uri

data class Playlist (
    val name: String,
    val coverArt: Uri,
    val songs: List<Song> = emptyList(),
    val navidromeID: String? = ""
)