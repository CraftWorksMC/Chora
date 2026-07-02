package com.craftworks.music.data.model

abstract class MediaQuery<T> (
    val sortBy: T,
    val sortOrder: SortOrder,
) {
    class AlbumListQuery(
        sortBy: AlbumListSort,
        sortOrder: SortOrder,

        val custom: Map<String, Any>? = null,
        val artistIds: List<String>? = null,
        val compilation: Boolean? = null,
        val favorite: Boolean? = null,
        val genreIds: List<String>? = null,
        val limit: Int? = null,
        val maxYear: Int? = null,
        val minYear: Int? = null,
        val musicFolderId: List<String>? = null,
        val searchTerm: String? = null,
        val startIndex: Int,
    ) : MediaQuery<AlbumListSort>(sortBy, sortOrder)
    class AlbumArtistListQuery(
        sortBy: AlbumArtistListSort,
        sortOrder: SortOrder,

        val custom: Map<String, Any>? = null,
        val favorite: Boolean? = null,
        val limit: Int? = null,
        val musicFolderId: List<String>? = null,
        val searchTerm: String? = null,
        val startIndex: Int
    ) : MediaQuery<AlbumArtistListSort>(sortBy, sortOrder)
    class ArtistListQuery(
        sortBy: ArtistListSort,
        sortOrder: SortOrder,

        val custom: Map<String, Any>? = null,
        val favorite: Boolean? = null,
        val limit: Int? = null,
        val musicFolderId: List<String>? = null,
        val role: String? = null,
        val searchTerm: String? = null,
        val startIndex: Int
    ) : MediaQuery<ArtistListSort>(sortBy, sortOrder)
    class FolderQuery(
        sortBy: SongListSort,
        sortOrder: SortOrder,

        val id: String,
        val musicFolderId: List<String>? = null,
        val searchTerm: String? = null,
    ) : MediaQuery<SongListSort>(sortBy, sortOrder)
    class GenreListQuery(
        sortBy: GenreListSort,
        sortOrder: SortOrder,

        val custom: Map<String, Any>? = null,
        val limit: Int? = null,
        val musicFolderId: List<String>? = null,
        val searchTerm: String? = null,
        val startIndex: Int
    ) : MediaQuery<GenreListSort>(sortBy, sortOrder)
    class PlaylistListQuery(
        sortBy: PlaylistListSort,
        sortOrder: SortOrder,

        val custom: Map<String, Any>? = null,
        val excludeSmartPlaylists: Boolean = false,
        val limit: Int? = null,
        val searchTerm: String? = null,
        val startIndex: Int
    ) : MediaQuery<PlaylistListSort>(sortBy, sortOrder)
    class SongListQuery(
        sortBy: SongListSort,
        sortOrder: SortOrder,

        val custom: Map<String, Any>? = null,
        val albumArtistIds: List<String>? = null,
        val albumIds: List<String>? = null,
        val artistIds: List<String>? = null,
        val favorite: Boolean? = null,
        val genreIds: List<String>? = null,
        val hasRating: Boolean = false,
        val imageSize: Int? = null,
        val limit: Int? = null,
        val maxYear: Int? = null,
        val minYear: Int? = null,
        val musicFolderId: List<String>? = null,
        val searchTerm: String? = null,
        val startIndex: Int
    ) : MediaQuery<SongListSort>(sortBy, sortOrder)

    class UserListQuery(
        sortBy: UserListSort,
        sortOrder: SortOrder,

        val custom: Map<String, Any>? = null,
        val albumArtistIds: List<String>? = null,
        val limit: Int? = null,
        val searchTerm: String? = null,
        val startIndex: Int
    ) : MediaQuery<UserListSort>(sortBy, sortOrder)

    data class SearchQuery(
        val albumArtistLimit: Int? = null,
        val albumArtistStartIndex: Int? = null,
        val albumLimit: Int? = null,
        val albumStartIndex: Int? = null,
        val musicFolderId: List<String>? = null,
        val query: String? = null,
        val songLimit: Int? = null,
        val songStartIndex: Int? = null
    )

    data class RandomSongListQuery(
        val genre: String? = null,
        val limit: Int? = null,
        val maxYear: Int? = null,
        val minYear: Int? = null,
        val musicFolderId: Any? = null, // String or List<String>
        val played: Played
    )




    enum class Played {
        All,
        Never,
        Played
    }
}