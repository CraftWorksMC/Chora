package com.craftworks.music.ui.elements.dialogs.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.craftworks.music.R
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import kotlinx.coroutines.launch

@Composable
fun TranscodingBitrateDialog(setShowDialog: (Boolean) -> Unit, isWifiDialog: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = PlaybackSettingsManager(context)

    val currentBitrate by if (isWifiDialog)
        manager.wifiTranscodingBitrateFlow.collectAsState("")
    else
        manager.mobileDataTranscodingBitrateFlow.collectAsState("")

    val list = listOf("1", "96", "128", "192", "256", "320", "No Transcoding")

    GenericListDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.playback_max_bitrate,
        options = list,
        selectedOption = currentBitrate,
        label = {
            if (it != "No Transcoding") "$it Kbps" else "No Transcoding"
        },
        onOptionSelected = { bitrate ->
            scope.launch {
                if (isWifiDialog) manager.setWifiTranscodingBitrate(bitrate)
                else manager.setMobileDataTranscodingBitrate(bitrate)
            }
        }
    )
}

@Composable
fun TranscodingFormatDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = PlaybackSettingsManager(context)
    val currentFormat by manager.transcodingFormatFlow.collectAsState("")

    GenericListDialog(
        setShowDialog = setShowDialog,
        titleRes = R.string.playback_transcoding_format,
        options = listOf("mp3", "aac", "opus"),
        selectedOption = currentFormat,
        label = { it },
        onOptionSelected = { format ->
            scope.launch { manager.setTranscodingFormat(format) }
        }
    )
}