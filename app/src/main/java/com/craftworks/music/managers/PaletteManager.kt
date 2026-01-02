package com.craftworks.music.managers

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.data.database.dao.AlbumPaletteDao
import com.craftworks.music.data.database.entity.AlbumPaletteEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaletteManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumPaletteDao: AlbumPaletteDao
) {
    private val colorCache = LruCache<String, List<Int>>(100)

    suspend fun getPaletteColors(imageUrl: String): List<Int> {
        // 1. Check Memory Cache
        colorCache.get(imageUrl)?.let { return it }

        // 2. Check Database
        val dbEntity = albumPaletteDao.getPalette(imageUrl)
        if (dbEntity != null) {
            val colors = try {
                dbEntity.colors.split(",").map { it.toInt() }
            } catch (e: Exception) {
                emptyList()
            }
            if (colors.isNotEmpty()) {
                colorCache.put(imageUrl, colors)
                return colors
            }
        }

        // 3. Generate and Cache
        return generateAndCachePalette(imageUrl)
    }

    private suspend fun generateAndCachePalette(imageUrl: String): List<Int> = withContext(Dispatchers.IO) {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .size(256) // Use reasonable size for palette generation
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        val bitmap = result?.toBitmap()

        val colors = bitmap?.let { bitmapImage ->
            val palette = Palette.Builder(bitmapImage).generate()
            listOfNotNull(
                palette.mutedSwatch?.rgb,
                palette.darkVibrantSwatch?.rgb,
                palette.lightVibrantSwatch?.rgb,
                palette.vibrantSwatch?.rgb,
                palette.dominantSwatch?.rgb,
                palette.lightMutedSwatch?.rgb
            )
        } ?: emptyList()

        if (colors.isNotEmpty()) {
            // Save to Memory Cache
            colorCache.put(imageUrl, colors)

            // Save to Database
            albumPaletteDao.insert(
                AlbumPaletteEntity(
                    imageUrl = imageUrl,
                    colors = colors.joinToString(",")
                )
            )
        }

        colors
    }
}
