package com.craftworks.music.providers.navidrome

import android.Manifest
import android.content.Context
import android.util.Log
import android.content.pm.PackageManager
import android.os.Environment
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.NotificationUtil.IMPORTANCE_LOW
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.R
import com.craftworks.music.managers.NavidromeManager.getCurrentServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLEncoder

@OptIn(UnstableApi::class)
suspend fun downloadNavidromeSongs(
    context: Context,
    songs: List<MediaMetadata>,
    onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
) {
    songs.forEachIndexed { index, song ->
        onProgress(index + 1, songs.size)
        downloadNavidromeSong(context, song)
    }
}

suspend fun downloadNavidromeSong(
    context: Context,
    song: MediaMetadata
) {
    val notificationId = (song.extras?.getString("navidromeID")?.hashCode() ?: System.currentTimeMillis().toInt())
    val channelId = "download_channel_export" // Distinct channel for exports

    createNotificationChannel(
        context,
        channelId,
        R.string.Notification_Download_Name,
        R.string.Notification_Download_Desc,
        IMPORTANCE_LOW
    )

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(context.getString(R.string.Notification_Download_Progress) + " ${song.title} - ${song.artist}")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOnlyAlertOnce(true)
        .setProgress(100, 0, true)
        .setOngoing(true) // Prevent swipe away while downloading

    val notificationManager = NotificationManagerCompat.from(context)
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    notificationManager.notify(notificationId, notificationBuilder.build())

    val server = getCurrentServer() ?: throw IllegalArgumentException("Could not get current server.")

    withContext(Dispatchers.IO) {
        // ... (hashing logic same as before) ...
        val passwordSalt = generateSalt(8)
        val passwordHash = md5Hash(server.password + passwordSalt)
        val encodedUsername = URLEncoder.encode(server.username, "UTF-8")

        // Fix: Don't encode MD5 hash or Salt if they are already hex/plain, but here they seem fine.
        // DownloadWorker encoded them. Consistency would be good.
        // But Navidrome API expects them as is usually. Let's keep existing logic but just fix the File I/O.

        val urlString = "${server.url}/rest/download.view?id=${song.extras?.getString("navidromeID")}&u=$encodedUsername&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora"

        // Log.d("NAVIDROME", "Downloading song: ${song.title}") // Don't log potentially PII if not needed, but title is fine.

        var tempFile: File? = null
        var outputFile: File? = null

        try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (!musicDir.exists()) {
                if (!musicDir.mkdirs()) {
                    throw java.io.IOException("Failed to create Music directory")
                }
            }

            // Check for free space (approximate) - require at least 50MB buffer
            val stat = android.os.StatFs(musicDir.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            if (bytesAvailable < 50 * 1024 * 1024) {
                 throw java.io.IOException("Insufficient disk space")
            }

            // Whitelist sanitization (Safer!)
            val sanitizedTitle = song.title?.toString()?.replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "")?.trim() ?: "Unknown_Title"
            val sanitizedArtist = song.artist?.toString()?.replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "")?.trim() ?: "Unknown_Artist"
            val format = song.extras?.getString("format")?.replace(Regex("[^a-zA-Z0-9]"), "") ?: "mp3"

            val fileName = "$sanitizedTitle - $sanitizedArtist.$format"
            outputFile = File(musicDir, fileName)
            tempFile = File(musicDir, "$fileName.tmp")

            // Security check for path traversal
            if (!outputFile!!.canonicalPath.startsWith(musicDir.canonicalPath)) {
                throw SecurityException("Path traversal attempt detected")
            }

            val url = URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.connect()

            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("Server returned HTTP ${connection.responseCode}")
            }

            val fileSize = connection.contentLengthLong

            connection.inputStream.use { inputStream ->
                FileOutputStream(tempFile).use { fileOutputStream ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var bytesRead: Int
                    var lastProgressUpdate = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Throttle notification updates (every 500ms)
                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate > 500) {
                            val progress = if (fileSize > 0) ((totalBytesRead * 100) / fileSize).toInt() else 0
                            notificationBuilder.setProgress(100, progress, false)
                            notificationManager.notify(notificationId, notificationBuilder.build())
                            lastProgressUpdate = now
                        }
                    }
                    // Ensure flush to disk
                    fileOutputStream.fd.sync()
                }
            }

            // Atomic rename
            if (tempFile!!.exists() && !tempFile!!.renameTo(outputFile)) {
                 // Try copy if rename fails
                 tempFile!!.copyTo(outputFile!!, overwrite = true)
                 tempFile!!.delete()
            }

            Log.d("NAVIDROME", "Download completed: ${outputFile!!.absolutePath}")

            withContext(Dispatchers.Main) {
                notificationManager.cancel(notificationId)
                Toast.makeText(context, "${song.title} " + context.getString(R.string.Notification_Download_Success), Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup temp file
            tempFile?.delete()
            // We do NOT delete outputFile here because we only write to tempFile.
            // If outputFile existed before, it is preserved.
            // If rename failed, we are safe.

            withContext(Dispatchers.Main) {
                notificationBuilder
                    .setContentText(context.getString(R.string.Notification_Download_Failure) + ": " + e.localizedMessage)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(notificationId, notificationBuilder.build())

                Toast.makeText(context, context.getString(R.string.Notification_Download_Failure), Toast.LENGTH_SHORT).show()
            }
        }
    }
}