package com.craftworks.music.data

import com.craftworks.music.R

sealed class BottomNavItem(
    var title: String,
    var icon: Int
) {
    object Home :
        BottomNavItem(
            "Home",
            R.drawable.rounded_home_24
        )

    object Albums :
        BottomNavItem(
            "Albums",
            R.drawable.rounded_library_music_24
        )

    object Songs :
        BottomNavItem(
            "Songs",
            R.drawable.round_music_note_24
        )

    object Radios :
        BottomNavItem(
            "Radios",
            R.drawable.rounded_radio
        )
    object Playlists :
        BottomNavItem(
            "Playlists",
            R.drawable.placeholder
        )
}