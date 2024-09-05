package com.craftworks.music.providers.navidrome

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.NotificationUtil.IMPORTANCE_LOW
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.R
import com.craftworks.music.managers.NavidromeManager.getCurrentServer
import com.craftworks.music.player.SongHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

@OptIn(UnstableApi::class)
suspend fun downloadNavidromeSong(context: Context) {

    val channelId = "download_channel"
    createNotificationChannel(context, channelId, 0, 0, IMPORTANCE_LOW)

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Downloading file")
        .setContentText("Download in progress")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOnlyAlertOnce(true)
        .setProgress(100, 0, true)

    val notificationManager = NotificationManagerCompat.from(context)
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
    }
    notificationManager.notify(1, notificationBuilder.build())


    val server = getCurrentServer() ?: throw IllegalArgumentException("Could not get current server.")

    withContext(Dispatchers.IO) {
        val passwordSalt = generateSalt(8)
        val passwordHash = md5Hash(server.password + passwordSalt)

        val url = URL("${server.url}/rest/download.view?/id=${SongHelper.currentSong.navidromeID}&u=${server.username}&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora")

        try {
            val inputStream: InputStream = url.openStream()

            val outputFile = File("/Music/${SongHelper.currentSong.title} - ${SongHelper.currentSong.artist}.${SongHelper.currentSong.format}")
            val fileOutputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(4096)
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
            Toast.makeText(context, context.getString(R.string.Notification_Download_Success), Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()

            // Show failure notification
            notificationBuilder
                .setContentText(context.getString(R.string.Notification_Download_Failure))
                .setProgress(0, 0, false)
            notificationManager.notify(1, notificationBuilder.build())

            println("Download failed: ${e.message}")
        }
    }
}
