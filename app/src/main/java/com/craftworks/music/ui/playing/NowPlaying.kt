package com.craftworks.music.ui.playing

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.managers.SettingsManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

var lyricsOpen by mutableStateOf(false)

@Preview
@Composable
fun NowPlayingContent(
    context: Context = LocalContext.current,
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null
) {
    val backgroundStyle by SettingsManager(context).npBackgroundFlow.collectAsStateWithLifecycle("Animated Blur")
    var backgroundDarkMode by remember { mutableStateOf(false) }

    var colors by remember {
        mutableStateOf(listOf<Color>())
    }
    var iconTextColor by remember { mutableStateOf<Color>(Color.White) }

    backgroundDarkMode = isSystemInDarkTheme()

    LaunchedEffect(metadata?.artworkUri) {
        if (metadata?.artworkUri != null) {
            async {
                colors = extractColorsFromUri(metadata.artworkUri.toString(), context)
            }.await()

            backgroundDarkMode =
                (colors.elementAtOrNull(2) ?: Color.Black).customLuminance() <= 0.75f
            println("Generated new colors for song ${metadata.artworkUri}! Luminance: ${colors.elementAtOrNull(2)?.customLuminance()}")

            iconTextColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (backgroundDarkMode) dynamicDarkColorScheme(context).onBackground
                else dynamicLightColorScheme(context).onBackground
            } else {
                if (backgroundDarkMode) Color.White
                else Color.Black
            }
        }
    }

    NowPlaying_Background(colors, backgroundStyle)

    // Apply a back or white overlay to the background for improved contrast on the animated bg
    if (backgroundStyle == "Animated Blur") {
        val animatedOverlayColor by animateColorAsState(
            targetValue = if (backgroundDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.2f),
            animationSpec = tween(durationMillis = 1000),
            label = "Overlay Color Animation"
        )

        Box (
            modifier = Modifier
                .fillMaxSize()
                .background(animatedOverlayColor)
        )
    }

    if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) ||
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        NowPlayingLandscape(mediaController, iconTextColor, metadata)
    } else {
        NowPlayingPortrait(mediaController, iconTextColor, metadata)
    }
}

@Composable
fun dpToPx(dp: Int): Int {
    return with(LocalDensity.current) { dp.dp.toPx() }.toInt()
}

fun Color.customLuminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

suspend fun extractColorsFromUri(uri: String, context: Context): List<Color> = coroutineScope {
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .size(64)
        .data(uri)
        .allowHardware(false) // Disable hardware bitmaps.
        .build()

    val result = (loader.execute(request) as? SuccessResult)?.drawable
    val bitmap = result?.toBitmap()

    bitmap?.let { bitmapImage ->
        val palette = Palette.Builder(bitmapImage).generate()
        listOfNotNull(
            palette.mutedSwatch?.rgb?.let { Color(it) },
            palette.darkVibrantSwatch?.rgb?.let { Color(it) },
            palette.lightVibrantSwatch?.rgb?.let { Color(it) },
            palette.vibrantSwatch?.rgb?.let { Color(it) },
            palette.dominantSwatch?.rgb?.let { Color(it) },
            palette.lightMutedSwatch?.rgb?.let { Color(it) },
        )
    } ?: listOf()
}