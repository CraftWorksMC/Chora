package com.craftworks.music.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.craftworks.music.data.Screen
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.elements.BottomSpacer
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.elements.dialogs.TranscodingDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import kotlinx.coroutines.runBlocking

@Preview(showSystemUi = false, showBackground = true)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun S_PlaybackScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current

    val leftPadding =
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    var showTranscodingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = leftPadding,
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
                    }, modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back To Settings",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        HorizontalLineWithNavidromeCheck()

        Column(Modifier.padding(12.dp, 12.dp, 24.dp, 12.dp)) {

            // Transcoding

            val transcodingBitrate =
                SettingsManager(context).transcodingBitrateFlow.collectAsState("").value

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        showTranscodingDialog = true
                    }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_p_transcoding),
                    contentDescription = "Background Style Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.Setting_Transcoding),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = if (transcodingBitrate != "No Transcoding") "$transcodingBitrate Kbps" else transcodingBitrate,
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

            // Scrobble Percent
            val sliderValue = SettingsManager(context).scrobblePercentFlow.collectAsState(7)
            val interactionSource = remember { MutableInteractionSource() }

            Row(verticalAlignment = Alignment.CenterVertically,
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
                                            SettingsManager(context).setScrobblePercent(newValue.toInt())
                                        }
                                        true
                                    }
                                    keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
                                        val newValue = (sliderValue.value - 1f).coerceIn(1f, 10f)
                                        println("NewValue $newValue, sliderValue $sliderValue")
                                        runBlocking {
                                            SettingsManager(context).setScrobblePercent(newValue.toInt())
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
                                SettingsManager(context).setScrobblePercent(it.toInt())
                            }
                            println("Change percentage to $it")
                        },
                        valueRange = 1f..10f
                    )
                }
            }

            BottomSpacer()
        }

        if (showTranscodingDialog) TranscodingDialog(setShowDialog = { showTranscodingDialog = it })
    }
}