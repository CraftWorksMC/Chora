package com.craftworks.music.providers.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.media3.common.MediaItem
import com.craftworks.music.data.MediaData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream

/**
 * Generates a bitmap for playlist cover art by combining up to 4 album art images.
 * If less than 4 songs are available, it fills the grid by repeating images in a specific pattern.
 *
 * @param songs List of songs to create cover art from
 * @param context Android context used to access content resolver
 * @return Uri pointing to the saved image file, or null if generation failed
 */
suspend fun localPlaylistImageGenerator(songs: List<MediaItem>, context: Context): ByteArray? {
    println("Creating Playlist Cover Art")
    println("songs: ${songs.map { it.mediaMetadata.title }}")
    if (songs.isEmpty()) return null

    // Determine grid size
    val gridSize = if (songs.size == 1) 1 else 2 // 4x4 grid) 2 // 2x2 grid

    try {
        // Load and determine dimensions
        val bitmaps = mutableListOf<Bitmap>()
        var maxWidth = 0
        var maxHeight = 0

        // Get available song images first
        for (song in songs.take(4)) {
            try {
                val bitmap = loadBitmapFromUrl(song.mediaMetadata.artworkUri.toString(), context)
                if (bitmap != null) {
                    bitmaps.add(bitmap)

                    // Track maximum dimensions
                    maxWidth = maxOf(maxWidth, bitmap.width)
                    maxHeight = maxOf(maxHeight, bitmap.height)
                }
            } catch (e: Exception) {
                println("Error loading image for song: ${song.mediaMetadata.artworkUri}")
                e.printStackTrace()
                // Continue with other images
            }
        }

        // Use default dimensions if no valid images found
        if (maxWidth == 0) maxWidth = 256
        if (maxHeight == 0) maxHeight = 256

        // Create the combined bitmap
        val totalWidth = maxWidth * gridSize
        val totalHeight = maxHeight * gridSize
        val combinedBitmap = Bitmap.createBitmap(
            totalWidth, totalHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(combinedBitmap)

        // Fill the grid based on available images
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val position = row * gridSize + col
                val sourceIndex = when (bitmaps.size) {
                    1 -> 0
                    2 -> {
                        // Create checkerboard pattern: A B / B A
                        val isFirstRow = row == 0
                        val isFirstCol = col == 0
                        if ((isFirstRow && isFirstCol) || (!isFirstRow && !isFirstCol)) {
                            0 // Use image A for top-left and bottom-right
                        } else {
                            1 // Use image B for top-right and bottom-left
                        }
                    }
                    3 -> minOf(position, 2)
                    else -> position
                }

                if (sourceIndex < bitmaps.size) {
                    val bitmap = bitmaps[sourceIndex]

                    // Ensure bitmap is software-compatible before scaling
                    val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                        bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    } else {
                        bitmap
                    }

                    val scaledBitmap = Bitmap.createScaledBitmap(
                        softwareBitmap, maxWidth, maxHeight, true
                    )

                    val x = col * maxWidth
                    val y = row * maxHeight
                    canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
                }
            }
        }

        println("Playlist Cover Art Generated Successfully!")
        val stream = ByteArrayOutputStream()
        combinedBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        return stream.toByteArray()

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/**
 * Loads a bitmap from either a local URI or a remote HTTP/HTTPS URL
 *
 * @param imageUrl The URL or URI string for the image
 * @param context Android context used to access content resolver
 * @return The loaded Bitmap or null if loading failed
 */
private suspend fun loadBitmapFromUrl(imageUrl: String, context: Context): Bitmap? {
    return try {
        when {
            // Handle HTTP/HTTPS URLs
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
                // For web URLs
                withContext(Dispatchers.IO) {
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    try {
                        connection.doInput = true
                        connection.connect()
                        val inputStream = connection.inputStream
                        val options = BitmapFactory.Options().apply {
                            outWidth = 64
                            outHeight = 64
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream.close()
                        bitmap
                    } finally {
                        connection.disconnect()
                    }
                }
            }

            // Handle content:// URIs and file:// URIs
            else -> {
                val uri = imageUrl.toUri()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.setTargetSize(64, 64)
                        // Disable hardware acceleration
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    // For older Android versions
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bitmap
                }
            }
        }
    } catch (e: Exception) {
        println("Failed to load image from $imageUrl: ${e.message}")
        null
    }
}