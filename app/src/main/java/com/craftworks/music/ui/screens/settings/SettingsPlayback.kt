package com.craftworks.music.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .dialogFocusable()
        //.background(MaterialTheme.colorScheme.background)
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

        Column(Modifier.padding(12.dp, 12.dp, 24.dp, 12.dp)) {

            // Transcoding

            val transcodingBitrateWifi =
                SettingsManager(context).wifiTranscodingBitrateFlow.collectAsState("").value

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        showWifiTranscodingDialog = true
                    }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Setting_Transcoding_Wifi),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = if (transcodingBitrateWifi != "No Transcoding") "$transcodingBitrateWifi Kbps" else transcodingBitrateWifi,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            val transcodingBitrateData =
                SettingsManager(context).mobileDataTranscodingBitrateFlow.collectAsState("").value

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        showDataTranscodingDialog = true
                    }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Setting_Transcoding_Data),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = if (transcodingBitrateData != "No Transcoding") "$transcodingBitrateData Kbps" else transcodingBitrateData,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            val transcodingFormat =
                SettingsManager(context).transcodingFormatFlow.collectAsState("opus").value

            val transcodingFormatEnabled =
                transcodingBitrateData != "No Transcoding" || transcodingBitrateWifi != "No Transcoding"

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        enabled = transcodingFormatEnabled
                    ) {
                        showTranscodingFormatDialog = true
                    }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                    contentDescription = null,
                    tint = if (transcodingFormatEnabled)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.onBackground.copy(0.5f),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Setting_Transcoding_Format),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = if (transcodingFormatEnabled)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onBackground.copy(0.5f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = transcodingFormat,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (transcodingFormatEnabled)
                            MaterialTheme.colorScheme.onBackground.copy(0.75f)
                        else
                            MaterialTheme.colorScheme.onBackground.copy(0.3f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Scrobble Percent
            val sliderValue = SettingsManager(context).scrobblePercentFlow.collectAsState(7)
            val interactionSource = remember { MutableInteractionSource() }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .indication(interactionSource, LocalIndication.current)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_p_scrobble),
                    contentDescription = "Background Style Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Setting_Scrobble_Percent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Slider(
                        modifier = Modifier
                            .semantics { contentDescription = "Minimum Scrobble Percentage" }
                            .onKeyEvent { keyEvent ->
                                when {
                                    keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyDown -> {
                                        val newValue = (sliderValue.value + 1f).coerceIn(1f, 10f)
                                        println("NewValue $newValue, sliderValue $sliderValue")
                                        runBlocking {
                                            SettingsManager(context).setScrobblePercent(newValue.roundToInt())
                                        }
                                        true
                                    }

                                    keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
                                        val newValue = (sliderValue.value - 1f).coerceIn(1f, 10f)
                                        println("NewValue $newValue, sliderValue $sliderValue")
                                        runBlocking {
                                            SettingsManager(context).setScrobblePercent(newValue.roundToInt())
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            },
                        interactionSource = interactionSource,
                        value = (sliderValue.value).toFloat(),
                        steps = 8,
                        onValueChange = {
                            runBlocking {
                                SettingsManager(context).setScrobblePercent(it.roundToInt())
                            }
                            println("Change scrobble percentage to ${it.roundToInt()}")
                        },
                        valueRange = 1f..10f
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
    }
}