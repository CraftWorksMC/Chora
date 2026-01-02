package com.craftworks.music.data.model

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding_screen")
    data object Home : Screen("home_screen")
    data object HomeLists : Screen("home_lists")

    data object Song : Screen("songs_screen")
    data object Radio : Screen("radio_screen")

    data object NowPlayingLandscape : Screen("playing_tv_screen")
    data object Queue : Screen("queue_screen")

    //Albums
    data object Albums : Screen("album_screen")
    data object AlbumDetails : Screen("album_details")

    //Artists
    data object Artists : Screen("artists_screen")
    data object ArtistDetails : Screen("artist_details")

    //Playlists
    data object Playlists : Screen("playlist_screen")
    data object PlaylistDetails : Screen("playlist_details")

    //Settings
    data object Setting : Screen("setting_screen")
    data object S_Appearance : Screen("s_appearance_screen")
    data object S_Artwork : Screen("s_artwork_screen")
    data object S_Providers : Screen("s_providers_screen")
    data object S_Playback : Screen("s_playback_screen")
    data object S_Data : Screen("s_data_screen")
}
