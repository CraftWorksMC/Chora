package com.craftworks.music.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_palettes")
data class AlbumPaletteEntity(
    @PrimaryKey
    val imageUrl: String,
    val colors: String // Comma separated integers
)
