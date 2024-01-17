package com.craftworks.music.data

sealed class Screen(val route: String) {
    object Home : Screen("home_screen")
    object Song : Screen("songs_screen")
    object Radio : Screen("radio_screen")

    //Albums
    object Albums : Screen("album_screen")
    object AlbumDetails : Screen("album_details")

    //Playlists
    object Playlists : Screen("playlist_screen")
    object PlaylistDetails : Screen("playlist_details")

    //Settings
    object Setting : Screen("setting_screen")
}
