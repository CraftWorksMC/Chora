package com.craftworks.music.data.providers.media.navidrome

import com.craftworks.music.data.model.GainInfo
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.ProviderType
import kotlinx.serialization.Serializable

@Serializable
data class NavidromeLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class NavidromeLoginResponse(
    var token: String
)

@Serializable
data class NavidromeAlbum(
    val albumArtist: String,
    val albumArtistId: String,
    val allArtistIds: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val catalogNum: String? = null,
    val comment: String? = null,
    val compilation: Boolean? = null,
    val createdAt: String? = null,
    val duration: Double? = null,
    val explicitStatus: String? = null,
    val externalInfoUpdatedAt: String? = null,
    val externalUrl: String? = null,
    val fullText: String? = null,
    val genre: String? = null,
    val genres: List<NavidromeGenre>? = null,
    val id: String,
    val importedAt: String? = null,
    val libraryId: Int? = null,
    val libraryName: String? = null,
    val libraryPath: String? = null,
    val maxOriginalYear: Int? = null,
    val maxYear: Int,
    val mbzAlbumArtistId: String? = null,
    val mbzAlbumId: String? = null,
    val mbzAlbumType: String? = null,
    val mbzReleaseGroupId: String? = null,
    val minOriginalYear: Int? = null,
    val minYear: Int? = null,
    val name: String,
    val orderAlbumArtistName: String? = null,
    val orderAlbumName: String? = null,
    val originalDate: String? = null,
    val participants: Map<String, List<NavidromeParticipant>>? = null,
    val playCount: Int? = null,
    val playDate: String? = null,
    val rating: Int? = null,
    val releaseDate: String? = null,
    val size: Long,
    val songCount: Int? = null,
    val songs: List<NavidromeSong>? = null,
    val sortAlbumArtistName: String? = null,
    val sortArtistName: String? = null,
    val starred: Boolean? = null,
    val starredAt: String? = null,
    val tags: Map<String, List<String>>? = null,
    val updatedAt: String? = null,
) {
    fun toMediaModel(providerId: String): MediaModel.Album {
        val durationMs = this.duration?.let { (it * 1000).toInt() } ?: 0

        val explicit = when (this.explicitStatus) {
            "e" -> true
            "c" -> false
            else -> null
        }

        val extractedOriginalYear = this.originalDate?.take(4)?.toIntOrNull()
            ?: this.minOriginalYear

        val extractedReleaseYear = this.releaseDate?.take(4)?.toIntOrNull()
            ?: extractedOriginalYear
            ?: if (this.maxYear > 0) this.maxYear else null

        return MediaModel.Album(
            id = this.id,
            providerId = providerId,
            providerType = ProviderType.NAVIDROME,
            albumArtistName = this.albumArtist,
            artists = this.participants?.get("albumartist")?.map { MediaModel.Artist(
                name = it.name,
                id = it.id,
                providerId = providerId,
                providerType = ProviderType.NAVIDROME) } ?: emptyList(),
            comment = this.comment,
            createdAt = this.createdAt,
            durationMs = durationMs,
            explicit = explicit,
            genres = this.genres?.map { MediaModel.Genre(name = it.name) } ?: emptyList(),
            imageId = this.id,
            imageUrl = this.id,
            isCompilation = this.compilation,
            lastPlayedAt = this.playDate,
            mbzId = this.mbzAlbumId,
            mbzReleaseGroupId = this.mbzReleaseGroupId,
            name = this.name,
            originalDate = this.originalDate,
            originalYear = extractedOriginalYear,
            participants = this.participants?.mapValues { roleParticipants ->
                roleParticipants.value.map { MediaModel.Artist(
                name = it.name,
                id = it.id,
                providerId = providerId,
                providerType = ProviderType.NAVIDROME) } } ?: emptyMap(),
            playCount = (this.playCount ?: 0).toDouble(),
            recordLabels = emptyList(),
            releaseDate = this.releaseDate,
            releaseType = this.mbzAlbumType,
            releaseTypes = this.mbzAlbumType?.let { listOf(it) } ?: emptyList(),
            releaseYear = extractedReleaseYear,
            size = this.size.toInt(),
            songCount = this.songCount,
            songs = this.songs?.map { it.toMediaModel(providerId) } ?: emptyList(),
            sortName = this.orderAlbumName,
            tags = this.tags ?: emptyMap(),
            updatedAt = this.updatedAt,
            userFavorite = this.starred,
            userRating = this.rating,
            version = null
        )
    }
}

