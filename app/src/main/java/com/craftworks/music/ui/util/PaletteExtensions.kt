package com.craftworks.music.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.craftworks.music.ui.playing.PaletteManagerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberAlbumPalette(imageUrl: String?): State<List<Color>?> {
    val context = LocalContext.current
    return produceState<List<Color>?>(initialValue = null, key1 = imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            value = null
            return@produceState
        }
        
        val paletteManager = EntryPointAccessors.fromApplication(
            context, 
            PaletteManagerEntryPoint::class.java
        ).paletteManager()
        
        try {
            val ints = withContext(Dispatchers.IO) {
                paletteManager.getPaletteColors(imageUrl)
            }
            if (ints.isNotEmpty()) {
                value = ints.map { Color(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            value = null
        }
    }
}
