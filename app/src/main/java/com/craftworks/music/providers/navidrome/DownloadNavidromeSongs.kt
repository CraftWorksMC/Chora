package com.craftworks.music.providers.navidrome

import android.Manifest
import android.content.Context
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
import com.craftworks.music.providers.local.LocalProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

@OptIn(UnstableApi::class)
suspend fun downloadNavidromeSong(
    context: Context,
    song: MediaMetadata
) {
    val channelId = "download_channel"
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

    val notificationManager = NotificationManagerCompat.from(context)
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    notificationManager.notify(1, notificationBuilder.build())


    val server = getCurrentServer() ?: throw IllegalArgumentException("Could not get current server.")

    withContext(Dispatchers.IO) {
        val passwordSalt = generateSalt(8)
        val passwordHash = md5Hash(server.password + passwordSalt)

        val url = URL("${server.url}/rest/download.view?id=${song.extras?.getString("navidromeID")}&u=${server.username}&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

        println("DOWNLOADING FROM: $url")

        try {
            val inputStream: InputStream = url.openStream()

            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val fileName = "${song.title} - ${song.artist}.${song.extras?.getString("format")}"
            val outputFile = File(musicDir, fileName)
            val fileOutputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            val fileSize = url.openConnection().contentLength

            // Read from the input stream and write to the file output stream
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOutputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                // Update progress notification
                val progress = (totalBytesRead * 100 / fileSize).toInt()
                notificationBuilder.setProgress(100, progress, false)
                notificationManager.notify(1, notificationBuilder.build())
            }

            // Close streams
            fileOutputStream.close()
            inputStream.close()

            println("Download completed: ${outputFile.absolutePath}")

            withContext(Dispatchers.Main) {
                notificationManager.cancel(1)

                Toast.makeText(
                    context,
                    "${song.title} " + context.getString(R.string.Notification_Download_Success),
                    Toast.LENGTH_SHORT
                ).show()

                LocalProvider.getInstance().scanLocalFiles()
            }

        } catch (e: Exception) {
            e.printStackTrace()


            // Show failure notification
            notificationBuilder
                .setContentText(context.getString(R.string.Notification_Download_Failure) + " " + song.title)
                .setProgress(0, 0, false)
            notificationManager.notify(1, notificationBuilder.build())

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.Notification_Download_Failure) + " " + song.title,
                    Toast.LENGTH_SHORT
                ).show()
            }

            println("Download failed: ${e.message}")
        }
    }
}