package com.craftworks.music.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.craftworks.music.data.database.entity.OfflineSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineSongDao {
    @Query("SELECT * FROM offline_songs ORDER BY downloadedAt DESC")
    fun getAllOfflineSongs(): Flow<List<OfflineSongEntity>>

    @Query("SELECT * FROM offline_songs WHERE isAvailable = 1 ORDER BY downloadedAt DESC")
    fun getAvailableOfflineSongs(): Flow<List<OfflineSongEntity>>

    @Query("SELECT * FROM offline_songs WHERE songId = :songId LIMIT 1")
    suspend fun getOfflineSong(songId: String): OfflineSongEntity?

    @Query("SELECT * FROM offline_songs WHERE songId = :songId AND isAvailable = 1 LIMIT 1")
    suspend fun getAvailableOfflineSong(songId: String): OfflineSongEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM offline_songs WHERE songId = :songId AND isAvailable = 1)")
    suspend fun isOfflineAvailable(songId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM offline_songs WHERE songId = :songId AND isAvailable = 1)")
    fun isOfflineAvailableFlow(songId: String): Flow<Boolean>

    @Query("SELECT songId FROM offline_songs WHERE isAvailable = 1")
    suspend fun getAllOfflineSongIds(): List<String>

    @Query("SELECT songId FROM offline_songs WHERE songId IN (:songIds) AND isAvailable = 1")
    suspend fun getOfflineSongIdsFromList(songIds: List<String>): List<String>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM offline_songs WHERE isAvailable = 1")
    suspend fun getTotalOfflineSize(): Long

    @Query("SELECT COUNT(*) FROM offline_songs WHERE isAvailable = 1")
    fun getOfflineSongCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(offlineSong: OfflineSongEntity)

    @Update
    suspend fun update(offlineSong: OfflineSongEntity)

    @Query("UPDATE offline_songs SET lastAccessedAt = :accessedAt WHERE songId = :songId")
    suspend fun updateLastAccessed(songId: String, accessedAt: Long)

    @Query("UPDATE offline_songs SET isAvailable = :isAvailable WHERE songId = :songId")
    suspend fun updateAvailability(songId: String, isAvailable: Boolean)

    @Query("UPDATE offline_songs SET isAvailable = 0 WHERE songId = :songId")
    suspend fun markUnavailable(songId: String)

    @Query("DELETE FROM offline_songs WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)

    @Query("DELETE FROM offline_songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM offline_songs")
    suspend fun deleteAll()

    @Query("DELETE FROM offline_songs WHERE isAvailable = 0")
    suspend fun deleteUnavailable()

    // For cleanup - find songs where file might be missing
    @Query("SELECT * FROM offline_songs WHERE isAvailable = 1")
    suspend fun getAllAvailableOnce(): List<OfflineSongEntity>
}
