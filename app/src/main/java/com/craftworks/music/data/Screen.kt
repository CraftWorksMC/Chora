package com.craftworks.music.data

sealed class Screen(val route: String) {
    object Home : Screen("home_screen")
    object Song : Screen("songs_screen")
    object Radio : Screen("radio_screen")

    //Albums
    object Albums : Screen("album_screen")
    object AlbumDetails : Screen("album_details")

    //Artists
    //object Artists : Screen("artist_screen")
    object AristDetails : Screen("artist_details")

    //Playlists
    object Playlists : Screen("playlist_screen")
    object PlaylistDetails : Screen("playlist_details")

    //Settings
    object Setting : Screen("setting_screen")
    object S_Appearance : Screen("s_appearance_screen")
    object S_Providers : Screen("s_providers_screen")
}
