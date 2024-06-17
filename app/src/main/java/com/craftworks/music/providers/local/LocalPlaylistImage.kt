package com.craftworks.music.providers.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import com.craftworks.music.data.MediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

suspend fun localPlaylistImageGenerator(songs:List<MediaData.Song>, context:Context): Uri? {
    println("Creating Local Playlist Image")
    if (songs.any { it.navidromeID != "Local" }) return Uri.EMPTY

    // Determine the maximum width and height among the images
    val maxWidth = songs.take(4).maxOfOrNull { uri ->
        val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri.imageUrl))
        val bitmap = ImageDecoder.decodeBitmap(source)
        bitmap.width
    } ?: 0

    val maxHeight = songs.take(4).maxOfOrNull { uri ->
        val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri.imageUrl))
        val bitmap = ImageDecoder.decodeBitmap(source)
        bitmap.height
    } ?: 0

    // Create a new Bitmap to hold the combined images
    val combinedBitmap = Bitmap.createBitmap(maxWidth * 2, maxHeight * 2, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.RGBA_F16, true)
    val canvas = Canvas(combinedBitmap)

    // Convert URIs to Bitmaps and draw onto the combined Bitmap
    songs.take(4).forEachIndexed { index, uri ->
        val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri.imageUrl))
        val bitmap = ImageDecoder.decodeBitmap(source)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true).copy(Bitmap.Config.RGBA_F16, true)
        val x = (index % 2) * maxWidth
        val y = (index / 2) * maxHeight
        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
    }

    // Save the combined Bitmap to a file
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "combined_image.png")
    try {
        val fos = withContext(Dispatchers.IO) {
            FileOutputStream(file)
        }
        combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        withContext(Dispatchers.IO) {
            fos.close()
        }
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }

    println("Local Playlist Image Ready!")
    return Uri.fromFile(file)
}