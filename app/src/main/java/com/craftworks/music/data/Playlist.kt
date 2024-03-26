package com.craftworks.music.data

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf

var playlistList:MutableList<Playlist> = mutableStateListOf()

data class Playlist (
    val name: String,
    val coverArt: Uri,
    var songs: List<Song> = emptyList(),
    val navidromeID: String? = ""
)