package com.craftworks.music.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.craftworks.music.data.database.entity.SyncMetadata

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE `key` = :key")
    suspend fun getSyncMetadata(key: String): SyncMetadata?

    @Query("SELECT lastSyncTimestamp FROM sync_metadata WHERE `key` = :key")
    suspend fun getLastSyncTime(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadata)

    @Query("DELETE FROM sync_metadata WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAll()
}
