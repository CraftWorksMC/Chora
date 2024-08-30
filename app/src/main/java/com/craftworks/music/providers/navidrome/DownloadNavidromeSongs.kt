package com.craftworks.music.providers.navidrome

import android.os.Environment
import androidx.compose.material3.SnackbarHostState
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

fun downloadNavidromeSong(url: String, snackbarHostState: SnackbarHostState? = SnackbarHostState(), coroutineScope: CoroutineScope) {
    val thread = Thread {
        try {
            if (SongHelper.currentSong.isRadio == true || !NavidromeManager.checkActiveServers()) return@Thread

            println("\nSent 'GET' request to URL : $url")
            val destinationFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
            println("MUSIC_DIR: $destinationFolder")
            val fileUrl = URL(url)
            val connection: HttpURLConnection = fileUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val inputStream = BufferedInputStream(connection.inputStream)

            //val fileName = playingSong.selectedSong?.title + "." + playingSong.selectedSong?.format
            val contentDisposition = connection.getHeaderField("Content-Disposition")
            val fileName = extractNavidromeSongName(contentDisposition) ?: "downloaded_song.mp3"
            val outputFile = File(destinationFolder, fileName)
            val outputStream = FileOutputStream(outputFile)

            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }



            inputStream.close()
            outputStream.close()
            connection.disconnect()
            println("Song downloaded to: ${outputFile.absolutePath}")
            coroutineScope.launch {
                snackbarHostState?.showSnackbar("Song downloaded to: ${outputFile.absolutePath}")
            }

        } catch (e: Exception) {
            println(e)
        }
    }
    thread.start()
}

fun extractNavidromeSongName(contentDisposition: String?): String? {
    if (contentDisposition == null) return null

    val startIndex = contentDisposition.indexOf("filename=")
    if (startIndex == -1) return null

    var endIndex = contentDisposition.indexOf(";", startIndex)
    if (endIndex == -1) {
        endIndex = contentDisposition.length
    }

    var fileName = contentDisposition.substring(startIndex + 9, endIndex).trim('\"')

    // Remove any path or folder structure and keep only the base filename
    fileName = fileName.substringAfterLast('/')

    return fileName
}