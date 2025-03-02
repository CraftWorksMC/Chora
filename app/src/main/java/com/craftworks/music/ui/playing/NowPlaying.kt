package com.craftworks.music.ui.playing

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.collections.elementAtOrNull

var lyricsOpen by mutableStateOf(false)

@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_TELEVISION,
    device = "id:tv_1080p"
)
@Composable
fun NowPlayingContent(
    context: Context = LocalContext.current,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = rememberManagedMediaController().value
) {
    val backgroundStyle by SettingsManager(context).npBackgroundFlow.collectAsState("Static Blur")
    var backgroundDarkMode by remember { mutableStateOf(false) }

    if (backgroundStyle == "Plain")
        backgroundDarkMode = isSystemInDarkTheme()

    var colors by remember(SongHelper.currentSong.imageUrl) {
        mutableStateOf<List<Color>>(listOf(Color.Gray, Color.White))
    }

    LaunchedEffect(SongHelper.currentSong.imageUrl) {
        if (SongHelper.currentSong.imageUrl.isBlank() || backgroundStyle == "Plain" ) return@LaunchedEffect
        colors = withContext(Dispatchers.IO) {
            async {
                extractColorsFromUri(SongHelper.currentSong.imageUrl, context)
            }.await().toMutableList()
        }

        println("Generated new colors!")
        backgroundDarkMode = (colors.elementAtOrNull(2) ?: Color.Black).customLuminance() <= 0.6f
    }

    val iconTextColor = remember(backgroundDarkMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (backgroundDarkMode) dynamicDarkColorScheme(context).onBackground
            else dynamicLightColorScheme(context).onBackground
        } else {
            if (backgroundDarkMode) Color.White
            else Color.Black
        }
    }

    NowPlaying_Background(colors ,mediaController)

    // Apply a back or white overlay to the background for improved contrast on the animated bg
    if (backgroundStyle == "Animated Blur")
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (backgroundDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.2f)
            )
        )

    if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) ||
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE){
        NowPlayingLandscape(mediaController, navHostController)
    }
    else {
        NowPlayingPortrait(mediaController, navHostController, iconTextColor)
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