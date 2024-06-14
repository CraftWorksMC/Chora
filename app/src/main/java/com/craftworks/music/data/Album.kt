package com.craftworks.music.data

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable

@Serializable
data class AlbumJson(
    val id: String,
    val parent: String,

    val album: String,
    val title: String,
    val name: String,

    val isDir: Boolean,
    val coverArt: String,
    val songCount: Int,

    val created: String,
    val duration: Int,
    val playCount: Int,

    val artistId: String,
    val artist: String,
    val year: Int,
    val genre: String,
    val song: List<Song>
)

data class Album (
    val name: String,
    val artist: String,
    val year: String,
    val coverArt: Uri,
    val songs: List<Song> = emptyList(),
    val navidromeID: String? = "Local",
    val dateAdded: String? = "",
    val datePlayed: String? = "",
    val timesPlayed: Int? = 0
)



var albumList:MutableList<Album> = mutableStateListOf()