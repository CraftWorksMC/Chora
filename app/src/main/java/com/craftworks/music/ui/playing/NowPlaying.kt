package com.craftworks.music.ui.playing

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
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
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.craftworks.music.managers.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

var lyricsOpen by mutableStateOf(false)
var playQueueOpen by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun NowPlayingContent(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val backgroundStyle by settingsManager.npBackgroundFlow.collectAsStateWithLifecycle("Animated Blur")
    var backgroundDarkMode by remember { mutableStateOf(false) }

    var colors by remember {
        mutableStateOf(listOf<Color>())
    }
    var iconTextColor by remember { mutableStateOf<Color>(Color.White) }

    backgroundDarkMode = isSystemInDarkTheme()

    LaunchedEffect(metadata?.artworkUri) {
        if (metadata?.artworkUri != null && backgroundStyle != "Plain") {
            colors = extractColorsFromUri(metadata.artworkUri.toString(), context)

            backgroundDarkMode =
                (colors.elementAtOrNull(2) ?: Color.Black).customLuminance() <= 0.75f
            println("Generated new colors for song ${metadata.artworkUri}! Luminance: ${colors.elementAtOrNull(2)?.customLuminance()}")
        }
        iconTextColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (backgroundDarkMode) dynamicDarkColorScheme(context).onBackground
            else dynamicLightColorScheme(context).onBackground
        } else {
            if (backgroundDarkMode) Color.White
            else Color.Black
        }
    }

    val targetOverlayColor = if (backgroundStyle == "Animated Blur") {
        if (backgroundDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.2f)
    } else {
        Color.Transparent
    }
    NowPlaying_Background(colors, backgroundStyle, targetOverlayColor)

    if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) ||
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        NowPlayingLandscape(mediaController, iconTextColor, metadata)
    } else {
        NowPlayingPortrait(mediaController, iconTextColor, metadata)
    }

    // Modal Bottom Sheet for Play Queue
    val playQueueSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Makes it fully expand or hide, no half-state
    )
    if (playQueueOpen) {
        ModalBottomSheet(
            onDismissRequest = { playQueueOpen = false },
            sheetState = playQueueSheetState,
            // You can customize containerColor, contentColor, scrimColor etc.
            // scrimColor = Color.Black.copy(alpha = 0.6f) // Example for scrim
        ) {
            // Content of your Bottom Sheet
            PlayQueueContent(mediaController = mediaController)
            // Add a button inside the sheet to close it, or rely on swipe down/back press
            /*Button(onClick = {
                scope.launch { playQueueSheetState.hide() }.invokeOnCompletion {
                    if (!playQueueSheetState.isVisible) {
                        showPlayQueueSheet = false
                    }
                }
            }) {
                Text("Close Queue")
            }*/
            Spacer(modifier = Modifier.height(16.dp)) // Some padding at the bottom of the sheet
        }
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
    val loader = context.imageLoader
    val request = ImageRequest.Builder(context)
        .data(uri.replace("size=128", "size=16"))
        .allowHardware(false)
        .size(16)
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()

    val result = (loader.execute(request) as? SuccessResult)?.drawable
    val bitmap = result?.toBitmap()

    bitmap?.let { bitmapImage ->
        withContext(Dispatchers.Default) {
            val palette = Palette.Builder(bitmapImage).generate()
            listOfNotNull(
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