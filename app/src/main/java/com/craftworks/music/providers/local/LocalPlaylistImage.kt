package com.craftworks.music.providers.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import com.craftworks.music.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

suspend fun localPlaylistImageGenerator(songs:List<Song>, context:Context): Uri? {
    println("Creating Local Playlist Image")
    // Convert URIs to Bitmaps
    val bitmaps = songs.take(4).map { uri ->
        val source = ImageDecoder.createSource(context.contentResolver, uri.imageUrl)
        ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.RGBA_F16, true)
    }

    // Determine the size of each Bitmap and the combined Bitmap
    val bitmapSize = bitmaps.first().width // Assuming all Bitmaps are the same size
    val combinedWidth = bitmapSize * 2 // For a 2x2 grid
    val combinedHeight = bitmapSize * 2

    // Create a new Bitmap to hold the combined images
    val combinedBitmap = Bitmap.createBitmap(combinedWidth, combinedHeight, Bitmap.Config.ARGB_8888).copy(Bitmap.Config.RGBA_F16, true)
    val canvas = Canvas(combinedBitmap)

    // Draw each Bitmap onto the combined Bitmap
    bitmaps.forEachIndexed { index, bitmap ->
        val x = (index % 2) * bitmapSize
        val y = (index / 2) * bitmapSize
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
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
    // Return the URI of the saved file
    return Uri.fromFile(file)
}