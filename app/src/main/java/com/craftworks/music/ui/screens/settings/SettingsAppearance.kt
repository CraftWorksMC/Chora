package com.craftworks.music.ui.screens.settings

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.settings.AppTheme
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.ActionButtonType
import com.craftworks.music.ui.elements.dialogs.appearance.BackgroundDialog
import com.craftworks.music.ui.elements.dialogs.appearance.HomeItemsDialog
import com.craftworks.music.ui.elements.dialogs.appearance.NameDialog
import com.craftworks.music.ui.elements.dialogs.appearance.NavbarItemsDialog
import com.craftworks.music.ui.elements.dialogs.appearance.NowPlayingTitleAlignmentDialog
import com.craftworks.music.ui.elements.dialogs.appearance.SongListActionButtonsDialog
import com.craftworks.music.ui.elements.dialogs.appearance.ThemeDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.playing.NowPlayingAlignment
import com.craftworks.music.ui.playing.NowPlayingBackground
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
fun S_AppearanceScreen(navHostController: NavHostController = rememberNavController()) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showNavbarItemsDialog by remember { mutableStateOf(false) }
    var showHomeItemsDialog by remember { mutableStateOf(false) }
    var showNowPlayingTitleAlignmentDialog by remember { mutableStateOf(false) }
    var showNowPlayingLyricsAlignmentDialog by remember { mutableStateOf(false) }
    var showAlbumDetailsActionButtonsDialog by remember { mutableStateOf(false) }
    var showArtistDetailsActionButtonsDialog by remember { mutableStateOf(false) }
    var showPlaylistDetailsActionButtonsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val appearanceSettingsManager = AppearanceSettingsManager(context)

    // Now Playing Title Alignment
    val nowPlayingTitleAlignment by appearanceSettingsManager.nowPlayingTitleAlignment.collectAsState(
        NowPlayingAlignment.LEFT
    )
    // Now Playing Lyrics Alignment
    val nowPlayingLyricsAlignment by appearanceSettingsManager.nowPlayingLyricsAlignment.collectAsState(
        NowPlayingAlignment.CENTER
    )
    val alignmentLabels = mapOf(
        NowPlayingAlignment.LEFT to R.string.alignment_setting_left,
        NowPlayingAlignment.CENTER to R.string.alignment_setting_center,
        NowPlayingAlignment.RIGHT to R.string.alignment_setting_right
    )

    val albumDetailsActionButtons by appearanceSettingsManager.albumDetailsButtons.collectAsState(emptyList())
    val artistDetailsActionButtons by appearanceSettingsManager.artistDetailsButtons.collectAsState(emptyList())
    val playlistDetailsActionButtons by appearanceSettingsManager.playlistDetailsButtons.collectAsState(emptyList())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_appearance)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navHostController.popBackStack()
                        },
                        modifier = Modifier.size(56.dp, 70.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Previous Song",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding()
                )
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .dialogFocusable()
        ) {
            /* HEADER */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_a_palette),
                    contentDescription = "Settings Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_appearance),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = {
                            navHostController.navigate(Screen.Settings) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .size(56.dp, 70.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back To Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                Modifier
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    //Username
                    val username by appearanceSettingsManager.usernameFlow.collectAsState("Username")

                    SettingsDialogButton(
                        stringResource(R.string.appearance_username),
                        username,
                        ImageVector.vectorResource(R.drawable.s_a_username),
                        toggleEvent = {
                            showNameDialog = true
                        }
                    )
                }

                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    //Theme
                    val selectedTheme by appearanceSettingsManager.appTheme.collectAsState(
                        AppTheme.SYSTEM.name
                    )
                    val themes = listOf(
                        AppTheme.DARK.name,
                        AppTheme.LIGHT.name,
                        AppTheme.SYSTEM.name
                    )
                    val themeStrings = listOf(
                        R.string.theme_dark, R.string.theme_light, R.string.theme_system
                    )
                    SettingsDialogButton(
                        stringResource(R.string.appearance_theme),
                        stringResource(
                            id = themeStrings[themes.indexOf(selectedTheme)]
                        ),
                        ImageVector.vectorResource(R.drawable.s_a_palette),
                        toggleEvent = {
                            showThemesDialog = true
                        }
                    )

                    //Background Style
                    val backgroundType by appearanceSettingsManager.npBackgroundFlow.collectAsState(
                        NowPlayingBackground.STATIC_BLUR
                    )

                    val backgroundTypeLabels = mapOf(
                        NowPlayingBackground.PLAIN to R.string.background_style_plain,
                        NowPlayingBackground.STATIC_BLUR to R.string.background_style_blur,
                        NowPlayingBackground.ANIMATED_BLUR to R.string.background_style_anim,
                    )
                    SettingsDialogButton(
                        stringResource(R.string.appearance_background_style),
                        stringResource(
                            backgroundTypeLabels[backgroundType]
                                ?: androidx.media3.session.R.string.error_message_invalid_state
                        ),
                        ImageVector.vectorResource(R.drawable.s_a_background),
                        toggleEvent = {
                            showBackgroundDialog = true
                        }
                    )

                    //Disable Screen Standby
                    val disableScreenStandby = appearanceSettingsManager.disableScreenStandby.collectAsState(true)
                    SettingsSwitch(
                        disableScreenStandby.value,
                        stringResource(R.string.appearance_screen_standby),
                        ImageVector.vectorResource(R.drawable.rounded_tv_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setDisableScreenStandby(!disableScreenStandby.value)
                            }
                        }
                    )

                    //Navbar Items
                    val navBarItemsEnabled =
                        LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION
                    val enabledNavbarItems = appearanceSettingsManager.bottomNavItemsFlow.collectAsState(emptyList()).value
                            .filter { it.enabled }
                            .joinToString(", ") { it.title }
                    SettingsDialogButton(
                        stringResource(R.string.appearance_navbar_items),
                        enabledNavbarItems,
                        ImageVector.vectorResource(R.drawable.s_a_navbar_items),
                        toggleEvent = {
                            if (navBarItemsEnabled)
                                showNavbarItemsDialog = true
                        }
                    )

                    //Home Items
                    val titleMap = remember {
                        mapOf(
                            "recently_played" to R.string.home_recently_played,
                            "recently_added" to R.string.home_recently_added,
                            "most_played" to R.string.home_most_played,
                            "random_songs" to R.string.home_explore_library
                        )
                    }
                    val enabledHomeItems = appearanceSettingsManager.homeItemsItemsFlow.collectAsState(emptyList()).value
                            .filter { it.enabled }
                            .joinToString(", ") {
                                context.getString(
                                    titleMap[it.key]
                                        ?: androidx.media3.session.R.string.error_message_fallback
                                )
                            }

                    SettingsDialogButton(
                        stringResource(R.string.appearance_home_items),
                        enabledHomeItems,
                        ImageVector.vectorResource(R.drawable.s_a_home_items),
                        toggleEvent = {
                            showHomeItemsDialog = true
                        }
                    )


                    //Advanced Queue
                    val useAdvancedQueue =
                        AppearanceSettingsManager(context).useAdvancedQueue.collectAsState(false)
                    SettingsSwitch(
                        useAdvancedQueue.value,
                        stringResource(R.string.appearance_advanced_queue),
                        ImageVector.vectorResource(R.drawable.rounded_format_list_numbered_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setUseAdvancedQueue(!useAdvancedQueue.value)
                            }
                        }
                    )

                    SettingsDialogButton(
                        stringResource(R.string.appearance_now_playing_title_alignment),
                        stringResource(
                            alignmentLabels[nowPlayingTitleAlignment]
                                ?: R.string.alignment_setting_left
                        ),
                        ImageVector.vectorResource(R.drawable.rounded_sort_24),
                        toggleEvent = {
                            showNowPlayingTitleAlignmentDialog = true
                        }
                    )
                }

                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsDialogButton(
                        stringResource(R.string.appearance_now_playing_lyrics_alignment),
                        stringResource(
                            alignmentLabels[nowPlayingLyricsAlignment]
                                ?: R.string.alignment_setting_center
                        ),
                        ImageVector.vectorResource(R.drawable.rounded_sort_24),
                        toggleEvent = {
                            showNowPlayingLyricsAlignmentDialog = true
                        }
                    )
                    //Lyrics blur Info
                    val nowPlayingLyricsBlur by appearanceSettingsManager.nowPlayingLyricsBlurFlow.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        nowPlayingLyricsBlur,
                        stringResource(R.string.appearance_now_playing_lyrics_blur),
                        ImageVector.vectorResource(R.drawable.outline_line_weight_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setNowPlayingLyricsBlur(!nowPlayingLyricsBlur)
                            }
                        },
                        enabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
                    )

                    val lyricsAutoScroll by appearanceSettingsManager.lyricsAutoScroll.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        lyricsAutoScroll,
                        stringResource(R.string.appearance_lyrics_auto_scroll),
                        ImageVector.vectorResource(R.drawable.rounded_text_select_move_down_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setLyricsAutoScroll(!lyricsAutoScroll)
                            }
                        }
                    )

                    val lyricsRecenterAfterScroll by appearanceSettingsManager.lyricsRecenterAfterScroll.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        lyricsRecenterAfterScroll,
                        stringResource(R.string.appearance_lyrics_recenter),
                        ImageVector.vectorResource(R.drawable.rounded_vertical_align_center_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setLyricsRecenterAfterScroll(!lyricsRecenterAfterScroll)
                            }
                        }
                    )

                    val lyricsWordBounce by appearanceSettingsManager.lyricsBounce.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        lyricsWordBounce,
                        stringResource(R.string.appearance_lyrics_word_bounce),
                        ImageVector.vectorResource(R.drawable.rounded_format_line_spacing_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setLyricsBounce(!lyricsWordBounce)
                            }
                        }
                    )

                    // Lyrics Animation Speed
                    val lyricsAnimationSpeed =
                        appearanceSettingsManager.lyricsAnimationSpeedFlow.collectAsState(660)

                    val minValue = 300f
                    val maxValue = 1200f
                    val sliderValue = maxValue - lyricsAnimationSpeed.value.toFloat() + minValue

                    Column(
                        Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ) {
                        Text(
                            text = stringResource(R.string.appearance_lyrics_animation_speed),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .padding(top = 10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Slider(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 10.dp),
                            value = sliderValue,
                            steps = 4,
                            onValueChange = { uiValue ->
                                val real = (maxValue - (uiValue - minValue)).coerceIn(minValue, maxValue)
                                runBlocking {
                                    appearanceSettingsManager.setLyricsAnimationSpeed(real.roundToInt())
                                }
                            },
                            valueRange = minValue..maxValue
                        )
                    }
                }

                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    //More Song Info
                    val showMoreInfo =
                        appearanceSettingsManager.showMoreInfoFlow.collectAsState(true)
                    SettingsSwitch(
                        showMoreInfo.value,
                        stringResource(R.string.appearance_more_info),
                        ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setShowMoreInfo(!showMoreInfo.value)
                            }
                        }
                    )

                    //Show Navidrome Logo
                    val showNavidromeLogo =
                        appearanceSettingsManager.showNavidromeLogoFlow.collectAsState(true)
                    SettingsSwitch(
                        showNavidromeLogo.value,
                        stringResource(R.string.appearance_provider_logo),
                        ImageVector.vectorResource(R.drawable.s_m_navidrome_bw),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setShowNavidromeLogo(!showNavidromeLogo.value)
                            }
                        }
                    )

                    //Refresh Ripple
                    val refreshRipple =
                        appearanceSettingsManager.refreshAnimationFlow.collectAsState(true)
                    SettingsSwitch(
                        refreshRipple.value,
                        stringResource(R.string.appearance_refresh_animation),
                        Icons.Rounded.Refresh,
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setUseRefreshAnimation(!refreshRipple.value)
                            }
                        },
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    )

                    // Track numbers in album view
                    val showTrackNumbers =
                        appearanceSettingsManager.showTrackNumbersFlow.collectAsState(true)
                    SettingsSwitch(
                        showTrackNumbers.value,
                        stringResource(R.string.appearance_track_numbers_in_album_view),
                        ImageVector.vectorResource(R.drawable.rounded_format_list_numbered_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setShowTrackNumbers(!showTrackNumbers.value)
                            }
                        }
                    )
                }
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val albumDetailsButtons by appearanceSettingsManager.albumDetailsButtons.collectAsState(emptyList())
                    val artistDetailsButtons by appearanceSettingsManager.artistDetailsButtons.collectAsState(emptyList())
                    val playlistDetailsButtons by appearanceSettingsManager.playlistDetailsButtons.collectAsState(emptyList())

                    val detailButtonsLocalizationMap = mapOf(
                        ActionButtonType.SEPARATOR to stringResource(R.string.label_separator),
                        ActionButtonType.SHUFFLE to stringResource(R.string.action_shuffle),
                        ActionButtonType.FAVORITE to stringResource(R.string.action_add_to_favorites),
                        ActionButtonType.ADD_TO_QUEUE to stringResource(R.string.action_add_to_queue),
                        ActionButtonType.PLAY_NEXT to stringResource(R.string.action_play_next),
                        ActionButtonType.ADD_TO_PLAYLIST to stringResource(R.string.action_add_to_playlist),
                        ActionButtonType.DOWNLOAD to stringResource(R.string.action_download),
                    )

                    SettingsDialogButton(
                        stringResource(R.string.appearance_album_details_action_buttons),
                        albumDetailsButtons.filter { !it.inMenu }
                            .joinToString(", ") { detailButtonsLocalizationMap[it.type].toString() },
                        ImageVector.vectorResource(R.drawable.action_key_24px),
                        toggleEvent = {
                            showAlbumDetailsActionButtonsDialog = true
                        }
                    )

                    SettingsDialogButton(
                        stringResource(R.string.appearance_artist_details_action_buttons),
                        artistDetailsButtons.filter { !it.inMenu }
                            .joinToString(", ") { detailButtonsLocalizationMap[it.type].toString() },
                        ImageVector.vectorResource(R.drawable.action_key_24px),
                        toggleEvent = {
                            showArtistDetailsActionButtonsDialog = true
                        }
                    )

                    SettingsDialogButton(
                        stringResource(R.string.appearance_playlist_details_action_buttons),
                        playlistDetailsButtons.filter { !it.inMenu }
                            .joinToString(", ") { detailButtonsLocalizationMap[it.type].toString() },
                        ImageVector.vectorResource(R.drawable.action_key_24px),
                        toggleEvent = {
                            showPlaylistDetailsActionButtonsDialog = true
                        }
                    )
                }
            }
        }

        if(showNameDialog)
            NameDialog(setShowDialog = { showNameDialog = it })

        if(showBackgroundDialog)
            BackgroundDialog(setShowDialog = { showBackgroundDialog = it })

        if(showThemesDialog)
            ThemeDialog(setShowDialog = { showThemesDialog = it })

        if(showNavbarItemsDialog)
            NavbarItemsDialog(setShowDialog = { showNavbarItemsDialog = it })

        if(showHomeItemsDialog)
            HomeItemsDialog(setShowDialog = { showHomeItemsDialog = it })

        if(showNowPlayingTitleAlignmentDialog)
            NowPlayingTitleAlignmentDialog(
                setShowDialog = { showNowPlayingTitleAlignmentDialog = it },
                title = stringResource(R.string.appearance_now_playing_title_alignment),
                selection = nowPlayingTitleAlignment,
                onSet = {
                    runBlocking {
                        appearanceSettingsManager.setNowPlayingTitleAlignment(it)
                    }
                }
            )
        if(showNowPlayingLyricsAlignmentDialog)
            NowPlayingTitleAlignmentDialog(
                setShowDialog = { showNowPlayingLyricsAlignmentDialog = it },
                title = stringResource(R.string.appearance_now_playing_lyrics_alignment),
                selection = nowPlayingLyricsAlignment,
                onSet = {
                    runBlocking {
                        appearanceSettingsManager.setNowPlayingLyricsAlignment(it)
                    }
                }
            )
        if (showAlbumDetailsActionButtonsDialog)
            SongListActionButtonsDialog(
                title = stringResource(R.string.appearance_action_buttons_editor_title),
                actionButtons = albumDetailsActionButtons,
                supportedButtonTypes = listOf(
                    ActionButtonType.SEPARATOR,
                    ActionButtonType.SHUFFLE,
                    ActionButtonType.FAVORITE,
                    ActionButtonType.ADD_TO_QUEUE,
                    ActionButtonType.PLAY_NEXT,
                    ActionButtonType.ADD_TO_PLAYLIST,
                    ActionButtonType.DOWNLOAD,
                ),
                onSet = {
                    runBlocking {
                        appearanceSettingsManager.setAlbumDetailsButtons(it)
                    }
                },
                onDismissRequest = {
                    showAlbumDetailsActionButtonsDialog = false
                }
            )
        if (showArtistDetailsActionButtonsDialog)
            SongListActionButtonsDialog(
                title = stringResource(R.string.appearance_action_buttons_editor_title),
                actionButtons = artistDetailsActionButtons,
                supportedButtonTypes = listOf(
                    ActionButtonType.SEPARATOR,
                    ActionButtonType.SHUFFLE,
                    ActionButtonType.FAVORITE,
                    ActionButtonType.ADD_TO_QUEUE,
                    ActionButtonType.PLAY_NEXT,
                    ActionButtonType.ADD_TO_PLAYLIST,
                    ActionButtonType.DOWNLOAD,
                ),
                onSet = {
                    runBlocking {
                        appearanceSettingsManager.setArtistDetailsButtons(it)
                    }
                },
                onDismissRequest = {
                    showArtistDetailsActionButtonsDialog = false
                }
            )
        if (showPlaylistDetailsActionButtonsDialog)
            SongListActionButtonsDialog(
                title = stringResource(R.string.appearance_action_buttons_editor_title),
                actionButtons = playlistDetailsActionButtons,
                supportedButtonTypes = listOf(
                    ActionButtonType.SEPARATOR,
                    ActionButtonType.SHUFFLE,
                    ActionButtonType.ADD_TO_QUEUE,
                    ActionButtonType.PLAY_NEXT,
                    ActionButtonType.DOWNLOAD,
                ),
                onSet = {
                    runBlocking {
                        appearanceSettingsManager.setPlaylistDetailsButtons(it)
                    }
                },
                onDismissRequest = {
                    showPlaylistDetailsActionButtonsDialog = false
                }
            )
    }
}