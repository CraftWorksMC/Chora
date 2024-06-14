package com.craftworks.music.data

import android.net.Uri
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.LinkedList

//data class Song (
//    val imageUrl: Uri,
//    val title: String,
//    val artist: String,
//    val album: String,
//    val duration: Int,
//    val isRadio: Boolean? = false,
//    val media: Uri? = null,
//    var timesPlayed: Int? = 0,
//    val dateAdded: String? = "",
//    val year: String? = "",
//    val format: String? = "MP3",
//    val bitrate: String? = "320",
//    val navidromeID: String? = "",
//    val lastPlayed: String? = "",
//    val trackIndex: Int? = 0
//)

@Serializable
data class Song(
    @SerialName("id")
    val navidromeID: String,
    val parent: String,
    val isDir: Boolean? = false,
    val title: String,
    val album: String,
    val artist: String,
    val track: Int? = 0,
    val year: Int? = 0,
    val genre: String? = "",
    @SerialName("coverArt")
    var imageUrl: String,
    val size: Int,
    val contentType: String? = "music",
    @SerialName("suffix")
    val format: String,
    val duration: Int = 0,
    val bitrate: Int? = 0,
    val path: String,
    @SerialName("playCount")
    var timesPlayed: Int? = 0,
    val discNumber: Int? = 0,
    @SerialName("created")
    val dateAdded: String,
    val albumId: String,
    val artistId: String? = "",
    val type: String? = "music",
    val isVideo: Boolean? = false,
    @SerialName("played")
    val lastPlayed: String? = "",
    val bpm: Int,
    val comment: String? = "",
    val sortName: String? = "",
    val mediaType: String? = "song",
    val musicBrainzId: String? = "",
    val genres: List<Genre>? = listOf(),
    val replayGain: ReplayGain? = null,
    val channelCount: Int? = 2,
    val samplingRate: Int? = 0,

    val isRadio: Boolean? = false,
    var media: String? = null,
    val trackIndex: Int? = 0
)

@Serializable
data class Genre(
    val name: String? = "")

@Serializable
data class ReplayGain(
    val trackGain: Float? = 0f,
    val trackPeak: Float? = 0f,
    val albumPeak: Float? = 0f)

@Serializable
data class SearchResult3(val song: List<Song>)

//@Serializable
//data class SongJson(
//    val id : String,
//    val parent : String,
//    val isDir : Boolean,
//    val title : String,
//    val album : String,
//    val artist : String,
//    val track : Int? = 0,
//    val year : Int? = 0,
//    val genre : String? = "",
//    val coverArt : String,
//    val size : Int,
//    val contentType : String,
//    val suffix : String,
//    val duration: Int ? = 0,
//    val bitrate: Int? = 0,
//    val path : String,
//    val playCount : Int? = 0,
//    val discNumber: Int? = 0,
//    val created : String,
//    val albumId : String,
//    val artistId : String? = "",
//    val type : String,
//    val isVideo : Boolean,
//    val played : String? = "",
//    val bpm : Int,
//    val comment : String? = "",
//    val sortName : String? = "",
//    val mediaType : String,
//    val musicBrainzId : String? = "",
//    val genres : List<Genre>,
//    val replayGain : ReplayGain? = null,
//    val channelCount : Int? = 2,
//    val samplingRate : Int? = 0
//
//    // OpenSubsonic
//)


var songsList: MutableList<Song> = mutableStateListOf()