@Serializable
data class NavidromeGenre(
    val id: String,
    val name: String
)
@Serializable
data class NavidromeParticipant(
    val id: String,
    val name: String,
    val subRole: String? = null
)

@Serializable
data class NavidromeSong(
    val id: String,
    val title: String,
    val album: String,
    val albumArtist: String? = null,
    val albumArtistId: String? = null,
    val albumId: String,
    val artist: String,
    val artistId: String? = null,
    val bitRate: Int? = null,
    val bookmarkPosition: Long? = null,
    val compilation: Boolean? = null,
    val createdAt: String? = null,
    val discNumber: Int,
    val duration: Double,
    val fullText: String? = null,
    val genre: String? = null,
    val genres: List<NavidromeGenre>? = null,
    val hasCoverArt: Boolean? = null,
    val orderAlbumArtistName: String? = null,
    val orderAlbumName: String? = null,
    val orderArtistName: String? = null,
    val orderTitle: String? = null,
    val path: String? = null,
    val sampleRate: Int? = null,
    val size: Long,
    val sortAlbumArtistName: String? = null,
    val sortArtistName: String? = null,
    val starred: Boolean? = null,
    val suffix: String? = null,
    val trackNumber: Int,
    val updatedAt: String? = null,
    val year: Int,

    val bitDepth: Int? = null,
    val bpm: Int? = null,
    val catalogNum: String? = null,
    val channels: Int? = null,
    val comment: String? = null,
    val discSubtitle: String? = null,
    val embedArtPath: String? = null,
    val explicitStatus: String? = null,
    val externalInfoUpdatedAt: String? = null,
    val externalUrl: String? = null,
    val imageFiles: String? = null,
    val largeImageUrl: String? = null,
    val libraryPath: String? = null,
    val lyrics: String? = null,
    val mbzAlbumArtistId: String? = null,
    val mbzAlbumId: String? = null,
    val mbzArtistId: String? = null,
    val mbzReleaseTrackId: String? = null,
    val mediumImageUrl: String? = null,
    val participants: Map<String, List<NavidromeParticipant>>? = null,
    val playCount: Int? = null,
    val playDate: String? = null,
    val rating: Int? = null,
    val releaseDate: String? = null,
    val rgAlbumGain: Double? = null,
    val rgAlbumPeak: Double? = null,
    val rgTrackGain: Double? = null,
    val rgTrackPeak: Double? = null,
    val smallImageUrl: String? = null,
    val starredAt: String? = null,
    val tags: Map<String, List<String>>? = null
) {
    fun toMediaModel(providerId: String): MediaModel.Song {
        val durationMs = (this.duration * 1000).toInt()
        val explicit = when (this.explicitStatus) {
            "e" -> true
            "c" -> false
            else -> null
        }

        val extractedReleaseYear = this.releaseDate?.take(4)?.toIntOrNull()
            ?: if (this.year > 0) this.year else null

        val extractedReleaseDate = this.releaseDate
            ?: if (this.year > 0) this.year.toString() else null

        val trackSubtitle = this.tags?.get("subtitle")?.joinToString(" · ")

        return MediaModel.Song(
            id = this.id,
            providerId = providerId,
            providerType = ProviderType.NAVIDROME,
            name = this.title,
            album = this.album,
            albumId = this.albumId,
            albumArtistName = this.albumArtist,
            artistName = this.artist,
            artists = this.participants?.get("albumartist")?.map { MediaModel.Artist(
                name = it.name,
                id = it.id,
                providerId = providerId,
                providerType = ProviderType.NAVIDROME) } ?: emptyList(),
            bitDepth = this.bitDepth,
            bitRate = this.bitRate,
            bpm = this.bpm,
            channels = this.channels,
            comment = this.comment,
            container = this.suffix,
            createdAt = this.createdAt,
            discNumber = this.discNumber,
            discSubtitle = this.discSubtitle,
            durationMs = durationMs,
            explicit = explicit,
            gain = if (this.rgAlbumGain != null || this.rgTrackGain != null) {
                GainInfo(album = this.rgAlbumGain, track = this.rgTrackGain)
            } else null,
            peak = if (this.rgAlbumPeak != null || this.rgTrackPeak != null) {
                GainInfo(album = this.rgAlbumPeak, track = this.rgTrackPeak)
            } else null,
            genres = this.genres?.map { MediaModel.Genre(name = it.name) } ?: emptyList(),
            imageId = this.id,
            imageUrl = this.id,
            lastPlayedAt = this.playDate,
            lyrics = this.lyrics,
            mbzRecordingId = this.mbzReleaseTrackId,
            mbzTrackId = this.mbzReleaseTrackId,
            path = this.path,
            participants = this.participants?.mapValues { roleParticipants ->
                roleParticipants.value.map { MediaModel.Artist(
                    name = it.name,
                    id = it.id,
                    providerId = providerId,
                    providerType = ProviderType.NAVIDROME) } } ?: emptyMap(),
            playCount = this.playCount,
            releaseDate = extractedReleaseDate,
            releaseYear = extractedReleaseYear,
            sampleRate = this.sampleRate,
            size = this.size.toInt(),
            sortName = this.orderTitle,
            tags = this.tags ?: emptyMap(),
            trackNumber = this.trackNumber,
            trackSubtitle = trackSubtitle,
            updatedAt = this.updatedAt,
            userFavorite = this.starred,
            userRating = this.rating
        )
    }
}

