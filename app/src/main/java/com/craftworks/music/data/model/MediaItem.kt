package com.craftworks.music.data.model

import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata

abstract class MediaItem (
    val providerId: String,
    val providerType: ProviderType,
    val id: String,
) {

    class Album(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val albumArtistName: String,
        val albumArtists: List<RelatedArtist>,
        val artists: List<RelatedArtist>,
        val comment: String?,
        val createdAt: String,
        val duration: Int?,
        val explicitStatus: ExplicitStatus?,
        val genres: List<Genre>,
        val imageId: String?,
        val imageUrl: String?,
        val isCompilation: Boolean?,
        val lastPlayedAt: String?,
        val mbzId: String?,
        val mbzReleaseGroupId: String?,
        val name: String,
        val originalDate: String?, // was null | PartialIsoDateString
        val originalYear: Int,
        val participants: Map<String, List<RelatedArtist>>?,
        val playCount: Double?,
        val recordLabels: List<String>,
        val releaseDate: String?,
        val releaseType: String?,
        val releaseTypes: List<String>,
        val releaseYear: Int?,
        val size: Int?,
        val songCount: Int?,
        val songs: List<Song>?, // optional in TS
        val sortName: String,
        val tags: Map<String, List<String>>?,
        val updatedAt: String,
        val userFavorite: Boolean,
        val userRating: Int?,
        val version: String?
    ) : MediaItem(providerId, providerType, id)

    class AlbumArtist(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val albumCount: Int?,
        val biography: String?,
        val duration: Int?,
        val genres: List<Genre>,
        val imageId: String?,
        val imageUrl: String?,
        val lastPlayedAt: String?,
        val mbz: String?,
        val name: String,
        val playCount: Double?,
        val similarArtists: List<RelatedArtist>?,
        val songCount: Int?,
        val uploadedImage: String?,
        val userFavorite: Boolean,
        val userRating: Int?
    ) : MediaItem(providerId, providerType, id)

    class Artist(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val albumCount: Int?,
        val biography: String?,
        val duration: Int?,
        val genres: List<Genre>,
        val imageId: String?,
        val imageUrl: String?,
        val lastPlayedAt: String?,
        val mbz: String?,
        val name: String,
        val playCount: Double?,
        val similarArtists: List<RelatedArtist>?,
        val songCount: Int?,
        val uploadedImage: String?,
        val userFavorite: Boolean,
        val userRating: Int?
    ) : MediaItem(providerId, providerType, id)
    class Folder(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val children: Children? = null,

        val imageId: String? = null,
        val imageUrl: String? = null,

        val name: String,
        val parentId: String? = null
    ) : MediaItem(providerId, providerType, id) {
        data class Children(
            val folders: List<Folder>,
            val songs: List<Song>
        )
    }

    class Genre(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val albumCount: Int?,
        val imageId: String?,
        val imageUrl: String?,
        val name: String,
        val songCount: Int?
    ) : MediaItem(providerId, providerType, id)

    class Playlist(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val description: String?,
        val duration: Int?,
        val genres: List<Genre>,
        val imageId: String?,
        val imageUrl: String?,
        val name: String,
        val owner: String?,
        val ownerId: String?,
        val isPublic: Boolean?,
        val rules: PlaylistRules?,
        val size: Int?,
        val songCount: Int?,
        val sync: Boolean?,
        val uploadedImage: String?
    ) : MediaItem(providerId, providerType, id)

    class QueueSong(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val album: String?,
        val albumArtistName: String,
        val albumArtists: List<RelatedArtist>,
        val albumId: String,
        val artistName: String,
        val artists: List<RelatedArtist>,
        val bitDepth: Int?,
        val bitRate: Int,
        val bpm: Int?,
        val channels: Int?,
        val comment: String?,
        val compilation: Boolean?,
        val container: String?,
        val createdAt: String,
        val discNumber: Int,
        val discSubtitle: String?,
        val duration: Int,
        val explicitStatus: ExplicitStatus?,
        val gain: GainInfo?,
        val genres: List<Genre>,
        val imageId: String?,
        val imageUrl: String?,
        val lastPlayedAt: String?,
        val lyrics: String?,
        val mbzRecordingId: String?,
        val mbzTrackId: String?,
        val name: String,
        val participants: Map<String, List<RelatedArtist>>?,
        val path: String?,
        val peak: GainInfo?,
        val playCount: Int,
        val playlistItemId: String?,
        val releaseDate: String?,
        val releaseYear: Int?,
        val sampleRate: Int?,
        val size: Int,
        val sortName: String,
        val tags: Map<String, List<String>>?,
        val trackNumber: Int,
        val trackSubtitle: String?,
        val updatedAt: String,
        val userFavorite: Boolean,
        val userRating: Int?,
        val uniqueId: String
    ) : MediaItem(providerId, providerType, id)

    class Song(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val album: String?,
        val albumArtistName: String,
        val albumArtists: List<RelatedArtist>,
        val albumId: String,
        val artistName: String,
        val artists: List<RelatedArtist>,
        val bitDepth: Int?,
        val bitRate: Int,
        val bpm: Int?,
        val channels: Int?,
        val comment: String?,
        val compilation: Boolean?,
        val container: String?,
        val createdAt: String,
        val discNumber: Int,
        val discSubtitle: String?,
        val duration: Int,
        val explicitStatus: ExplicitStatus?,
        val gain: GainInfo?,
        val genres: List<Genre>,
        val imageId: String?,
        val imageUrl: String?,
        val lastPlayedAt: String?,
        val lyrics: String?,
        val mbzRecordingId: String?,
        val mbzTrackId: String?,
        val name: String,
        val participants: Map<String, List<RelatedArtist>>?,
        val path: String?,
        val peak: GainInfo?,
        val playCount: Int,
        val playlistItemId: String?,
        val releaseDate: String?,
        val releaseYear: Int?,
        val sampleRate: Int?,
        val size: Int,
        val sortName: String,
        val tags: Map<String, List<String>>?,
        val trackNumber: Int,
        val trackSubtitle: String?,
        val updatedAt: String,
        val userFavorite: Boolean,
        val userRating: Int?
    ) : MediaItem(providerId, providerType, id) {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setArtist(this.artistName)
                    .setAlbumTitle(this.album)
                    .setArtworkUri(this.imageUrl?.toUri()) // TODO("Call provider's getImageUrl")
                    .setRecordingYear(this.releaseYear)
                    .setDiscNumber(this.discNumber)
                    .setTrackNumber(this.trackNumber)
                    .setIsBrowsable(false).setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setDurationMs(this.duration.toLong() * 1000)
                    .setGenre(this.genres.joinToString())
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(this.id)
                .setUri(this.id.toUri()) // TODO("Call provider's getStreamUrl")
                .setMediaMetadata(mediaMetadata)
                .build()
        }
    }
}