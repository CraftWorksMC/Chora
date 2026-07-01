package com.craftworks.music.data.model

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata

abstract class MediaModel (
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
        val originalDate: String?,
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
        val songs: List<Song>?,
        val sortName: String,
        val tags: Map<String, List<String>>?,
        val updatedAt: String,
        val userFavorite: Boolean,
        val userRating: Int?,
        val version: String?
    ) : MediaModel(providerId, providerType, id) {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setArtist(this.albumArtistName)
                    .setAlbumTitle(this.name)
                    .setDisplayTitle(this.name)
                    .setAlbumArtist(this.albumArtistName)
                    .setArtworkUri(this.imageUrl?.toUri()) // TODO("Call provider's getImageUrl")
                    .setRecordingYear(this.releaseYear)
                    .setDurationMs(this.duration?.times(1000)?.toLong())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setGenre(this.genres.joinToString { it.name })
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .setExtras(
                        Bundle().apply {
                            putString("id", this@Album.id)
                            putBoolean("userFavorite", this@Album.userFavorite)
                        }
                    )
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(this.id)
                .setMediaMetadata(mediaMetadata)
                .build()
        }
    }

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
    ) : MediaModel(providerId, providerType, id) {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
                    .setArtworkUri(this.imageUrl?.toUri()) // TODO("Call provider's getImageUrl")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(this.id)
                .setUri(this.id)
                .build()
        }
    }

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
    ) : MediaModel(providerId, providerType, id)
    class Folder(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val children: Children? = null,

        val imageId: String? = null,
        val imageUrl: String? = null,

        val name: String,
        val parentId: String? = null
    ) : MediaModel(providerId, providerType, id) {
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
    ) : MediaModel(providerId, providerType, id)

    class InternetRadioStation(
        providerId: String,
        providerType: ProviderType,
        id: String,

        val homepageUrl: String?,
        val imageId: String? = null,
        val imageUrl: String? = null,
        val name: String,
        val streamUrl: String,
        val uploadedImage: String? = null
    ) : MediaModel(providerId, providerType, id) {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setStation(this.name)
                    .setArtist(this.name)
                    .setArtworkUri(
                        ("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder).toUri()
                    )
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .setExtras(
                        Bundle().apply {
                            putString("providerId", this@InternetRadioStation.providerId)
                            putString("id", this@InternetRadioStation.id)
                            putString("homepage", this@InternetRadioStation.homepageUrl ?: "")
                        }
                    )
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(this.id)
                .setMediaMetadata(mediaMetadata)
                .build()
        }
    }

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
    ) : MediaModel(providerId, providerType, id) {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setDescription(this.description)
                    .setArtworkUri(this.imageUrl?.toUri()) // TODO("Call provider's getImageUrl")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .setDurationMs(this.duration?.times(1000)?.toLong())
                    .setExtras(
                        Bundle().apply {
                            putString("id", this@Playlist.id)
                        }
                    )
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(this.id)
                .setMediaMetadata(mediaMetadata)
                .build()
        }
    }

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
    ) : MediaModel(providerId, providerType, id)

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
    ) : MediaModel(providerId, providerType, id) {
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
                    .setGenre(this.genres.joinToString { it.name })
                    .setExtras(
                        Bundle().apply {
                            putString("id", this@Song.id)
                            putBoolean("userFavorite", this@Song.userFavorite)
                        }
                    )
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(this.id)
                .setUri(this.id.toUri()) // TODO("Call provider's getStreamUrl")
                .setMediaMetadata(mediaMetadata)
                .build()
        }
    }
}