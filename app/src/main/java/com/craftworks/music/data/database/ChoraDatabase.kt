package com.craftworks.music.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.craftworks.music.data.database.dao.AlbumDao
import com.craftworks.music.data.database.dao.AlbumPaletteDao
import com.craftworks.music.data.database.dao.ArtistDao
import com.craftworks.music.data.database.dao.DownloadDao
import com.craftworks.music.data.database.dao.OfflineSongDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.database.dao.SyncMetadataDao
import com.craftworks.music.data.database.entity.AlbumEntity
import com.craftworks.music.data.database.entity.AlbumPaletteEntity
import com.craftworks.music.data.database.entity.ArtistEntity
import com.craftworks.music.data.database.entity.DownloadEntity
import com.craftworks.music.data.database.entity.DownloadStatus
import com.craftworks.music.data.database.entity.MediaType
import com.craftworks.music.data.database.entity.OfflineSongEntity
import com.craftworks.music.data.database.entity.SongEntity
import com.craftworks.music.data.database.entity.SyncMetadata

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus {
        return try {
            DownloadStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // Safe fallback for unknown/corrupted values
            DownloadStatus.FAILED
        }
    }

    @TypeConverter
    fun fromMediaType(type: MediaType): String = type.name

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return try {
            MediaType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // Safe fallback for unknown/corrupted values
            MediaType.SONG
        }
    }
}

@Database(
    entities = [
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        SyncMetadata::class,
        DownloadEntity::class,
        OfflineSongEntity::class,
        AlbumPaletteEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChoraDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun downloadDao(): DownloadDao
    abstract fun offlineSongDao(): OfflineSongDao
    abstract fun albumPaletteDao(): AlbumPaletteDao

    companion object {
        const val DATABASE_NAME = "chora_database"
    }
}
