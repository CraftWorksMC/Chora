package com.craftworks.music.data

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.Serializable

var songsList: MutableList<MediaData.Song> = mutableStateListOf()

@Immutable
@Serializable
data class Genre(
    val name: String? = "")

@Immutable
@Serializable
data class ReplayGain(
    val trackGain: Float? = 0f,
    //val trackPeak: Float? = 0f,
    //val albumPeak: Float? = 0f
)

fun MediaData.Song.toMediaItem(): MediaItem {
    val mediaMetadata =
        MediaMetadata.Builder()
            .setTitle(this@toMediaItem.title)
            .setArtist(this@toMediaItem.artist)
            .setAlbumTitle(this@toMediaItem.album)
            .setArtworkUri(Uri.parse(this@toMediaItem.imageUrl))
            .setReleaseYear(this@toMediaItem.year)
            .setIsBrowsable(false).setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(Bundle().apply {
                putString("navidromeID", this@toMediaItem.navidromeID)
                putInt("duration", this@toMediaItem.duration)
                putString("format", this@toMediaItem.format)
                this@toMediaItem.bitrate?.let { putInt("bitrate", it) }
                putBoolean("isRadio", this@toMediaItem.isRadio == true)
                if (this@toMediaItem.replayGain?.trackGain != null)
                    putFloat("replayGain", this@toMediaItem.replayGain.trackGain)
            }).build()

    val requestMetadata = RequestMetadata.Builder().setMediaUri(Uri.parse(this@toMediaItem.media)).build()

    return MediaItem.Builder()
        .setMediaId(this@toMediaItem.media.toString())
        .setUri(Uri.parse(this@toMediaItem.media))
        .setMediaMetadata(mediaMetadata)
        .setRequestMetadata(requestMetadata)
        .build()
}

fun MediaItem.toSong(): MediaData.Song {
    val mediaMetadata = this@toSong.mediaMetadata
    val extras = mediaMetadata.extras

    return MediaData.Song(
        navidromeID = extras?.getString("navidromeID") ?: "",
        title = mediaMetadata.title.toString(),
        artist = mediaMetadata.artist.toString(),
        album = mediaMetadata.albumTitle.toString(),
        imageUrl = mediaMetadata.artworkUri.toString(),
        year = mediaMetadata.releaseYear ?: 0,
        duration = extras?.getInt("duration") ?: 0,
        format = extras?.getString("format") ?: "",
        bitrate = extras?.getInt("bitrate"),
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