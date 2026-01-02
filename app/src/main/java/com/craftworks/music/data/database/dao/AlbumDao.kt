package com.craftworks.music.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.craftworks.music.data.database.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY name COLLATE NOCASE ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllAlbumsOnce(): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY year DESC")
    fun getAlbumsByArtist(artistId: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE navidromeID = :id")
    suspend fun getAlbumById(id: String): AlbumEntity?

    @Query("SELECT * FROM albums ORDER BY created DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 50): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE starred IS NOT NULL AND starred != '' ORDER BY name COLLATE NOCASE ASC")
    fun getStarredAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY RANDOM() LIMIT :limit")
    fun getRandomAlbums(limit: Int = 50): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE navidromeID = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM albums")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM albums")
    fun getCountFlow(): Flow<Int>

    // Batch query for artist albums cache-first
    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY year DESC")
    suspend fun getAlbumsByArtistOnce(artistId: String): List<AlbumEntity>

    /**
     * Replaces all albums atomically.
     * Uses batch processing to avoid memory issues with large datasets.
     * The @Transaction annotation ensures all-or-nothing semantics.
     */
    @Transaction
    suspend fun replaceAll(albums: List<AlbumEntity>) {
        deleteAll()
        // Batch insert to avoid memory pressure with large lists
        albums.chunked(500).forEach { batch ->
            insertAll(batch)
        }
    }

    @Query("UPDATE albums SET starred = :starred WHERE navidromeID = :id")
    suspend fun updateStarred(id: String, starred: String?)

    @Upsert
    suspend fun upsert(album: AlbumEntity)

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)
}
