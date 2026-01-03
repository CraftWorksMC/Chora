package com.craftworks.music.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for ChoraDatabase.
 * Each migration handles schema changes between database versions.
 */
object Migrations {

    /**
     * Migration from version 1 to 2.
     * Adds indices to songs, albums, and artists tables for performance.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add indices to songs table
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_albumId` ON `songs` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_artistId` ON `songs` (`artistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_starred` ON `songs` (`starred`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_lastPlayed` ON `songs` (`lastPlayed`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_dateAdded` ON `songs` (`dateAdded`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_timesPlayed` ON `songs` (`timesPlayed`)")

            // Add indices to albums table
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_artistId` ON `albums` (`artistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_starred` ON `albums` (`starred`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_created` ON `albums` (`created`)")

            // Add index to artists table
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_starred` ON `artists` (`starred`)")
        }
    }

    /**
     * Migration from version 2 to 3.
     * Adds downloads and offline_songs tables for the Download Manager feature.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create downloads table with format column and unique mediaId index
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `downloads` (
                    `id` TEXT NOT NULL,
                    `mediaId` TEXT NOT NULL,
                    `mediaType` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `albumTitle` TEXT,
                    `imageUrl` TEXT,
                    `status` TEXT NOT NULL,
                    `progress` REAL NOT NULL DEFAULT 0,
                    `bytesDownloaded` INTEGER NOT NULL DEFAULT 0,
                    `totalBytes` INTEGER NOT NULL DEFAULT 0,
                    `localFilePath` TEXT,
                    `workRequestId` TEXT,
                    `queuedAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    `failureReason` TEXT,
                    `retryCount` INTEGER NOT NULL DEFAULT 0,
                    `format` TEXT NOT NULL DEFAULT 'mp3',
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            // Create indices for downloads table - unique on mediaId to prevent duplicates
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_status` ON `downloads` (`status`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_downloads_mediaId` ON `downloads` (`mediaId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_queuedAt` ON `downloads` (`queuedAt`)")

            // Create offline_songs table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `offline_songs` (
                    `id` TEXT NOT NULL,
                    `songId` TEXT NOT NULL,
                    `localFilePath` TEXT NOT NULL,
                    `fileSize` INTEGER NOT NULL,
                    `downloadedAt` INTEGER NOT NULL,
                    `lastAccessedAt` INTEGER NOT NULL,
                    `isAvailable` INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            // Create indices for offline_songs table
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_offline_songs_songId` ON `offline_songs` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_songs_downloadedAt` ON `offline_songs` (`downloadedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_songs_isAvailable` ON `offline_songs` (`isAvailable`)")
        }
    }

    /**
     * Migration from version 3 to 4.
     * Adds missing index on isAvailable column for offline_songs table.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add index on isAvailable for faster offline availability queries
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_songs_isAvailable` ON `offline_songs` (`isAvailable`)")
        }
    }

    /**
     * Migration from version 4 to 5.
     * Adds album_palettes table for caching dynamic artwork colors.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `album_palettes` (
                    `imageUrl` TEXT NOT NULL,
                    `colors` TEXT NOT NULL,
                    PRIMARY KEY(`imageUrl`)
                )
            """.trimIndent())
        }
    }

    /**
     * Migration from version 5 to 6.
     * Adds missing indices for sorting and query optimization.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add index on artist name for sorting
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_artists_name` ON `artists` (`name`)")

            // Add indices on albums for sorting
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_name` ON `albums` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_year` ON `albums` (`year`)")

            // Add indices on songs for sorting
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_songs_discNumber_track` ON `songs` (`discNumber`, `track`)")
        }
    }

    /**
     * List of all migrations in order.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6
    )
}
