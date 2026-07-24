package com.craftworks.music.providers.subsonic

import androidx.compose.runtime.Immutable
import com.craftworks.music.data.model.ExplicitStatus
import com.craftworks.music.data.model.GainInfo
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.ProviderType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicProviderData(
    var url: String,
    var username: String,
    var credentials: String? = null,
    var allowSelfSignedCert: Boolean = false,
)

@Serializable
data class SubsonicResponse(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicBody
)

@Serializable
data class SubsonicBody(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,

    val user: SubsonicUser? = null,
    val musicFolders: SubsonicMusicFolders? = null,

    val searchResult3: SearchResult3? = null,
    val starred: Starred? = null,

    // Songs
    val song: SubsonicSong? = null,
    val songsByGenre: SubsonicSongList? = null,

    // Albums
    val albumList: SubsonicAlbumList? = null,
    val album: SubsonicAlbum? = null,
)

@Serializable
data class SubsonicMusicFolders(
    val musicFolder: List<SubsonicMusicFolder>
)

@Serializable
data class SubsonicMusicFolder(
    val id: Int,
    val name: String
)

@Serializable
data class SearchResult3(
    val artist: List<SubsonicArtist>? = null,
    val album: List<SubsonicAlbum>? = null,
    val song: List<SubsonicSong>? = null
)
@Serializable
data class Starred(
    val artist: List<SubsonicArtist>? = null,
    val album: List<SubsonicAlbum>? = null,
    val song: List<SubsonicSong>? = null
)

@Serializable
data class SubsonicSongList(
    val song: List<SubsonicSong>? = null
)
@Serializable
data class SubsonicSong(
    val id: String,
    val isDir: Boolean? = false,
    val title: String,
    val parent: String? = null,
    val album: String? = null,
    val artist: String = "",
    val artists: List<SubsonicArtist> = listOf(),
    val track: Int? = 0,
    val year: Int? = 0,
    val genre: String? = "",
    @SerialName("coverArt")
    var imageUrl: String? = "",
    val size: Int? = 0,
    val contentType: String? = "audio/flac",
    @SerialName("suffix")
    val format: String? = null,
    val duration: Int = 0,
    @SerialName("bitRate")
    val bitrate: Int? = 0,
    val bitDepth: Int? = 0,
    val path: String? = null,
    @SerialName("playCount")
    var timesPlayed: Int? = 0,
    val discNumber: Int? = 1,
    @SerialName("created")
    val dateAdded: String? = null,
    val albumId: String = "",
    val artistId: String? = "",
    val type: String? = "music",
    val isVideo: Boolean? = false,
    @SerialName("played")
    val lastPlayed: String? = "",
    val bpm: Int? = 0,
    val comment: String? = "",
    val sortName: String? = "",
    val mediaType: String? = "song",
    val musicBrainzId: String? = "",
    val genres: List<SubsonicItemGenre>? = listOf(),
    val replayGain: SubsonicReplayGain? = null,
    val channelCount: Int? = 2,
    val samplingRate: Int? = 0,
    val explicitStatus: String? = "",
    val displayArtist: String? = null,
    val displayAlbumArtist: String? = null,

    val isRadio: Boolean? = false,
    var media: String? = null,
    val trackIndex: Int? = 0,
    var starred: String? = null,
) {
    fun toMediaModel(providerId: String): MediaModel.Song = MediaModel.Song(
        album = this.album,
        albumArtistName = this.artist,
        albumArtists = this.artists.map { it.toMediaModel(providerId) },
        albumId = this.albumId,
        artistName = this.artist,
        artists = this.artists.map { it.toMediaModel(providerId) },
        bitDepth = this.bitDepth,
        bitRate = this.bitrate,
        bpm = this.bpm,
        channels = this.channelCount,
        comment = this.comment,
        compilation = null,
        container = this.contentType,
        createdAt = this.dateAdded,
        discNumber = this.discNumber ?: 1,
        discSubtitle = null,
        durationMs = this.duration * 1000,
        explicitStatus = null,
        gain = GainInfo(replayGain?.albumPeak?.toDouble(), replayGain?.trackGain?.toDouble()),
        genres = this.genres?.map { MediaModel.Genre(name = it.name) } ?: listOf(),
        imageId = this.imageUrl,
        imageUrl = this.imageUrl,
        lastPlayedAt = this.lastPlayed,
        lyrics = null,
        mbzRecordingId = this.musicBrainzId,
        mbzTrackId = null,
        name = this.title,
        participants = null,
        path = this.path,
        peak = null,
        playCount = this.timesPlayed,
        playlistItemId = null,
        releaseDate = null,
        releaseYear = this.year,
        sampleRate = this.samplingRate,
        size = this.size,
        sortName = this.sortName,
        tags = null,
        trackNumber = this.track ?: 0,
        trackSubtitle = null,
        updatedAt = null,
        userFavorite = !this.starred.isNullOrEmpty(),
        userRating = null
    ).apply {
        this.id = this@SubsonicSong.id
        this.providerId = providerId
        this.providerType = ProviderType.SUBSONIC
    }
}

