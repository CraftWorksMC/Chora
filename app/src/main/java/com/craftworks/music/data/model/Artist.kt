package com.craftworks.music.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Collections

/**
 * @deprecated Global mutable state is deprecated. Use ArtistRepository instead.
 * This is kept for backwards compatibility with legacy code.
 * TODO: Remove once all usages are migrated to repository pattern.
 */
@Deprecated("Use ArtistRepository instead of global mutable state")
val artistList: MutableList<MediaData.Artist> = Collections.synchronizedList(mutableListOf())

/**
 * @deprecated Global mutable state is deprecated. Use proper state management in ViewModel.
 * TODO: Refactor to use ViewModel-based state management.
 */
@Deprecated("Use ViewModel-based state management instead")
var selectedArtist by mutableStateOf(
    MediaData.Artist(
        name = "My Favourite Artist",
        artistImageUrl = "",
        navidromeID = "Local"
    )
)