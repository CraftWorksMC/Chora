package com.craftworks.music.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey
    val key: String,
    val lastSyncTimestamp: Long,
    val itemCount: Int = 0
) {
    companion object {
        const val KEY_SONGS = "songs_sync"
        const val KEY_ALBUMS = "albums_sync"
        const val KEY_ARTISTS = "artists_sync"
        const val KEY_LAST_FULL_SYNC = "last_full_sync"
    }
}
