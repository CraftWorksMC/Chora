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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppTheme
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.OLEDProtectionMode
import com.craftworks.music.ui.elements.dialogs.tv.BackgroundDialog
import com.craftworks.music.ui.elements.dialogs.tv.HomeItemsDialog
import com.craftworks.music.ui.elements.dialogs.tv.NameDialog
import com.craftworks.music.ui.elements.dialogs.tv.NavbarItemsDialog
import com.craftworks.music.ui.elements.dialogs.tv.NowPlayingAlignmentDialog
import com.craftworks.music.ui.elements.dialogs.tv.OledProtectionModeDialog
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
    var showOledDialog by remember { mutableStateOf(false) }
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
                    title = stringResource(R.string.appearance_username),
                    subtitle = username,
                    icon = ImageVector.vectorResource(R.drawable.s_a_username),
                    onClick = { showNameDialog = true }
                )

                // Theme
                val selectedTheme by AppearanceSettingsManager(context).appTheme.collectAsState(
                    AppTheme.SYSTEM.name
                )
                val themes = listOf(
                    AppTheme.DARK.name,
                    AppTheme.LIGHT.name,
                    AppTheme.SYSTEM.name
                )
                val themeStrings = mapOf(
                    AppTheme.DARK.name to R.string.theme_dark,
                    AppTheme.LIGHT.name to R.string.theme_light,
                    AppTheme.SYSTEM.name to R.string.theme_system
                )

                SettingsButtonItem(
                    title = stringResource(R.string.appearance_theme),
                    subtitle = stringResource(themeStrings[selectedTheme] ?: R.string.theme_system),
                    icon = ImageVector.vectorResource(R.drawable.s_a_palette),
                    onClick = { showThemesDialog = true }
                )

                // Background Style
                val backgroundType by AppearanceSettingsManager(context).npBackgroundFlow.collectAsState(
                    NowPlayingBackground.STATIC_BLUR
                )
                val backgroundLabels = mapOf(
                    NowPlayingBackground.PLAIN to R.string.background_style_plain,
                    NowPlayingBackground.STATIC_BLUR to R.string.background_style_blur,
                    NowPlayingBackground.ANIMATED_BLUR to R.string.background_style_anim,
                )

                SettingsButtonItem(
                    title = stringResource(R.string.appearance_background_style),
                    subtitle = stringResource(
                        backgroundLabels[backgroundType] ?: R.string.background_style_plain
                    ),
                    icon = ImageVector.vectorResource(R.drawable.s_a_background),
                    onClick = { showBackgroundDialog = true }
                )

                // OLED Protection Mode
                val oledProtection by AppearanceSettingsManager(context).oledProtectionMode.collectAsState(
                    OLEDProtectionMode.OFF
                )
                val oledLabels = mapOf(
                    OLEDProtectionMode.OFF to R.string.oled_protection_mode_off,
                    OLEDProtectionMode.LYRICS_ONLY to R.string.oled_protection_mode_lyrics_only,
                    OLEDProtectionMode.MINIMAL to R.string.oled_protection_mode_minimal,
                )

                SettingsButtonItem(
                    title = stringResource(R.string.appearance_oled_mode),
                    subtitle = stringResource(
                        oledLabels[oledProtection] ?: R.string.oled_protection_mode_off
                    ),
                    icon = ImageVector.vectorResource(R.drawable.rounded_tv_24),
                    onClick = { showOledDialog = true }
                )

                // Screen standby
                val screenStandby by AppearanceSettingsManager(context).disableScreenStandby.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.appearance_screen_standby),
                    icon = ImageVector.vectorResource(R.drawable.rounded_tv_24),
                    checked = screenStandby,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setDisableScreenStandby(it)
                        }
                    }
                )

                // Nav Items
                val enabledNavbarItems by AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(
                    emptyList()
                )

                SettingsButtonItem(
                    title = stringResource(R.string.appearance_navbar_items),
                    subtitle = enabledNavbarItems.filter { it.enabled }
                        .joinToString(", ") { it.title },
                    icon = ImageVector.vectorResource(R.drawable.s_a_navbar_items),
                    onClick = { showNavbarItemsDialog = true }
                )

                // Home Items
                val titleMap = mapOf(
                    "recently_played" to R.string.home_recently_played,
                    "recently_added" to R.string.home_recently_added,
                    "most_played" to R.string.home_most_played
                )
                val enabledHomeItems by AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(
                    emptyList()
                )

                SettingsButtonItem(
                    title = stringResource(R.string.appearance_home_items),
                    subtitle = enabledHomeItems.filter { it.enabled }
                        .map { stringResource(titleMap[it.key] ?: R.string.home_recently_played) }
                        .joinToString(","),
                    icon = ImageVector.vectorResource(R.drawable.s_a_home_items),
                    onClick = { showHomeItemsDialog = true }
                )

                // Now Playing Lyrics Alignment
                val alignmentLabels = mapOf(
                    NowPlayingAlignment.LEFT to R.string.alignment_setting_left,
                    NowPlayingAlignment.CENTER to R.string.alignment_setting_center,
                    NowPlayingAlignment.RIGHT to R.string.alignment_setting_right
                )

                SettingsButtonItem(
                    title = stringResource(R.string.appearance_now_playing_lyrics_alignment),
                    subtitle = stringResource(
                        alignmentLabels[nowPlayingTitleAlignment]
                            ?: R.string.alignment_setting_left
                    ),
                    icon = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                    onClick = { showNowPlayingLyricsAlignmentDialog = true }
                )
            }
        }

        // Switches
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Lyrics blur
                val nowPlayingLyricsBlur by AppearanceSettingsManager(context).nowPlayingLyricsBlurFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.appearance_now_playing_lyrics_blur),
                    icon = ImageVector.vectorResource(R.drawable.outline_line_weight_24),
                    checked = nowPlayingLyricsBlur,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setNowPlayingLyricsBlur(it)
                        }
                    },
                    enabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
                )
                val lyricsAutoScroll by AppearanceSettingsManager(context).lyricsAutoScroll.collectAsStateWithLifecycle(true)
                SettingsSwitchItem(
                    title = stringResource(R.string.appearance_lyrics_auto_scroll),
                    icon = ImageVector.vectorResource(R.drawable.rounded_text_select_move_down_24),
                    checked = lyricsAutoScroll,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setLyricsAutoScroll(!lyricsAutoScroll)
                        }
                    }
                )

                val lyricsRecenterAfterScroll by AppearanceSettingsManager(context).lyricsRecenterAfterScroll.collectAsStateWithLifecycle(true)
                SettingsSwitchItem(
                    title = stringResource(R.string.appearance_lyrics_recenter),
                    icon = ImageVector.vectorResource(R.drawable.rounded_vertical_align_center_24),
                    checked = lyricsRecenterAfterScroll,
                    onCheckedChange = {
                        coroutineScope.launch {
                            AppearanceSettingsManager(context).setLyricsRecenterAfterScroll(it)
                        }
                    }
                )

                // More Song Info
                val showMoreInfo by AppearanceSettingsManager(context).showMoreInfoFlow.collectAsState(
                    true
                )
                SettingsSwitchItem(
                    title = stringResource(R.string.appearance_more_info),
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
                    title = stringResource(R.string.appearance_provider_logo),
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
                    title = stringResource(R.string.appearance_provider_dividers),
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
                    title = stringResource(R.string.appearance_track_numbers_in_album_view),
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
                        text = stringResource(R.string.appearance_lyrics_animation_speed),
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
    if (showOledDialog) OledProtectionModeDialog(setShowDialog = { showOledDialog = it })
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