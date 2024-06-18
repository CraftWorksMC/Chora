package com.craftworks.music.providers.navidrome

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    // ... other properties
)