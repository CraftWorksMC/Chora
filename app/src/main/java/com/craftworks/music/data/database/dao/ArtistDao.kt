package com.craftworks.music.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.craftworks.music.data.database.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    // Sort unknown/blank artists to the end
    @Query("""
        SELECT * FROM artists
        ORDER BY
            CASE
                WHEN name IS NULL OR name = '' OR LOWER(name) LIKE '%unknown%' THEN 1
                ELSE 0
            END,
            name COLLATE NOCASE ASC
    """)
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("""
        SELECT * FROM artists
        ORDER BY
            CASE
                WHEN name IS NULL OR name = '' OR LOWER(name) LIKE '%unknown%' THEN 1
                ELSE 0
            END,
            name COLLATE NOCASE ASC
    """)
    suspend fun getAllArtistsOnce(): List<ArtistEntity>

    @Query("SELECT * FROM artists WHERE navidromeID = :id")
    suspend fun getArtistById(id: String): ArtistEntity?

    @Query("SELECT * FROM artists WHERE starred IS NOT NULL AND starred != ''")
    fun getStarredArtists(): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artist: ArtistEntity)

    @Query("DELETE FROM artists WHERE navidromeID = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM artists")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM artists")
    fun getCountFlow(): Flow<Int>

    /**
     * Replaces all artists atomically.
     * Uses batch processing to avoid memory issues with large datasets.
     * The @Transaction annotation ensures all-or-nothing semantics.
     */
    @Transaction
    suspend fun replaceAll(artists: List<ArtistEntity>) {
        deleteAll()
        // Batch insert to avoid memory pressure with large lists
        artists.chunked(500).forEach { batch ->
            insertAll(batch)
        }
    }

    @Query("UPDATE artists SET starred = :starred WHERE navidromeID = :id")
    suspend fun updateStarred(id: String, starred: String?)

    @Upsert
    suspend fun upsert(artist: ArtistEntity)

    @Upsert
    suspend fun upsertAll(artists: List<ArtistEntity>)
}
