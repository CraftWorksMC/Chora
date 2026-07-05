package com.craftworks.music.data.model

import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import com.craftworks.music.R
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.providers.MediaProvider
import kotlinx.serialization.Serializable

abstract class MediaModel()
{
    lateinit var providerId: String
    lateinit var providerType: ProviderType
    lateinit var id: String

    class Album(
        val albumArtistName: String? = "Unknown Artist",
        val artists: List<Artist> = listOf(),
        val comment: String? = null,
        val createdAt: String? = null,
        val durationMs: Int = 0,
        val explicitStatus: ExplicitStatus? = null,
        val genres: List<Genre> = listOf(),
        val imageId: String? = null,
        val imageUrl: String? = null,
        val isCompilation: Boolean? = null,
        val lastPlayedAt: String? = null,
        val mbzId: String? = null,
        val mbzReleaseGroupId: String? = null,
        val name: String,
        val originalDate: String? = null,
        val originalYear: Int? = null,
        val participants: Map<String, List<RelatedArtist>> = mapOf(),
        val playCount: Double? = null,
        val recordLabels: List<String> = listOf(),
        val releaseDate: String? = null,
        val releaseType: String? = null,
        val releaseTypes: List<String> = listOf(),
        val releaseYear: Int? = null,
        val size: Int? = null,
        val songCount: Int? = null,
        val songs: List<Song> = listOf(),
        val sortName: String? = null,
        val tags: Map<String, List<String>> = mapOf(),
        val updatedAt: String? = null,
        val userFavorite: Boolean? = null,
        val userRating: Int? = null,
        val version: String? = null
    ) : MediaModel() {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            return toMediaItem(MediaProviderManager.getProvider(providerId))
        }
        fun toMediaItem(provider: MediaProvider?): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setArtist(this.albumArtistName)
                    .setAlbumTitle(this.name)
                    .setDisplayTitle(this.name)
                    .setAlbumArtist(this.albumArtistName)
                    .setArtworkUri(this.imageUrl?.let { provider?.getImageUrl(it)?.toUri() })
                    .setRecordingYear(this.releaseYear)
                    .setDurationMs(this.durationMs.toLong())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setGenre(this.genres.joinToString { it.name })
                    .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                    .setExtras(
                        Bundle().apply {
                            putString("id", this@Album.id)
                            putString("providerId", this@Album.providerId)
                            putBoolean("userFavorite", this@Album.userFavorite == true)
                        }
                    )
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(this.id)
                .setMediaMetadata(mediaMetadata)
                .build()
        }
    }

    @Serializable
    class Artist(
        val albumCount: Int?,
        val biography: String?,
        val durationMs: Int?,
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
    ) : MediaModel() {
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

    class Folder(
        val children: Children? = null,

        val imageId: String? = null,
        val imageUrl: String? = null,

        val name: String,
        val parentId: String? = null
    ) : MediaModel() {
        data class Children(
            val folders: List<Folder>,
            val songs: List<Song>
        )
    }

    @Serializable
    class Genre(
        val albumCount: Int? = null,
        val imageId: String? = null,
        val imageUrl: String? = null,
        val name: String,
        val songCount: Int? = null
    ) : MediaModel()

    class InternetRadioStation(
        val homepageUrl: String?,
        val imageId: String? = null,
        val imageUrl: String? = null,
        val name: String,
        val streamUrl: String,
        val uploadedImage: String? = null
    ) : MediaModel() {
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
        val description: String?,
        val durationMs: Int?,
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
    ) : MediaModel() {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            return toMediaItem(MediaProviderManager.getProvider(providerId))
        }
        fun toMediaItem(provider: MediaProvider?): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setDescription(this.description)
                    .setArtworkUri(this.imageUrl?.let { provider?.getImageUrl(it)?.toUri() })
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .setDurationMs(this.durationMs?.toLong())
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
        val durationMs: Int,
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
    ) : MediaModel()

    @Serializable
    class Song(
        val album: String? = null,
        val albumArtistName: String,
        val albumArtists: List<Artist> = listOf(),
        val albumId: String,
        val artistName: String,
        val artists: List<Artist> = listOf(),
        val bitDepth: Int? = null,
        val bitRate: Int? = null,
        val bpm: Int? = null,
        val channels: Int? = null,
        val comment: String? = null,
        val compilation: Boolean? = null,
        val container: String? = null,
        val createdAt: String? = null,
        val discNumber: Int,
        val discSubtitle: String? = null,
        val durationMs: Int,
        val explicitStatus: ExplicitStatus? = null,
        val gain: GainInfo? = null,
        val genres: List<Genre> = listOf(),
        val imageId: String? = null,
        val imageUrl: String? = null,
        val lastPlayedAt: String? = null,
        val lyrics: String? = null,
        val mbzRecordingId: String? = null,
        val mbzTrackId: String? = null,
        val name: String,
        val participants: Map<String, List<RelatedArtist>>? = null,
        val path: String? = null,
        val peak: GainInfo? = null,
        val playCount: Int? = null,
        val playlistItemId: String? = null,
        val releaseDate: String? = null,
        val releaseYear: Int? = null,
        val sampleRate: Int? = null,
        val size: Int? = null,
        val sortName: String? = null,
        val tags: Map<String, List<String>>? = null,
        val trackNumber: Int,
        val trackSubtitle: String? = null,
        val updatedAt: String? = null,
        val userFavorite: Boolean,
        val userRating: Int? = null
    ) : MediaModel() {
        fun toMediaItem(): androidx.media3.common.MediaItem {
            return toMediaItem(MediaProviderManager.getProvider(providerId))
        }
        fun toMediaItem(provider: MediaProvider?): androidx.media3.common.MediaItem {
            val mediaMetadata =
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setArtist(this.artistName)
                    .setAlbumTitle(this.album)
                    .setArtworkUri(this.imageId?.let { provider?.getImageUrl(it)?.toUri() })
                    .setRecordingYear(this.releaseYear)
                    .setDiscNumber(this.discNumber)
                    .setTrackNumber(this.trackNumber)
                    .setIsBrowsable(false).setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setDurationMs(this.durationMs.toLong())
                    .setGenre(this.genres.joinToString { it.name })
                    .setExtras(
                        Bundle().apply {
                            putString("id", this@Song.id)
                            putString("providerId", this@Song.providerId)
                            putString("albumId", this@Song.albumId)
                            putBoolean("userFavorite", this@Song.userFavorite)
                        }
                    )
                    .build()

            return androidx.media3.common.MediaItem.Builder()
                .setMediaId(provider?.getStreamUrl(this.id, false).toString())
                .setUri(provider?.getStreamUrl(this.id, false)?.toUri())
                .setMediaMetadata(mediaMetadata)
                .build()
        }
    }
}

val MediaMetadata.id: String?
    get() = extras?.getString("id")