package com.craftworks.music.ui.playing

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.ChoraMediaLibraryService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class NowPlayingViewModel @Inject constructor (
    @ApplicationContext private val context: Context,
    appearanceSettingsManager: AppearanceSettingsManager
) : ViewModel() {
    private val _lyricsOpen = MutableStateFlow(false)
    val lyricsOpen = _lyricsOpen.asStateFlow()

    private val _playQueueOpen = MutableStateFlow(false)
    val playQueueOpen = _playQueueOpen.asStateFlow()

    private val _detailsOpen = MutableStateFlow(false)
    val detailsOpen = _detailsOpen.asStateFlow()

    private val _sleepTimerDialogOpen = MutableStateFlow(false)
    val sleepTimerDialogOpen = _sleepTimerDialogOpen.asStateFlow()

    fun setLyricsOpen(open: Boolean) { _lyricsOpen.value = open }
    fun setPlayQueueOpen(open: Boolean) { _playQueueOpen.value = open }
    fun setDetailsOpen(open: Boolean) { _detailsOpen.value = open }
    fun setSleepTimerDialogOpen(open: Boolean) { _sleepTimerDialogOpen.value = open }

    val sleepTimer = MutableStateFlow(0).asStateFlow()

    val backgroundStyle = appearanceSettingsManager.npBackgroundFlow

    private val _paletteColors = MutableStateFlow<List<Color>>(emptyList())
    val paletteColors = _paletteColors.asStateFlow()

    private val _iconTextColor = MutableStateFlow<Color>(Color.White)
    val iconTextColor = _iconTextColor.asStateFlow()

    private val _isBackgroundDark = MutableStateFlow(false)
    val isBackgroundDark = _isBackgroundDark.asStateFlow()

    private val _meta = MutableStateFlow(MediaItem.EMPTY)
    val metadata = _meta.asStateFlow()

    fun updatePaletteFromUri(uri: Uri?, currentBackgroundStyle: NowPlayingBackground) {
        if (uri == null || currentBackgroundStyle == NowPlayingBackground.PLAIN) return

        viewModelScope.launch {
            val palette = extractColorsFromUri(uri.toString(), context)
            val extractedColors = palette.filterNotNull()
            _paletteColors.value = extractedColors

            val referenceColor = palette.elementAtOrNull(2) ?: palette.elementAtOrNull(4) ?: Color.Black
            _isBackgroundDark.value = referenceColor.customLuminance() <= 0.8f

            _iconTextColor.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (_isBackgroundDark.value) dynamicDarkColorScheme(context).onBackground
                else dynamicLightColorScheme(context).onBackground
            } else {
                if (_isBackgroundDark.value) Color.White
                else Color.Black
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun setSleepTimer(minutes: Int) {
        ChoraMediaLibraryService.getInstance()?.setSleepTimer(minutes)
    }

    private fun Color.customLuminance(): Float {
        return 0.2126f * red + 0.7152f * green + 0.0722f * blue
    }

    private suspend fun extractColorsFromUri(uri: String, context: Context): List<Color?> = coroutineScope {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(uri.replace("size=128", "size=32"))
            .allowHardware(false)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        val bitmap = result?.toBitmap()

        bitmap?.let { bitmapImage ->
            withContext(Dispatchers.Default) {
                val palette = Palette.Builder(bitmapImage).generate()

                val swatches = mapOf(
                    "Muted" to palette.mutedSwatch,
                    "Dark Vibrant" to palette.darkVibrantSwatch,
                    "Light Vibrant" to palette.lightVibrantSwatch,
                    "Vibrant" to palette.vibrantSwatch,
                    "Dominant" to palette.dominantSwatch,
                    "Light Muted" to palette.lightMutedSwatch
                )

                // Log the results
                swatches.forEach { (name, swatch) ->
                    swatch?.let {
                        val hex = Integer.toHexString(it.rgb).uppercase()
                        Log.d("PaletteColor", "$name: #$hex | Body Text Color: ${Integer.toHexString(it.bodyTextColor)}")
                    }
                }

                listOf(
                    palette.mutedSwatch?.rgb?.let { Color(it) },
                    palette.darkVibrantSwatch?.rgb?.let { Color(it) },
                    palette.lightVibrantSwatch?.rgb?.let { Color(it) },
                    palette.vibrantSwatch?.rgb?.let { Color(it) },
                    palette.dominantSwatch?.rgb?.let { Color(it) },
                    palette.lightMutedSwatch?.rgb?.let { Color(it) },
                )
            }
        } ?: listOf()
    }
}