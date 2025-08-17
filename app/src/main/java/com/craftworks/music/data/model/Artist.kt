package com.craftworks.music.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

var artistList: MutableList<MediaData.Artist> = mutableStateListOf()

var selectedArtist by mutableStateOf(
    MediaData.Artist(
        name = "My Favourite Artist",
        artistImageUrl = "",
        navidromeID = "Local"
    )
)