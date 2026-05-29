package com.craftworks.music.ui.screens.tv.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.dialogs.tv.BackgroundDialog
import com.craftworks.music.ui.elements.dialogs.tv.HomeItemsDialog
import com.craftworks.music.ui.elements.dialogs.tv.NameDialog
import com.craftworks.music.ui.elements.dialogs.tv.NavbarItemsDialog
import com.craftworks.music.ui.elements.dialogs.tv.NowPlayingAlignmentDialog
import com.craftworks.music.ui.elements.dialogs.tv.ThemeDialog
import com.craftworks.music.ui.playing.NowPlayingAlignment
import com.craftworks.music.ui.playing.NowPlayingBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview(device = "id:tv_1080p", showSystemUi = true, showBackground = true)
fun TvS_AppearanceScreen() {
    var showNameDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showNavbarItemsDialog by remember { mutableStateOf(false) }
    var showHomeItemsDialog by remember { mutableStateOf(false) }
    var showNowPlayingLyricsAlignmentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val nowPlayingTitleAlignment by AppearanceSettingsManager(context).nowPlayingLyricsAlignment.collectAsState(
        NowPlayingAlignment.LEFT
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp)
    ) {
        // Username, Theme, Background, Navbar Items, Home Items, Title Alignment
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                val username by AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")

                SettingsButtonItem(
                    title = stringResource(R.string.Setting_Username),
                    subtitle = username,
                    icon = ImageVector.vectorResource(R.drawable.s_a_username),
                    onClick = { showNameDialog = true }
                )

                // Theme
                val selectedTheme by AppearanceSettingsManager(context).appTheme.collectAsState(
                    AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name
                )
                val themes = listOf(
                    AppearanceSettingsManager.Companion.AppTheme.DARK.name,
                    AppearanceSettingsManager.Companion.AppTheme.LIGHT.name,
                    AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name
                )
                val themeStrings = mapOf(
                    AppearanceSettingsManager.Companion.AppTheme.DARK.name to R.string.Theme_Dark,
                    AppearanceSettingsManager.Companion.AppTheme.LIGHT.name to R.string.Theme_Light,
                    AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name to R.string.Theme_System
                )

                SettingsButtonItem(
                    title = stringResource(R.string.Dialog_Theme),
                    subtitle = stringResource(themeStrings[selectedTheme] ?: R.string.Theme_System),
                    icon = ImageVector.vectorResource(R.drawable.s_a_palette),
                    onClick = { showThemesDialog = true }
                )

                // Background Style
                val backgroundType by AppearanceSettingsManager(context).npBackgroundFlow.collectAsState(
                    NowPlayingBackground.STATIC_BLUR
                )
                val backgroundLabels = mapOf(
                    NowPlayingBackground.PLAIN to R.string.Background_Plain,
                    NowPlayingBackground.STATIC_BLUR to R.string.Background_Blur,
                    NowPlayingBackground.ANIMATED_BLUR to R.string.Background_Anim,
                )

                SettingsButtonItem(
                    title = stringResource(R.string.Setting_Background),
                    subtitle = stringResource(
                        backgroundLabels[backgroundType] ?: R.string.Background_Plain
                    ),
                    icon = ImageVector.vectorResource(R.drawable.s_a_background),
                    onClick = { showBackgroundDialog = true }
                )

                val enabledNavbarItems by AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(
                    emptyList()
                )

                SettingsButtonItem(
                    title = stringResource(R.string.Setting_Navbar_Items),
                    subtitle = enabledNavbarItems.filter { it.enabled }
                        .joinToString(", ") { it.title },
                    icon = ImageVector.vectorResource(R.drawable.s_a_navbar_items),
                    onClick = { showNavbarItemsDialog = true }
                )


                // Home Items
                val titleMap = mapOf(
                    "recently_played" to R.string.recently_played,
                    "recently_added" to R.string.recently_added,
                    "most_played" to R.string.most_played
                )
                val enabledHomeItems by AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(
                    emptyList()
                )

                SettingsButtonItem(
                    title = stringResource(R.string.Setting_Home_Items),
                    subtitle = enabledHomeItems.filter { it.enabled }
                        .map { stringResource(titleMap[it.key] ?: R.string.recently_played) }
                        .joinToString(","),
                    icon = ImageVector.vectorResource(R.drawable.s_a_home_items),
                    onClick = { showHomeItemsDialog = true }
                )

                // Now Playing Lyrics Alignment
                val alignmentLabels = mapOf(
                    NowPlayingAlignment.LEFT to R.string.NowPlayingTitleAlignment_Left,
                    NowPlayingAlignment.CENTER to R.string.NowPlayingTitleAlignment_Center,
                    NowPlayingAlignment.RIGHT to R.string.NowPlayingTitleAlignment_Right
                )

                SettingsButtonItem(
                    title = stringResource(R.string.Setting_NowPlayingLyricsAlignment),
                    subtitle = stringResource(
                        alignmentLabels[nowPlayingTitleAlignment]
                            ?: R.string.NowPlayingTitleAlignment_Left
                    ),
                    icon = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                    onClick = { showNowPlayingLyricsAlignmentDialog = true }
                )
            }
        }

        // Switches
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Lyrics blur Info
                val nowPlayingLyricsBlur by AppearanceSettingsManager(context).nowPlayingLyricsBlurFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.Setting_NowPlayingLyricsBlur),
                    icon = ImageVector.vectorResource(R.drawable.outline_line_weight_24),
                    checked = nowPlayingLyricsBlur,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setNowPlayingLyricsBlur(it)
                        }
                    },
                    enabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
                )

                // More Song Info
                val showMoreInfo by AppearanceSettingsManager(context).showMoreInfoFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.Setting_MoreInfo),
                    icon = ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                    checked = showMoreInfo,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setShowMoreInfo(it)
                        }
                    }
                )

                // Show Navidrome Logo
                val showNavidromeLogo by AppearanceSettingsManager(context).showNavidromeLogoFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.Setting_NavidromeLogo),
                    icon = ImageVector.vectorResource(R.drawable.s_m_navidrome_bw),
                    checked = showNavidromeLogo,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setShowNavidromeLogo(it)
                        }
                    }
                )

                // Show Provider Dividers
                val showProviderDividers by AppearanceSettingsManager(context).showProviderDividersFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.Setting_ProviderDividers),
                    icon = ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                    checked = showProviderDividers,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setShowProviderDividers(it)
                        }
                    }
                )

                // Refresh Ripple
                /*
                val refreshRipple by AppearanceSettingsManager(context).refreshAnimationFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.Setting_RefreshAnimation),
                    icon = ImageVector.vectorResource(R.drawable.placeholder),
                    checked = refreshRipple,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setUseRefreshAnimation(it)
                        }
                    },
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                )
                */

                // Track numbers in album view
                val showTrackNumbers by AppearanceSettingsManager(context).showTrackNumbersFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.Setting_TrackNumbersAlbum),
                    icon = ImageVector.vectorResource(R.drawable.rounded_format_list_numbered_24),
                    checked = showTrackNumbers,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setShowTrackNumbers(it)
                        }
                    }
                )
            }
        }

        // Lyrics Animation Speed Slider
        item {
            val lyricsAnimationSpeed by AppearanceSettingsManager(context).lyricsAnimationSpeedFlow.collectAsState(
                1200
            )
            val sliderValue = 2400f - lyricsAnimationSpeed.toFloat() + 600f
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .padding(vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.Setting_LyricsAnimationSpeed),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { uiValue ->
                            val real = (2400f - (uiValue - 600f)).coerceIn(600f, 2400f)
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setLyricsAnimationSpeed(real.toInt())
                            }
                        },
                        valueRange = 600f..2400f,
                        steps = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { keyEvent ->
                                when (keyEvent.key) {
                                    Key.DirectionRight -> {
                                        coroutineScope.launch {
                                            AppearanceSettingsManager(context)
                                                .setLyricsAnimationSpeed(
                                                    (lyricsAnimationSpeed - 300).coerceAtLeast(
                                                        600
                                                    )
                                                )
                                        }
                                        true
                                    }

                                    Key.DirectionLeft -> {
                                        coroutineScope.launch {
                                            AppearanceSettingsManager(context)
                                                .setLyricsAnimationSpeed(
                                                    (lyricsAnimationSpeed + 300).coerceAtMost(
                                                        2400
                                                    )
                                                )
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            }
                    )
                }
            }
        }
    }

    // Dialogs (still need TV adaptation, but keep original for now)
    if (showNameDialog) NameDialog(setShowDialog = { showNameDialog = it })
    if (showBackgroundDialog) BackgroundDialog(setShowDialog = { showBackgroundDialog = it })
    if (showThemesDialog) ThemeDialog(setShowDialog = { showThemesDialog = it })
    if (showNavbarItemsDialog) NavbarItemsDialog(setShowDialog = { showNavbarItemsDialog = it })
    if (showHomeItemsDialog) HomeItemsDialog(setShowDialog = { showHomeItemsDialog = it })
    if (showNowPlayingLyricsAlignmentDialog) NowPlayingAlignmentDialog(
        setShowDialog = { showNowPlayingLyricsAlignmentDialog = it },
        selection = nowPlayingTitleAlignment,
        onSet = {
            coroutineScope.launch {
                AppearanceSettingsManager(context).setNowPlayingLyricsAlignment(it)
            }
        }
    )
}