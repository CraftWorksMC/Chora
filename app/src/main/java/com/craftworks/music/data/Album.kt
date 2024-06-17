package com.craftworks.music.data

import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Album(
    @SerialName("id")
    val navidromeID : String,
    val parent : String? = "",

    val album : String? = "",
    val title : String? = "",
    val name : String? = "",

    val isDir : Boolean? = false,
    var coverArt : String?,
    val songCount : Int,

    val played : String? = "",
    val created : String? = "",
    val duration : Int,
    val playCount : Int? = 0,

    val artistId : String?,
    val artist : String,
    val year : Int? = 0,
    val genre : String? = "",
    val genres : List<Genre>? = listOf(),

    @SerialName("song")
    var songs: List<Song>? = listOf()
)

var albumList:MutableList<Album> = mutableStateListOf()