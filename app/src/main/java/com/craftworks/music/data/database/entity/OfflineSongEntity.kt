package com.craftworks.music.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "offline_songs",
    indices = [
        Index(value = ["songId"], unique = true),
        Index(value = ["downloadedAt"]),
        Index(value = ["isAvailable"])
    ]
)
data class OfflineSongEntity(
    @PrimaryKey
    val id: String,
    val songId: String,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true
)
