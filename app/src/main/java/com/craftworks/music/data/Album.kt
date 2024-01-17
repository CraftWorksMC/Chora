package com.craftworks.music.data

import android.net.Uri

data class Album (
    val name: String,
    val artist: String,
    val year: String,
    val coverArt: Uri,
    val songs: List<Song> = emptyList(),
    val navidromeID: String? = ""
)