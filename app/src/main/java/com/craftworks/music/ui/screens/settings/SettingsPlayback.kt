package com.craftworks.music.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.SleepTimerManager
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import com.craftworks.music.ui.elements.dialogs.SleepTimerDialog
import com.craftworks.music.ui.elements.dialogs.TranscodingBitrateDialog
import com.craftworks.music.ui.elements.dialogs.TranscodingFormatDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import kotlin.math.roundToInt

@Preview(showSystemUi = false, showBackground = true)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun S_PlaybackScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Use single remembered instance to avoid creating multiple SettingsManager instances
    val playbackSettingsManager = remember { PlaybackSettingsManager(context.applicationContext) }

    var showWifiTranscodingDialog by remember { mutableStateOf(false) }
    var showDataTranscodingDialog by remember { mutableStateOf(false) }
    var showTranscodingFormatDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val isTimerActive by SleepTimerManager.isTimerActive.collectAsStateWithLifecycle()
    val remainingTimeMs by SleepTimerManager.remainingTimeMs.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.Settings_Header_Playback)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navHostController.popBackStack() },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { innerPadding ->
        Box (
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding()
                )
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .dialogFocusable()
        ) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Transcoding
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val transcodingBitrateWifi =
                        playbackSettingsManager.wifiTranscodingBitrateFlow.collectAsStateWithLifecycle("").value

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.Setting_Transcoding_Wifi),
                        settingsSubtitle = if (transcodingBitrateWifi != "No Transcoding") "$transcodingBitrateWifi Kbps" else transcodingBitrateWifi,
                        settingsIcon = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                        toggleEvent = { showWifiTranscodingDialog = true }
                    )


                    val transcodingBitrateData =
                        playbackSettingsManager.mobileDataTranscodingBitrateFlow.collectAsStateWithLifecycle(
                            ""
                        ).value

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.Setting_Transcoding_Data),
                        settingsSubtitle = if (transcodingBitrateData != "No Transcoding") "$transcodingBitrateData Kbps" else transcodingBitrateData,
                        settingsIcon = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                        toggleEvent = { showDataTranscodingDialog = true }
                    )

                    val transcodingFormat =
                        playbackSettingsManager.transcodingFormatFlow.collectAsStateWithLifecycle("opus").value

                    val transcodingFormatEnabled =
                        transcodingBitrateData != "No Transcoding" || transcodingBitrateWifi != "No Transcoding"

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.Setting_Transcoding_Format),
                        settingsSubtitle = transcodingFormat,
                        settingsIcon = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                        enabled = transcodingFormatEnabled,
                        toggleEvent = { showTranscodingFormatDialog = true }
                    )
                }

                // Scrobble Percent
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val sliderValue =
                        playbackSettingsManager.scrobblePercentFlow.collectAsStateWithLifecycle(7)

                    SettingsSlider(
                        settingsName = stringResource(R.string.Setting_Scrobble_Percent),
                        value = sliderValue.value.toFloat(),
                        steps = 8,
                        minValue = 1f, maxValue = 10f,
                        onValueChange = {
                            coroutineScope.launch {
                                playbackSettingsManager.setScrobblePercent(it.roundToInt())
                            }
                        }
                    )
                }

                // Sleep Timer
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val timerSubtitle = if (isTimerActive) {
                        SleepTimerManager.formatRemainingTime(remainingTimeMs)
                    } else {
                        stringResource(R.string.Setting_Sleep_Timer_Off)
                    }

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.Setting_Sleep_Timer),
                        settingsSubtitle = timerSubtitle,
                        settingsIcon = ImageVector.vectorResource(R.drawable.s_p_sleep_timer),
                        toggleEvent = { showSleepTimerDialog = true }
                    )
                }

                // Queue Position
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val queueAddToBottom by playbackSettingsManager.queueAddToBottomFlow.collectAsStateWithLifecycle(true)

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.Setting_Queue_Position),
                        settingsSubtitle = if (queueAddToBottom)
                            stringResource(R.string.Setting_Queue_Bottom)
                        else
                            stringResource(R.string.Setting_Queue_Top),
                        settingsIcon = ImageVector.vectorResource(R.drawable.rounded_queue_music_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                playbackSettingsManager.setQueueAddToBottom(!queueAddToBottom)
                            }
                        }
                    )
                }
            }
        }

        if (showWifiTranscodingDialog) TranscodingBitrateDialog(setShowDialog = {
            showWifiTranscodingDialog = it
        }, true)
        if (showDataTranscodingDialog) TranscodingBitrateDialog(setShowDialog = {
            showDataTranscodingDialog = it
        }, false)
        if (showTranscodingFormatDialog) TranscodingFormatDialog(setShowDialog = {
            showTranscodingFormatDialog = it
        })
        if (showSleepTimerDialog) SleepTimerDialog(
            setShowDialog = { showSleepTimerDialog = it },
            onDurationSelected = { duration ->
                SleepTimerManager.startTimer(duration)
            }
        )
    }
}