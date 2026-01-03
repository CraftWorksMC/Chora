package com.craftworks.music.data.di

import android.content.Context
import androidx.room.Room
import com.craftworks.music.data.database.ChoraDatabase
import com.craftworks.music.data.database.Migrations
import com.craftworks.music.data.database.dao.AlbumDao
import com.craftworks.music.data.database.dao.AlbumPaletteDao
import com.craftworks.music.data.database.dao.ArtistDao
import com.craftworks.music.data.database.dao.DownloadDao
import com.craftworks.music.data.database.dao.OfflineSongDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.database.dao.SyncMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChoraDatabase {
        return Room.databaseBuilder(
            context,
            ChoraDatabase::class.java,
            ChoraDatabase.DATABASE_NAME
        )
        .addMigrations(*Migrations.ALL_MIGRATIONS)
        .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: ChoraDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun provideAlbumDao(database: ChoraDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    @Singleton
    fun provideArtistDao(database: ChoraDatabase): ArtistDao {
        return database.artistDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: ChoraDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: ChoraDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideOfflineSongDao(database: ChoraDatabase): OfflineSongDao {
        return database.offlineSongDao()
    }
    
    @Provides
    @Singleton
    fun provideAlbumPaletteDao(database: ChoraDatabase): AlbumPaletteDao {
        return database.albumPaletteDao()
    }
}
