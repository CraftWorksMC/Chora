package com.craftworks.music.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.craftworks.music.player.SongHelper.Companion.minPercentageScrobble
import com.craftworks.music.data.Screen
import com.craftworks.music.ui.elements.BottomSpacer
import com.craftworks.music.ui.elements.dialogs.TranscodingDialog
import com.craftworks.music.ui.elements.dialogs.transcodingBitrate

@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_PlaybackScreen(navHostController: NavHostController = rememberNavController()) {
    val leftPadding =
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    var showTranscodingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = leftPadding,
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .background(MaterialTheme.colorScheme.background)
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
                IconButton(onClick = { navHostController.navigate(Screen.Setting.route) {
                    launchSingleTop = true
                } },
                    modifier = Modifier
                        .size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back To Settings",
                        modifier = Modifier.size(32.dp))
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(Modifier.padding(12.dp,12.dp,24.dp,12.dp)){

            // Transcoding
            Row (verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
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
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = if (transcodingBitrate.value != "No Transcoding") "${transcodingBitrate.value} Kbps" else transcodingBitrate.value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Scrobble Percent
            Row (verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable {
                        //showBackgroundDialog = true
                    }) {
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
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Slider(
                        modifier = Modifier.semantics { contentDescription = "Minimum Scrobble Percentage" },
                        value = (minPercentageScrobble.intValue / 10).toFloat(),
                        onValueChange = { minPercentageScrobble.intValue = it.toInt() * 10
                                        println("Change percentage to ${it.toInt()}")},
                        valueRange = 0f..10f
                    )
                }
            }

            BottomSpacer()
        }

        if(showTranscodingDialog)
            TranscodingDialog(setShowDialog = { showTranscodingDialog = it })
    }
}