@Serializable
data class SubsonicAlbumList(
    val album: List<SubsonicAlbum>? = null
)

@Serializable
data class SubsonicAlbum(
    val id: String,
    val name: String,
    val version: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int,
    val duration: Int,
    val playCount: Long? = null,
    val created: String,
    val starred: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val played: String? = null,
    val userRating: Int? = null,
    val recordLabels: List<SubsonicRecordLabel>? = null,
    val musicBrainzId: String? = null,
    val genres: List<SubsonicItemGenre>? = null,
    val artists: List<SubsonicArtist>? = null,
    val displayArtist: String? = null,
    val releaseTypes: List<String>? = null,
    val moods: List<String>? = null,
    val sortName: String? = null,
    val originalReleaseDate: SubsonicItemDate? = null,
    val releaseDate: SubsonicItemDate? = null,
    val isCompilation: Boolean? = null,
    val explicitStatus: String? = null,
    val discTitles: List<SubsonicDiscTitle>? = null,
    val song: List<SubsonicSong>? = null
) {
    fun toMediaModel(providerId: String): MediaModel.Album {
        val domainExplicitStatus = when (this.explicitStatus?.lowercase()) {
            "explicit" -> ExplicitStatus.EXPLICIT
            "clean" -> ExplicitStatus.CLEAN
            else -> null
        }

        return MediaModel.Album(
            albumArtistName = displayArtist,
            artists = this.artists?.map { it.toMediaModel(providerId) } ?: emptyList(),
            comment = null,
            createdAt = this.created,
            durationMs = this.duration,
            explicitStatus = domainExplicitStatus,
            genres = this.genres?.map { MediaModel.Genre(name = it.name) } ?: listOf(),
            imageId = this.coverArt,
            imageUrl = this.coverArt,
            isCompilation = this.isCompilation,
            lastPlayedAt = this.played,
            mbzId = this.musicBrainzId,
            mbzReleaseGroupId = null,
            name = this.name,
            originalDate = this.originalReleaseDate?.year?.toString(),
            originalYear = this.originalReleaseDate?.year ?: this.year,
            participants = mapOf(),
            playCount = this.playCount?.toDouble(),
            recordLabels = this.recordLabels?.map { it.name } ?: listOf(),
            releaseDate = this.releaseDate?.year?.toString(),
            releaseType = this.releaseTypes?.firstOrNull(),
            releaseTypes = this.releaseTypes ?: listOf(),
            releaseYear = this.year,
            size = null,
            songCount = this.songCount,
            songs = this.song?.map { it.toMediaModel(providerId) } ?: emptyList(),
            sortName = this.sortName,
            tags = if (!this.moods.isNullOrEmpty()) mapOf("moods" to this.moods) else mapOf(),
            updatedAt = null,
            userFavorite = !this.starred.isNullOrEmpty(),
            userRating = this.userRating,
            version = this.version
        ).apply {
            this.id = this@SubsonicAlbum.id
            this.providerId = providerId
            this.providerType = ProviderType.SUBSONIC
        }
    }
}

@Immutable
@Serializable
data class SubsonicRecordLabel(
    val name: String
)

@Immutable
@Serializable
data class SubsonicItemGenre(
    val name: String
)

@Immutable
@Serializable
data class SubsonicItemDate(
    val year: Int? = 0,
    val month: Int? = 0,
    val day: Int? = 0
)

@Immutable
@Serializable
data class SubsonicDiscTitle(
    val disc: Int,
    val title: String,
    val coverArt: String? = null
)

@Immutable
@Serializable
data class SubsonicReplayGain(
    val trackGain: Float? = 0f,
    val trackPeak: Float? = 0f,
    val albumPeak: Float? = 0f
)

@Immutable
@Serializable
data class SubsonicArtist(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val artistImageUrl: String? = null,
    val albumCount: Int? = null,
    val starred: String? = null,
    val musicBrainzId: String? = null,
    val sortName: String? = null,
    val roles: List<String>? = null
) {
    fun toMediaModel(providerId: String): MediaModel.Artist {
        return MediaModel.Artist(
            albumCount = albumCount,
            imageId = coverArt,
            imageUrl = artistImageUrl ?: coverArt,
            mbz = musicBrainzId,
            name = name,
            userFavorite = !this.starred.isNullOrEmpty()
        ).apply {
            this.id = this@SubsonicArtist.id
            this.providerId = providerId
            this.providerType = ProviderType.SUBSONIC
        }
    }
}

@Serializable
data class SubsonicError(val code: Int, val message: String)

@Serializable
data class SubsonicUser(
    val username: String,
    val adminRole: Boolean
)