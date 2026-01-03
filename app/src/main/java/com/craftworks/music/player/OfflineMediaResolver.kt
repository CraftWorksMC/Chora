package com.craftworks.music.player

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.craftworks.music.data.database.dao.OfflineSongDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves MediaItems to use local offline files when available.
 * This enables seamless offline playback by substituting remote URLs
 * with local file paths for downloaded songs.
 */
@Singleton
class OfflineMediaResolver @Inject constructor(
    private val offlineSongDao: OfflineSongDao,
    @ApplicationContext private val context: Context
) {
    // Expected base directories for offline files - prevents path traversal attacks
    // Use getter to ensure we always get the latest state of storage volumes
    private val allowedBaseDirs: List<File>
        get() = listOfNotNull(
            context.filesDir,
            context.getExternalFilesDir(null),
            context.cacheDir
        )

    /**
     * Validates that the file path is within allowed directories.
     * Prevents path traversal attacks from malicious database entries.
     */
    private fun isPathSafe(path: String): Boolean {
        try {
            val file = File(path).canonicalFile
            val currentAllowed = allowedBaseDirs

            // Fail safe if no directories are allowed/available
            if (currentAllowed.isEmpty()) {
                Log.w("OfflineMediaResolver", "Security: No allowed base directories available")
                return false
            }

            return currentAllowed.any { baseDir ->
                file.canonicalPath.startsWith(baseDir.canonicalPath + File.separator)
            }
        } catch (e: Exception) {
            Log.e("OfflineMediaResolver", "Security: Error validating path $path", e)
            return false
        }
    }
    /**
     * Resolves a single MediaItem, returning a new MediaItem with local file URI
     * if the song is available offline, or the original MediaItem otherwise.
     */
    suspend fun resolve(mediaItem: MediaItem): MediaItem {
        val songId = mediaItem.mediaMetadata.extras?.getString("navidromeID") ?: return mediaItem

        // Check if this song is available offline
        val offlineSong = offlineSongDao.getAvailableOfflineSong(songId) ?: return mediaItem

        // Security: Validate path is within allowed directories
        if (!isPathSafe(offlineSong.localFilePath)) {
            Log.e("OfflineMediaResolver", "Security: Rejecting unsafe path for song $songId: ${offlineSong.localFilePath}")
            offlineSongDao.markUnavailable(songId)
            return mediaItem
        }

        // Validate file exists before using it
        val file = File(offlineSong.localFilePath)
        if (!file.exists()) {
            Log.w("OfflineMediaResolver", "Offline file missing for song $songId: ${offlineSong.localFilePath}")
            // Mark as unavailable so we don't keep trying
            offlineSongDao.markUnavailable(songId)
            return mediaItem
        }

        // Update last accessed timestamp using songId (navidrome ID), not id (primary key)
        offlineSongDao.updateLastAccessed(offlineSong.songId, System.currentTimeMillis())

        // Create a new MediaItem with the local file URI
        return mediaItem.buildUpon()
            .setUri(offlineSong.localFilePath.toUri())
            .build()
    }

    /**
     * Resolves a list of MediaItems, substituting local file URIs for any
     * songs that are available offline.
     */
    suspend fun resolveAll(mediaItems: List<MediaItem>): List<MediaItem> {
        return mediaItems.map { resolve(it) }
    }

    /**
     * Checks if a specific song is available offline.
     */
    suspend fun isOfflineAvailable(songId: String): Boolean {
        val offlineSong = offlineSongDao.getAvailableOfflineSong(songId) ?: return false
        // Security check + verify file exists
        return isPathSafe(offlineSong.localFilePath) && File(offlineSong.localFilePath).exists()
    }

    /**
     * Gets the local file path for an offline song, if available.
     * Returns null if the song is not available offline or the file is missing.
     */
    suspend fun getOfflinePath(songId: String): String? {
        val offlineSong = offlineSongDao.getAvailableOfflineSong(songId) ?: return null

        // Security: Validate path is within allowed directories
        if (!isPathSafe(offlineSong.localFilePath)) {
            Log.e("OfflineMediaResolver", "Security: Rejecting unsafe path for song $songId: ${offlineSong.localFilePath}")
            offlineSongDao.markUnavailable(songId)
            return null
        }

        // Validate file exists
        val file = File(offlineSong.localFilePath)
        if (!file.exists()) {
            Log.w("OfflineMediaResolver", "Offline file missing for song $songId: ${offlineSong.localFilePath}")
            offlineSongDao.markUnavailable(songId)
            return null
        }

        // Update last accessed timestamp using songId (navidrome ID), not id (primary key)
        offlineSongDao.updateLastAccessed(offlineSong.songId, System.currentTimeMillis())
        return offlineSong.localFilePath
    }
}
