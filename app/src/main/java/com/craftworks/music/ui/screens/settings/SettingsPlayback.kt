package com.craftworks.music.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.elements.dialogs.TranscodingBitrateDialog
import com.craftworks.music.ui.elements.dialogs.TranscodingFormatDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

@Preview(showSystemUi = false, showBackground = true)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun S_PlaybackScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current

    var showWifiTranscodingDialog by remember { mutableStateOf(false) }
    var showDataTranscodingDialog by remember { mutableStateOf(false) }

    var showTranscodingFormatDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .dialogFocusable()
    ) {
        /* HEADER */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.s_m_playback),
                contentDescription = "Settings Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.Settings_Header_Playback),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(
                    onClick = {
                        navHostController.navigate(Screen.Setting.route) {
                            launchSingleTop = true
                        }
                    }, modifier = Modifier.size(56.dp, 70.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back To Settings",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Transcoding
            Column (
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val transcodingBitrateWifi =
                    SettingsManager(context).wifiTranscodingBitrateFlow.collectAsState("").value

                SettingsDialogButton(
                    settingsName = stringResource(R.string.Setting_Transcoding_Wifi),
                    settingsSubtitle = if (transcodingBitrateWifi != "No Transcoding") "$transcodingBitrateWifi Kbps" else transcodingBitrateWifi,
                    settingsIcon = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                    toggleEvent = { showWifiTranscodingDialog = true }
                )


                val transcodingBitrateData =
                    SettingsManager(context).mobileDataTranscodingBitrateFlow.collectAsState("").value

                SettingsDialogButton(
                    settingsName = stringResource(R.string.Setting_Transcoding_Data),
                    settingsSubtitle = if (transcodingBitrateData != "No Transcoding") "$transcodingBitrateData Kbps" else transcodingBitrateData,
                    settingsIcon = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                    toggleEvent = { showDataTranscodingDialog = true }
                )

                val transcodingFormat =
                    SettingsManager(context).transcodingFormatFlow.collectAsState("opus").value

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
            Column (
                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val sliderValue = SettingsManager(context).scrobblePercentFlow.collectAsState(7)

                SettingsSlider(
                    settingsName = stringResource(R.string.Setting_Scrobble_Percent),
                    value = sliderValue.value.toFloat(),
                    steps = 8,
                    minValue = 1f, maxValue = 10f,
                    onValueChange = {
                        runBlocking {
                            SettingsManager(context).setScrobblePercent(it.roundToInt())
                        }
                    }
                )
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
    }
}