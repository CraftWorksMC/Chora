package com.craftworks.music.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    object MainGraph : Screen
    @Serializable
    object Home : Screen
    @Serializable
    data class HomeLists(val category: String) : Screen
    @Serializable
    object Songs : Screen
    @Serializable
    object Radios : Screen
    @Serializable
    object NowPlayingLandscape : Screen
    @Serializable
    object Search : Screen
    @Serializable
    object Albums : Screen
    @Serializable
    object AlbumList : Screen
    @Serializable
    data class AlbumDetails(val albumId: String, val imageUri: String) : Screen
    @Serializable
    object Artists : Screen
    @Serializable
    object ArtistsList : Screen
    @Serializable
    data class ArtistDetails(val artistId: String, val imageUri: String?) : Screen
    @Serializable
    object Playlists : Screen
    @Serializable
    object PlaylistList : Screen
    @Serializable
    data class PlaylistDetails(val playlistId: String, val imageUri: String) : Screen
    @Serializable
    object Settings : Screen
    @Serializable
    object SettingsList : Screen
    @Serializable
    object S_Appearance : Screen
    @Serializable
    object S_Media_Providers : Screen
    @Serializable
    object S_Lyrics_Providers : Screen
    @Serializable
    object S_Playback : Screen
    @Serializable
    object S_Misc : Screen
}
