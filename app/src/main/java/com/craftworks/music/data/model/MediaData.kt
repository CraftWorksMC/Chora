package com.craftworks.music.data.model

data class RelatedArtist(
    val id: String,
    val imageId: String?,
    val imageUrl: String?,
    val name: String,
    val userFavorite: Boolean,
    val userRating: Number?
)

data class GainInfo(
    val album: Double? = null,
    val track: Double? = null
)

enum class ExplicitStatus {
    CLEAN,
    EXPLICIT,
}

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
    val similarArtists: List<RelatedArtist>? = null
)
data class AlbumInfo(
    val imageUrl: String?,
    val notes: String?
)
data class InternetRadioStation(
    val homepageUrl: String?,
    val id: String,
    val imageId: String? = null,
    val imageUrl: String? = null,
    val name: String,
    val streamUrl: String,
    val uploadedImage: String? = null
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