@Serializable
data class NavidromeArtist(
    val id: String,
    val name: String,
    val albumCount: Int? = null,
    val biography: String? = null,
    val externalInfoUpdatedAt: String? = null,
    val externalUrl: String? = null,
    val fullText: String? = null,
    val genres: List<NavidromeGenre>? = null,
    val orderArtistName: String? = null,
    val rating: Int? = null,
    val size: Long? = null,
    val songCount: Int? = null,
    val starred: Boolean? = null,
    val starredAt: String? = null,
    val createdAt: String? = null,
    val largeImageUrl: String? = null,
    val mbzArtistId: String? = null,
    val mediumImageUrl: String? = null,
    val playCount: Int? = null,
    val playDate: String? = null,
    val smallImageUrl: String? = null,
    val stats: Map<String, ArtistStats>? = null,
    val updatedAt: String? = null,
    val uploadedImage: String? = null
) {
    fun toMediaModel(providerId: String): MediaModel.Artist {
        val (albumCount, songCount) = if (this.stats != null) {
            val albumArtistStats = this.stats["albumartist"]
            val artistStats = this.stats["artist"]

            val calculatedAlbumCount = maxOf(
                albumArtistStats?.albumCount ?: 0,
                artistStats?.albumCount ?: 0
            )
            val calculatedSongCount = maxOf(
                albumArtistStats?.songCount ?: 0,
                artistStats?.songCount ?: 0
            )

            Pair(calculatedAlbumCount, calculatedSongCount)
        } else {
            Pair(this.albumCount, this.songCount)
        }

        return MediaModel.Artist(
            id = this.id,
            providerId = providerId,
            providerType = ProviderType.NAVIDROME,
            name = this.name,
            albumCount = albumCount,
            biography = this.biography,
            durationMs = null,
            genres = this.genres?.map { MediaModel.Genre(name = it.name) } ?: emptyList(),
            imageId = this.id,
            lastPlayedAt = this.playDate,
            playCount = (this.playCount ?: 0).toDouble(),
            songCount = songCount,
            uploadedImage = this.uploadedImage,
            userFavorite = this.starred,
            userRating = this.rating
        )
    }

}

@Serializable
data class ArtistStats(
    val albumCount: Int? = null,
    val size: Long? = null,
    val songCount: Int? = null
    // Add additional fields as needed for the artist stats object
)