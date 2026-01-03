package com.craftworks.music.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.craftworks.music.providers.navidrome.SyncedLyrics
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class MediaData {
    @Stable
    @Serializable
    data class Song(
        @SerialName("id")
        val navidromeID: String,
        val parent: String,
        val isDir: Boolean? = false,
        val title: String,
        val album: String,
        val artist: String,
        val artists: List<Artists> = listOf(),
        val track: Int? = 0,
        val year: Int? = 0,
        val genre: String? = "",
        @SerialName("coverArt")
        val imageUrl: String,
        val size: Int? = 0,
        val contentType: String? = "audio/flac",
        @SerialName("suffix")
        val format: String,
        val duration: Int = 0,
        @SerialName("bitRate")
        val bitrate: Int? = 0,
        val path: String,
        @SerialName("playCount")
        val timesPlayed: Int? = 0,
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
        val media: String? = null,
        val trackIndex: Int? = 0,
        val starred: String? = null,
    ) : MediaData()

    @Stable
    @Serializable
    data class Album(
        @SerialName("id")
        val navidromeID : String,
        val parent : String? = "",

        val album : String? = "",
        val title : String? = "",
        val name : String? = "",

        val isDir : Boolean? = false,
        val coverArt : String?,
        val songCount : Int,

        val played : String? = "",
        val created : String? = "",
        val duration : Int? = 0,
        val playCount : Int? = 0,

        val artistId : String?,
        val artist : String,
        val year : Int? = 0,
        val genre : String? = "",
        val genres : List<Genre>? = listOf(),

        val starred: String? = null,

        @SerialName("song")
        val songs: List<Song>? = listOf()
    ) : MediaData()

    @Stable
    @Serializable
    data class Artist(
        @SerialName("id")
        val navidromeID : String = "",
        val name : String = "",
        //val coverArt : String? = "",
        val artistImageUrl : String? = null,
        val albumCount : Int? = 0,
        val description : String = "",
        val starred : String? = "",
        val musicBrainzId : String? = "",
//        val sortName: String? = "",
        val similarArtist : List<Artist>? = null,
        val album : List<Album>? = null
    ) : MediaData()

    @Stable
    @Serializable
    data class ArtistInfo(
        val biography : String? = "",
        val musicBrainzId : String? = "",
        val lastFmUrl : String? = "",
        val similarArtist : List<Artist>? = null
    )

    @Immutable
    @Serializable
    data class Radio(
        @SerialName("id")
        val navidromeID: String,
        val name: String,
        @SerialName("streamUrl")
        val media: String,
        val homePageUrl: String? = "",
    ) : MediaData()

    @Stable
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
        val coverArt: String? = "",
        @SerialName("entry")
        val songs: List<Song>? = listOf()
    ) : MediaData()

    @Immutable
    @Serializable
    data class PlainLyrics(
        val value: String,
        val artist: String? = "",
        val title: String? = ""
    ) : MediaData()

    @Stable
    @Serializable
    data class StructuredLyrics(
        val displayArtist: String? = "",
        val displayTitle: String? = "",
        val lang: String,
        val offset: Int? = 0,
        val synced: Boolean,
        val line: List<SyncedLyrics>
    ) : MediaData()
}