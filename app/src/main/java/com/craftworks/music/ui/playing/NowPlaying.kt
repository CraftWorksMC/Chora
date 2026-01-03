package com.craftworks.music.ui.playing

import android.app.Activity
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.util.LayoutMode
import com.craftworks.music.ui.util.rememberFoldableState
import kotlinx.coroutines.coroutineScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.craftworks.music.managers.PaletteManager

// Top-level UI state for NowPlaying panels
// Note: These are intentionally top-level singletons because they need to persist
// across configuration changes (fold/unfold) but within the same navigation context.
// They are reset when the user navigates away from NowPlaying.
private val _lyricsOpen = mutableStateOf(false)
private val _playQueueOpen = mutableStateOf(false)

var lyricsOpen: Boolean
    get() = _lyricsOpen.value
    set(value) { _lyricsOpen.value = value }

var playQueueOpen: Boolean
    get() = _playQueueOpen.value
    set(value) { _playQueueOpen.value = value }

// Call this when leaving NowPlaying to reset state
fun resetNowPlayingPanelState() {
    _lyricsOpen.value = false
    _playQueueOpen.value = false
}

enum class NowPlayingTitleAlignment {
    LEFT, CENTER, RIGHT
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PaletteManagerEntryPoint {
    fun paletteManager(): PaletteManager
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun NowPlayingContent(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settingsManager = remember { AppearanceSettingsManager(context) }
    val backgroundStyle by settingsManager.npBackgroundFlow.collectAsStateWithLifecycle(NowPlayingBackground.SIMPLE_ANIMATED_BLUR)
    var backgroundDarkMode by remember { mutableStateOf(false) }

    var colors by remember {
        mutableStateOf(listOf<Color>())
    }
    var iconTextColor by remember { mutableStateOf<Color>(Color.White) }

    // Initialize backgroundDarkMode with system theme only once
    val systemDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        backgroundDarkMode = systemDarkTheme
    }

    // Update system bar icons based on the calculated background darkness
    if (!view.isInEditMode) {
        DisposableEffect(backgroundDarkMode) {
            val window = (view.context as Activity).window
            // isAppearanceLightStatusBars = true means dark icons (for light background)
            // isAppearanceLightStatusBars = false means light icons (for dark background)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !backgroundDarkMode

            onDispose {
                // Restore to match system theme when leaving this screen
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !systemDarkTheme
            }
        }
    }

    LaunchedEffect(metadata?.artworkUri) {
        if (metadata?.artworkUri != null && backgroundStyle != NowPlayingBackground.PLAIN) {
            colors = extractColorsFromUri(metadata.artworkUri.toString(), context)

            // Fix: Use 0.5f threshold for better contrast (0.8f was too high, causing white text on light grey)
            backgroundDarkMode =
                (colors.elementAtOrNull(2) ?: Color.Black).customLuminance() <= 0.5f
        }
        iconTextColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (backgroundDarkMode) dynamicDarkColorScheme(context).onBackground
            else dynamicLightColorScheme(context).onBackground
        } else {
            if (backgroundDarkMode) Color.White
            else Color.Black
        }
    }

    val targetOverlayColor = if (backgroundStyle == NowPlayingBackground.ANIMATED_BLUR || backgroundStyle == NowPlayingBackground.SIMPLE_ANIMATED_BLUR) {
        if (backgroundDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.2f)
    } else {
        Color.Transparent
    }

    NowPlaying_Background(colors, backgroundStyle, targetOverlayColor)

    val foldableState = rememberFoldableState()
    val configuration = LocalConfiguration.current
    val isTV = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Treat wide screens (>580dp) as needing landscape layout - covers unfolded foldables
    val isWideScreen = configuration.screenWidthDp > 580

    // Use landscape for TV, actual landscape orientation, table-top mode, unfolded devices, or wide screens
    val useLandscapeLayout = isTV || isLandscape || isWideScreen ||
        foldableState.layoutMode == LayoutMode.TABLE_TOP ||
        foldableState.layoutMode == LayoutMode.EXPANDED ||
        foldableState.layoutMode == LayoutMode.BOOK_MODE

    // Debug log
    android.util.Log.d("NowPlaying", "useLandscapeLayout=$useLandscapeLayout, isWideScreen=$isWideScreen, screenHeight=${configuration.screenHeightDp}")

    // Reset lyrics state when layout mode changes to prevent stale UI state
    LaunchedEffect(useLandscapeLayout, foldableState.layoutMode) {
        // Close lyrics when transitioning between layouts to avoid visual glitches
        if (lyricsOpen) {
            lyricsOpen = false
        }
    }

    if (useLandscapeLayout) {
        NowPlayingLandscape(mediaController, iconTextColor, metadata)
    } else {
        NowPlayingPortrait(mediaController, iconTextColor, metadata)
    }

    // Play Queue
    val playQueueSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    if (playQueueOpen) {
        ModalBottomSheet(
            onDismissRequest = { playQueueOpen = false },
            sheetState = playQueueSheetState,
        ) {
            PlayQueueContent(mediaController = mediaController)
            Spacer(modifier = Modifier.height(16.dp))
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
    val paletteManager = EntryPointAccessors.fromApplication(context, PaletteManagerEntryPoint::class.java).paletteManager()
    paletteManager.getPaletteColors(uri).map { Color(it) }
}
