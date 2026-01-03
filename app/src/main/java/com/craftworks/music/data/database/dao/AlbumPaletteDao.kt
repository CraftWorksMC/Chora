package com.craftworks.music.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.craftworks.music.data.database.entity.AlbumPaletteEntity

@Dao
interface AlbumPaletteDao {
    @Query("SELECT * FROM album_palettes WHERE imageUrl = :imageUrl")
    suspend fun getPalette(imageUrl: String): AlbumPaletteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(palette: AlbumPaletteEntity)

    @Query("SELECT imageUrl FROM album_palettes")
    suspend fun getAllUrls(): List<String>
    
    @Query("DELETE FROM album_palettes")
    suspend fun clear()
}
