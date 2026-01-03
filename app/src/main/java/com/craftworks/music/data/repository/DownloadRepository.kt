package com.craftworks.music.data.repository

import android.content.Context
import androidx.media3.common.MediaMetadata
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.craftworks.music.data.database.dao.DownloadDao
import com.craftworks.music.data.database.dao.OfflineSongDao
import com.craftworks.music.data.database.entity.DownloadEntity
import com.craftworks.music.data.database.entity.DownloadStatus
import com.craftworks.music.data.database.entity.MediaType
import com.craftworks.music.worker.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val offlineSongDao: OfflineSongDao
) {
    private val workManager = WorkManager.getInstance(context)

    // Flows for UI observation
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()
    val failedDownloads: Flow<List<DownloadEntity>> = downloadDao.getFailedDownloads()
    val activeDownloadCount: Flow<Int> = downloadDao.getActiveDownloadCount()

    val hasActiveDownloads: Flow<Boolean> = activeDownloadCount.map { it > 0 }

    suspend fun queueSongDownload(song: MediaMetadata): String {
        val mediaId = song.extras?.getString("navidromeID") ?: return ""
        val format = song.extras?.getString("format") ?: "mp3"

        // Check if already offline - file exists and is available
        if (offlineSongDao.isOfflineAvailable(mediaId)) {
            return ""
        }

        val downloadId = UUID.randomUUID().toString()

        val downloadEntity = DownloadEntity(
            id = downloadId,
            mediaId = mediaId,
            mediaType = MediaType.SONG,
            title = song.title?.toString() ?: "Unknown",
            artist = song.artist?.toString() ?: "Unknown",
            albumTitle = song.albumTitle?.toString(),
            imageUrl = song.artworkUri?.toString(),
            status = DownloadStatus.QUEUED,
            queuedAt = System.currentTimeMillis(),
            format = format
        )

        // Use insertOrIgnore to handle race conditions atomically
        // If mediaId already exists (unique constraint), insertion returns -1
        val inserted = downloadDao.insertOrIgnore(downloadEntity)
        if (inserted == -1L) {
            // Already exists in downloads table - check if it's a completed download
            val existingDownload = downloadDao.getDownloadByMediaId(mediaId)
            if (existingDownload != null) {
                // If it's completed but somehow offline check failed, don't re-download
                if (existingDownload.status == DownloadStatus.COMPLETED) {
                    return ""
                }
                // If it's failed/paused, return existing ID (user can retry manually)
                return existingDownload.id
            }
            return ""
        }

        scheduleDownload(downloadEntity)

        return downloadId
    }

    suspend fun queueSongsDownload(songs: List<MediaMetadata>): List<String> {
        return songs.mapNotNull { song ->
            queueSongDownload(song).takeIf { it.isNotEmpty() }
        }
    }

    private fun scheduleDownload(download: DownloadEntity) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                DownloadWorker.KEY_DOWNLOAD_ID to download.id,
                DownloadWorker.KEY_MEDIA_ID to download.mediaId,
                DownloadWorker.KEY_TITLE to download.title,
                DownloadWorker.KEY_ARTIST to download.artist,
                DownloadWorker.KEY_FORMAT to download.format,
                DownloadWorker.KEY_IMAGE_URL to download.imageUrl
            ))
            .addTag("download")
            .addTag("download_${download.mediaId}")
            .build()

        workManager.enqueueUniqueWork(
            "download_${download.mediaId}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    suspend fun cancelDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId) ?: return

        // Cancel the work
        workManager.cancelUniqueWork("download_${download.mediaId}")

        // Remove from database
        downloadDao.deleteById(downloadId)
    }

    suspend fun pauseDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId) ?: return

        // Cancel the work (WorkManager doesn't support true pause)
        workManager.cancelUniqueWork("download_${download.mediaId}")

        // Update status
        downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
    }

    suspend fun resumeDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId) ?: return

        if (download.status != DownloadStatus.PAUSED) return

        // Reset status and re-queue
        downloadDao.updateStatus(downloadId, DownloadStatus.QUEUED)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                DownloadWorker.KEY_DOWNLOAD_ID to download.id,
                DownloadWorker.KEY_MEDIA_ID to download.mediaId,
                DownloadWorker.KEY_TITLE to download.title,
                DownloadWorker.KEY_ARTIST to download.artist,
                DownloadWorker.KEY_FORMAT to download.format,
                DownloadWorker.KEY_IMAGE_URL to download.imageUrl
            ))
            .addTag("download")
            .addTag("download_${download.mediaId}")
            .build()

        workManager.enqueueUniqueWork(
            "download_${download.mediaId}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun retryDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId) ?: return

        if (download.status != DownloadStatus.FAILED) return

        // Reset for retry
        downloadDao.resetForRetry(downloadId)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                DownloadWorker.KEY_DOWNLOAD_ID to download.id,
                DownloadWorker.KEY_MEDIA_ID to download.mediaId,
                DownloadWorker.KEY_TITLE to download.title,
                DownloadWorker.KEY_ARTIST to download.artist,
                DownloadWorker.KEY_FORMAT to download.format,
                DownloadWorker.KEY_IMAGE_URL to download.imageUrl
            ))
            .addTag("download")
            .addTag("download_${download.mediaId}")
            .build()

        workManager.enqueueUniqueWork(
            "download_${download.mediaId}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun clearCompleted() {
        downloadDao.deleteCompleted()
    }

    suspend fun deleteOfflineSong(songId: String) {
        val offlineSong = offlineSongDao.getOfflineSong(songId) ?: return

        // Delete file
        try {
            val file = java.io.File(offlineSong.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}

        // Remove from database
        offlineSongDao.deleteBySongId(songId)
    }

    suspend fun isOfflineAvailable(songId: String): Boolean {
        return offlineSongDao.isOfflineAvailable(songId)
    }

    fun isOfflineAvailableFlow(songId: String): Flow<Boolean> {
        return offlineSongDao.isOfflineAvailableFlow(songId)
    }

    suspend fun getOfflinePath(songId: String): String? {
        return offlineSongDao.getAvailableOfflineSong(songId)?.localFilePath
    }

    suspend fun pauseAll() {
        workManager.cancelAllWorkByTag("download")
        // Also update DB status for all active downloads
        downloadDao.pauseAllActive()
    }

    suspend fun resumeAll() {
        // Get all paused downloads and resume them
        val pausedDownloads = downloadDao.getPausedDownloads()
        pausedDownloads.forEach { download ->
            resumeDownload(download.id)
        }
    }

    /**
     * Cleans up orphaned OfflineSongEntity records where the local file no longer exists.
     * This handles cases where files were deleted externally or through system cleanup.
     * Returns the number of orphaned entries cleaned up.
     */
    suspend fun cleanupOrphanedOfflineSongs(): Int {
        var cleanedUp = 0
        val allOfflineSongs = offlineSongDao.getAllAvailableOnce()

        for (offlineSong in allOfflineSongs) {
            val file = java.io.File(offlineSong.localFilePath)
            if (!file.exists()) {
                // File is missing, mark as unavailable
                offlineSongDao.markUnavailable(offlineSong.songId)
                cleanedUp++
            }
        }

        return cleanedUp
    }

    /**
     * Removes all unavailable offline song entries from the database.
     * Call this after cleanupOrphanedOfflineSongs() to permanently delete the records.
     */
    suspend fun purgeUnavailableOfflineSongs() {
        offlineSongDao.deleteUnavailable()
    }
}
