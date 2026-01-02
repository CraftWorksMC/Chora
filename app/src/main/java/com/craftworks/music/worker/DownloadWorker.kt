package com.craftworks.music.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.media3.common.util.NotificationUtil.IMPORTANCE_LOW
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.craftworks.music.R
import com.craftworks.music.data.database.dao.DownloadDao
import com.craftworks.music.data.database.dao.OfflineSongDao
import com.craftworks.music.data.database.entity.DownloadStatus
import com.craftworks.music.data.database.entity.OfflineSongEntity
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.generateSalt
import com.craftworks.music.providers.navidrome.md5Hash
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import androidx.room.withTransaction
import com.craftworks.music.data.database.ChoraDatabase

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val database: ChoraDatabase,
    private val downloadDao: DownloadDao,
    private val offlineSongDao: OfflineSongDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_FORMAT = "format"
        const val KEY_IMAGE_URL = "image_url"

        const val KEY_PROGRESS = "progress"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"

        private const val CHANNEL_ID = "download_channel"
        private val notificationCounter = AtomicInteger(2000)
    }

    // Unique notification ID for this worker instance
    private val notificationId = notificationCounter.getAndIncrement()

    @androidx.annotation.OptIn(UnstableApi::class)
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        val mediaId = inputData.getString(KEY_MEDIA_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Unknown"
        val artist = inputData.getString(KEY_ARTIST) ?: "Unknown"
        val format = inputData.getString(KEY_FORMAT) ?: "mp3"

        Log.d("DownloadWorker", "Starting download for: $title - $artist")

        // Update status to downloading
        downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING)

        // Set foreground with notification
        setForeground(createForegroundInfo(title, artist, 0))

        return try {
            val localPath = downloadFile(downloadId, mediaId, title, artist, format)

            // Atomic completion: mark completed AND create offline entry in single transaction
            val file = File(localPath)
            val offlineSong = OfflineSongEntity(
                id = UUID.randomUUID().toString(),
                songId = mediaId,
                localFilePath = localPath,
                fileSize = file.length(),
                downloadedAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis(),
                isAvailable = true
            )

            database.withTransaction {
                downloadDao.markCompleted(downloadId, System.currentTimeMillis(), localPath)
                offlineSongDao.insert(offlineSong)
            }

            Log.d("DownloadWorker", "Download completed: $localPath")
            Result.success()

        } catch (e: java.net.SocketTimeoutException) {
            Log.e("DownloadWorker", "Network timeout: ${e.message}", e)
            downloadDao.markFailed(downloadId, "Network timeout")
            handleRetry(downloadId)

        } catch (e: java.net.UnknownHostException) {
            Log.e("DownloadWorker", "No network: ${e.message}", e)
            downloadDao.markFailed(downloadId, "No network connection")
            handleRetry(downloadId)

        } catch (e: java.io.IOException) {
            Log.e("DownloadWorker", "IO error: ${e.message}", e)
            downloadDao.markFailed(downloadId, "Download failed: ${e.message}")
            handleRetry(downloadId)

        } catch (e: IllegalStateException) {
            // Config error (no server) - don't retry
            Log.e("DownloadWorker", "Config error: ${e.message}", e)
            downloadDao.markFailed(downloadId, e.message ?: "Configuration error")
            Result.failure()

        } catch (e: Exception) {
            Log.e("DownloadWorker", "Download failed: ${e.message}", e)
            downloadDao.markFailed(downloadId, e.message ?: "Unknown error")
            handleRetry(downloadId)
        }
    }

    private suspend fun handleRetry(downloadId: String): Result {
        val download = downloadDao.getDownloadById(downloadId)
        return if (download != null && download.retryCount < 3) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private suspend fun downloadFile(
        downloadId: String,
        mediaId: String,
        title: String,
        artist: String,
        format: String
    ): String = withContext(Dispatchers.IO) {
        val server = NavidromeManager.getCurrentServer()
            ?: throw IllegalStateException("No server configured")

        val passwordSalt = generateSalt(8)
        val passwordHash = md5Hash(server.password + passwordSalt)

        // URL encode parameters to handle special characters
        val encodedMediaId = URLEncoder.encode(mediaId, "UTF-8")
        val encodedUsername = URLEncoder.encode(server.username, "UTF-8")
        val encodedHash = URLEncoder.encode(passwordHash, "UTF-8")
        val encodedSalt = URLEncoder.encode(passwordSalt, "UTF-8")

        val downloadUrl = "${server.url}/rest/download.view?id=$encodedMediaId&u=$encodedUsername&t=$encodedHash&s=$encodedSalt&v=1.16.1&c=Chora"

        Log.d("DownloadWorker", "Downloading from: ${server.url}/rest/download.view?id=$encodedMediaId")

        // Use app-specific external storage (no permissions needed on Android 10+)
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: throw IllegalStateException("Cannot access music directory")

        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }

        // Sanitize filename
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "").trim()
        val sanitizedArtist = artist.replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "").trim()
        val fileName = "$sanitizedTitle - $sanitizedArtist.$format"
        val outputFile = File(musicDir, fileName)

        // Use temp file for atomic writes - prevents partial corrupt files
        val tempFile = File(musicDir, "$fileName.tmp")

        val url = URL(downloadUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            val totalBytes = connection.contentLength.toLong()

            connection.inputStream.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastReportedProgress = -1  // Track last reported percentage

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress
                        val progress = if (totalBytes > 0) {
                            (totalBytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        val currentPercentage = (progress * 100).toInt()

                        // Throttle updates: only update DB every 2% change
                        if (currentPercentage >= lastReportedProgress + 2 || currentPercentage == 100) {
                            lastReportedProgress = currentPercentage

                            // Update database
                            downloadDao.updateProgress(downloadId, DownloadStatus.DOWNLOADING, progress, totalBytesRead)

                            // Update notification
                            setForeground(createForegroundInfo(title, artist, currentPercentage))

                            // Set progress for observers
                            setProgress(workDataOf(
                                KEY_PROGRESS to progress,
                                KEY_BYTES_DOWNLOADED to totalBytesRead,
                                KEY_TOTAL_BYTES to totalBytes
                            ))
                        }
                    }
                }
            }

            // Atomic rename: only after successful download
            if (!tempFile.renameTo(outputFile)) {
                // Fallback: copy and delete if rename fails (cross-filesystem)
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            // Clean up partial/temp file on failure
            tempFile.delete()
            outputFile.delete()
            throw e
        } finally {
            // Always disconnect the connection
            connection.disconnect()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun createForegroundInfo(title: String, artist: String, progress: Int): ForegroundInfo {
        createNotificationChannel(
            context,
            CHANNEL_ID,
            R.string.Notification_Download_Name,
            R.string.Notification_Download_Desc,
            IMPORTANCE_LOW
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.Notification_Download_Progress))
            .setContentText("$title - $artist")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .build()

        return ForegroundInfo(notificationId, notification)
    }
}
