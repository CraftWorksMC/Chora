package com.craftworks.music.data.model

data class AuthenticationResponse(
    val credential: String,
    val isAdmin: Boolean? = null,
    val ndCredential: String? = null,
    val userId: String? = null,
    val username: String
)
data class ImageRequest(
    val cacheKey: String,
    val credentials: String? = null,
    val headers: Map<String, String>? = null,
    val url: String
)
sealed interface LyricsResponse {
    data class Plain(val text: String) : LyricsResponse
    data class Synchronized(val lines: List<Pair<Double, String>>) : LyricsResponse
}
data class GetQueueResponse(
    val changed: String,
    val changedBy: String,
    val currentIndex: Int,
    val entry: List<MediaModel.Song>,
    val positionMs: Long,
    val username: String
)
data class ProviderInfo(
    val features: ProviderFeatures,
    val id: String? = null,
    val version: String
)
data class TagListResponse(
    val excluded: Excluded,
    val tags: List<Tag>? = null
) {
    data class Excluded(
        val album: List<String>,
        val song: List<String>
    )
}
data class UserInfoResponse(
    val id: String,
    val isAdmin: Boolean,
    val name: String
)
data class User(
    val createdAt: String?,
    val email: String?,
    val id: String,
    val isAdmin: Boolean?,
    val lastLoginAt: String?,
    val name: String,
    val updatedAt: String?
)
data class SearchResponse(
    val albumArtists: List<MediaModel.AlbumArtist>,
    val albums: List<MediaModel.Album>,
    val songs: List<MediaModel.Song>
)
