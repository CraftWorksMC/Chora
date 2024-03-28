package com.craftworks.music.data

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf

data class Artist (
    val name: String,
    val imageUri: Uri = Uri.parse("https://www.last.fm/music/Infected+Mushroom/+images/cb29d4fedad34896b1498278aef1946c"),
    //val songs: List<Song> = emptyList(),
    val navidromeID: String = "Local",
    val description: String = ""
)

var artistList:MutableList<Artist> = mutableStateListOf()