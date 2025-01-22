package com.craftworks.music.data

import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable

var songsList: MutableList<MediaData.Song> = mutableStateListOf()

@Serializable
data class Genre(
    val name: String? = "")

@Serializable
data class ReplayGain(
    val trackGain: Float? = 0f,
    //val trackPeak: Float? = 0f,
    //val albumPeak: Float? = 0f
)