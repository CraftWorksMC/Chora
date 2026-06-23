package com.craftworks.music.ui.playing

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.ui.playing.tv.TvNowPlaying

enum class NowPlayingAlignment {
    LEFT, CENTER, RIGHT
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun NowPlayingContent(
    mediaController: MediaController? = null,
    metadata: MediaMetadata? = null,
    viewModel: NowPlayingViewModel = viewModel(),
) {
    val backgroundStyle by viewModel.backgroundStyle.collectAsStateWithLifecycle(NowPlayingBackground.STATIC_BLUR)
    val backgroundDarkMode by viewModel.isBackgroundDark.collectAsStateWithLifecycle()
    val lyricsOpen by viewModel.lyricsOpen.collectAsStateWithLifecycle()
    val playQueueOpen by viewModel.playQueueOpen.collectAsStateWithLifecycle()
    val detailsOpen by viewModel.detailsOpen.collectAsStateWithLifecycle()
    val sleepTimerOpen by viewModel.sleepTimerDialogOpen.collectAsStateWithLifecycle()
    val sleepTimerMinutes by ChoraMediaLibraryService.getInstance()?.sleepTimerRemainingTime
        ?.collectAsStateWithLifecycle(initialValue = 0)
        ?: remember { mutableIntStateOf(0) }
    val colors by viewModel.paletteColors.collectAsStateWithLifecycle()
    val iconTextColor by viewModel.iconTextColor.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    LaunchedEffect(metadata?.artworkUri, backgroundStyle) {
        viewModel.updatePaletteFromUri(metadata?.artworkUri, backgroundStyle, isSystemDark)
    }

    val targetOverlayColor = if (backgroundStyle == NowPlayingBackground.ANIMATED_BLUR) {
        if (backgroundDarkMode) Color.Black.copy(0.2f) else Color.White.copy(0.2f)
    } else {
        Color.Transparent
    }

    NowPlaying_Background(colors, backgroundStyle, targetOverlayColor)

    if (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) {
        TvNowPlaying(
            mediaController,
            iconTextColor,
            metadata
        )
    } else if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        NowPlayingLandscape(
            mediaController = mediaController,
            metadata = metadata,
            iconColor = iconTextColor,
            sleepTimerMinutes = sleepTimerMinutes,
            onOpenSleepTimer = { viewModel.setSleepTimerDialogOpen(true) },
            onToggleQueue = { viewModel.setPlayQueueOpen(!playQueueOpen) },
        )
    } else {
        NowPlayingPortrait(
            mediaController = mediaController,
            metadata = metadata,
            iconColor = iconTextColor,
            lyricsOpen = lyricsOpen,
            sleepTimerMinutes = sleepTimerMinutes,
            onToggleLyrics = { viewModel.setLyricsOpen(!lyricsOpen) },
            onToggleQueue = { viewModel.setPlayQueueOpen(!playQueueOpen) },
            onToggleDetails = { viewModel.setDetailsOpen(!detailsOpen) },
            onOpenSleepTimer = { viewModel.setSleepTimerDialogOpen(true) },
            onRefreshLyrics =  { viewModel.refreshLyrics(metadata) }
        )
    }


    val playQueueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timePickerState = rememberTimePickerState(initialHour = 0, initialMinute = 0, is24Hour = true)

    if (playQueueOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setPlayQueueOpen(false) },
            sheetState = playQueueSheetState,
        ) {
            PlayQueueContent(mediaController = mediaController)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (detailsOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setDetailsOpen(false) },
            sheetState = playQueueSheetState,
        ) {
            NowPlayingDetails(mediaController = mediaController)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (sleepTimerOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setSleepTimerDialogOpen(false) },
            title = { Text(stringResource(R.string.Dialog_SetSleepTimer)) },
            text = {
                TimePicker(
                    state = timePickerState,
                )
            },
            confirmButton = {
                Button (
                    onClick = {
                        ChoraMediaLibraryService.getInstance()?.setSleepTimer(timePickerState.hour * 60 + timePickerState.minute)
                        viewModel.setSleepTimerDialogOpen(false)
                    }
                ) {
                    Text(stringResource(R.string.Action_Done))
                }
            }
        )
    }
}

@Composable
fun dpToPx(dp: Int): Int {
    return with(LocalDensity.current) { dp.dp.toPx() }.toInt()
}