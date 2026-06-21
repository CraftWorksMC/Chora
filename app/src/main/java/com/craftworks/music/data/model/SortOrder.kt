package com.craftworks.music.data.model

enum class SortOrder {
    ASC,
    DESC
}
enum class AlbumListSort {
    ALBUM_ARTIST,
    ARTIST,
    COMMUNITY_RATING,
    CRITIC_RATING,
    DURATION,
    EXPLICIT_STATUS,
    FAVORITED,
    ID,
    NAME,
    PLAY_COUNT,
    RANDOM,
    RATING,
    RECENTLY_ADDED,
    RECENTLY_PLAYED,
    RELEASE_DATE,
    SONG_COUNT,
    SORT_NAME,
    YEAR
}
enum class AlbumArtistListSort {
    ALBUM,
    ALBUM_COUNT,
    DURATION,
    FAVORITED,
    NAME,
    PLAY_COUNT,
    RANDOM,
    RATING,
    RECENTLY_ADDED,
    RELEASE_DATE,
    SONG_COUNT
}
enum class ArtistListSort {
    ALBUM,
    ALBUM_COUNT,
    DURATION,
    FAVORITED,
    NAME,
    PLAY_COUNT,
    RANDOM,
    RATING,
    RECENTLY_ADDED,
    RELEASE_DATE,
    SONG_COUNT
}
enum class SongListSort {
    ALBUM,
    ALBUM_ARTIST,
    ARTIST,
    BPM,
    CHANNELS,
    COMMENT,
    DURATION,
    EXPLICIT_STATUS,
    FAVORITED,
    GENRE,
    ID,
    NAME,
    PLAY_COUNT,
    RANDOM,
    RATING,
    RECENTLY_ADDED,
    RECENTLY_PLAYED,
    RELEASE_DATE,
    SORT_NAME,
    YEAR
}
enum class GenreListSort {
    NAME,
}
enum class PlaylistListSort {
    DURATION,
    NAME,
    OWNER,
    PUBLIC,
    SONG_COUNT,
    UPDATED_AT
}
enum class UserListSort {
    NAME,
}