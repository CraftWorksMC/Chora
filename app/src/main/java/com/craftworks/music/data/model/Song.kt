package com.craftworks.music.data.model

import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.Serializable
import java.util.UUID

var songsList: MutableList<MediaData.Song> = mutableStateListOf()

@Immutable
@Serializable
data class Genre(
    val name: String? = ""
)

@Immutable
@Serializable
data class ReplayGain(
    val trackGain: Float? = 0f,
    //val trackPeak: Float? = 0f,
    //val albumPeak: Float? = 0f
)

@Immutable
@Serializable
data class Artists(
    val id: String? = "",
    val name: String? = ""
)

fun MediaData.Song.toMediaItem(): MediaItem {
    val mediaMetadata =
        MediaMetadata.Builder()
            .setTitle(this@toMediaItem.title)
            .setArtist(this@toMediaItem.artist)
            .setAlbumTitle(this@toMediaItem.album)
            .setArtworkUri(this@toMediaItem.imageUrl.toUri())
            .setRecordingYear(this@toMediaItem.year)
            .setDiscNumber(this@toMediaItem.discNumber)
            .setTrackNumber(this@toMediaItem.track)
            .setIsBrowsable(false).setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setDurationMs(this@toMediaItem.duration.times(1000).toLong())
            .setGenre(this@toMediaItem.genres?.joinToString() { it.name ?: "" })
            .setExtras(Bundle().apply {
                putString("navidromeID", this@toMediaItem.navidromeID)
                putString("lyricsArtist", this@toMediaItem.artists[0].name)
                putInt("duration", this@toMediaItem.duration)
                putString("format", this@toMediaItem.format)
                putLong("bitrate", this@toMediaItem.bitrate?.toLong() ?: 0)
                putBoolean("isRadio", this@toMediaItem.isRadio == true)
                if (this@toMediaItem.replayGain?.trackGain != null)
                    putFloat("replayGain", this@toMediaItem.replayGain.trackGain)
            }).build()

    return MediaItem.Builder()
        .setMediaId(this@toMediaItem.media.toString())
        .setUri(this@toMediaItem.media?.toUri())
        .setMediaMetadata(mediaMetadata)
        .build()
}

fun MediaItem.toSong(): MediaData.Song {
    val mediaMetadata = this@toSong.mediaMetadata
    val extras = mediaMetadata.extras

    return MediaData.Song(
        navidromeID = extras?.getString("navidromeID") ?: "",
        title = mediaMetadata.title.toString(),
        artist = mediaMetadata.artist.toString(),
        artists = listOf(Artists(UUID.randomUUID().toString(), mediaMetadata.artist.toString())),
        album = mediaMetadata.albumTitle.toString(),
        imageUrl = mediaMetadata.artworkUri.toString(),
        year = mediaMetadata.recordingYear ?: 0,
        duration = mediaMetadata.durationMs?.toInt()?.div(1000) ?: 0,
        format = extras?.getString("format") ?: "",
        bitrate = extras?.getLong("bitrate")?.toInt(),
        media = this@toSong.mediaId.toString(),
        replayGain = ReplayGain(
            trackGain = extras?.getFloat("replayGain") ?: 0f
        ),
        isRadio = extras?.getBoolean("isRadio"),
        path = "",
        parent = "",
        dateAdded = "",
        bpm = 0,
        albumId = ""
    )
}