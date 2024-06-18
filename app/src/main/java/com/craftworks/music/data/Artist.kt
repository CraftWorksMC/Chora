package com.craftworks.music.data

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Artist (
    val name: String,
    val imageUri: Uri = Uri.parse("https://www.last.fm/music/Infected+Mushroom/+images/cb29d4fedad34896b1498278aef1946c"),
    //val songs: List<Song> = emptyList(),
    var navidromeID: String = "Local",
    val description: String = "",
    val similarArtistsID: String = ""
)

var artistList = mutableStateListOf<MediaData.Artist>()

var selectedArtist by mutableStateOf<MediaData.Artist>(
    MediaData.Artist(
        name = "My Favourite Artist",
        coverArt = "",
        navidromeID = "Local"
    )
)