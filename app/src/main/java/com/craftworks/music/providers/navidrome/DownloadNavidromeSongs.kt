package com.craftworks.music.providers.navidrome

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.craftworks.music.R
import com.craftworks.music.managers.NavidromeManager.getCurrentServer
import java.io.File

@OptIn(UnstableApi::class)
fun downloadNavidromeSong(
    context: Context,
    song: MediaMetadata,
    albumName: String? = null
) {
    val server = getCurrentServer() ?: return

    val passwordSalt = generateSalt(8)
    val passwordHash = md5Hash(server.password + passwordSalt)
    val url = "${server.url}/rest/download.view?id=${song.extras?.getString("navidromeID")}&u=${server.username}&t=$passwordHash&s=$passwordSalt&v=1.16.1&c=Chora".toUri()

    val extension = song.extras?.getString("format") ?: "mp3"
    val fileName = "${song.title} - ${song.artist}.$extension"

    val relativePath = if (!albumName.isNullOrBlank()) {
        "$albumName${File.separator}$fileName"
    } else {
        fileName
    }

    val request = DownloadManager.Request(url)
        .setTitle("${context.getString(R.string.Notification_Download_Name)} ${song.title}")
        .setDescription(context.getString(R.string.Notification_Download_Desc))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, relativePath)

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}