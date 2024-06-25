package com.craftworks.music.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class MediaData {
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
        val size: Int? = 0,
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
    ) : MediaData()

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
    ) : MediaData()

    @Serializable
    data class Artist(
        @SerialName("id")
        var navidromeID : String,
        val name : String,
        //val coverArt : String? = "",
        val artistImageUrl : String? = "",
        val albumCount : Int? = 0,
        var description : String = "",
        var starred : String? = "",
        var musicBrainzId : String? = "",
//        val sortName: String? = "",
        var similarArtist : List<Artist>? = null,
        var album : List<Album>? = null
    ) : MediaData()

    @Serializable
    data class ArtistInfo(
        val biography : String? = "",
        val musicBrainzId : String? = "",
        val lastFmUrl : String? = "",
        val similarArtist : List<Artist>? = null
    )

    @Serializable
    data class Playlist(
        @SerialName("id")
        val navidromeID: String,
        val name: String,
        val comment: String? = "",
        val owner: String? = "",
        val public: Boolean? = true,
        val created: String,
        val changed: String,
        val songCount: Int,
        val duration: Int,
        var coverArt: String? = "",
        @SerialName("entry")
        var songs: List<Song>? = listOf()
    ) : MediaData()
}