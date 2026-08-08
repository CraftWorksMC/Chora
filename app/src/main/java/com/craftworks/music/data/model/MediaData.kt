package com.craftworks.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MusicFolder (
    val id: String,
    val name: String,
)

@Serializable
data class GainInfo(
    val album: Double? = null,
    val track: Double? = null
)

data class PlaylistRules(
    val limit: Int? = null,
    val limitPercent: Int? = null,
    val sort: String? = null
)

enum class LibraryType {
    ALBUM,
    ALBUM_ARTIST,
    ARTIST,
    FOLDER,
    GENRE,
    PLAYLIST,
    PLAYLIST_SONG,
    QUEUE_SONG,
    RADIO_STATION,
    SONG,
}
data class AlbumArtistInfo(
    val biography: String? = null,
    val imageUrl: String? = null,
    val similarArtists: List<MediaModel.Artist>? = null
)
data class AlbumInfo(
    val imageUrl: String?,
    val notes: String?
)

data class Tag(
    val name: String,
    val options: List<Option>
) {
    data class Option(
        val id: String,
        val name: String
    )
}
enum class ScrobbleEvent {
    PAUSE,
    UNPAUSE,
    START,
    STOP
}
enum class ScrobbleMediaType {
    SONG,
    PODCAST
}