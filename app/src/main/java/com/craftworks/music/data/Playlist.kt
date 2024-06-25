package com.craftworks.music.data

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf

var playlistList:MutableList<MediaData.Playlist> = mutableStateListOf()

data class Playlist (
    val name: String,
    var coverArt: Uri,
    var songs: List<MediaData.Song> = emptyList(),
    val navidromeID: String? = ""
)