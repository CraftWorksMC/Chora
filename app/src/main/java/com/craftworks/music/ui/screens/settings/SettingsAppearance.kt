package com.craftworks.music.ui.screens.settings

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Menu
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.ArtworkSettingsManager
import com.craftworks.music.ui.elements.dialogs.BackgroundDialog
import com.craftworks.music.ui.elements.dialogs.HomeItemsDialog
import com.craftworks.music.ui.elements.dialogs.NameDialog
import com.craftworks.music.ui.elements.dialogs.NavbarItemsDialog
import com.craftworks.music.ui.elements.dialogs.NowPlayingTitleAlignmentDialog
import com.craftworks.music.ui.elements.dialogs.ThemeDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.playing.NowPlayingBackground
import com.craftworks.music.ui.playing.NowPlayingTitleAlignment
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_AppearanceScreen(navHostController: NavHostController = rememberNavController()) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
    var showNavbarItemsDialog by remember { mutableStateOf(false) }
    var showHomeItemsDialog by remember { mutableStateOf(false) }
    var showNowPlayingTitleAlignmentDialog by remember { mutableStateOf(false) }
    val lyricsAnimationSpeedStep = 300
    val minLyricsAnimationSpeed = 600
    val maxLyricsAnimationSpeed = 2400
    val defaultLyricsAnimationSpeed = 1200


    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // FIXED: Use single remembered instance to avoid creating multiple SettingsManager instances
    val appearanceSettingsManager = remember { AppearanceSettingsManager(context.applicationContext) }
    val artworkSettingsManager = remember { ArtworkSettingsManager(context.applicationContext) }

    val focusRequester = remember { FocusRequester() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.Settings_Header_Appearance)) },
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
        Box(
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding()
                )
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .dialogFocusable()
        ) {
            Column(
                Modifier
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    //Username
                    val username by appearanceSettingsManager.usernameFlow.collectAsStateWithLifecycle("Username")

                    SettingsDialogButton(
                        stringResource(R.string.Setting_Username),
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
                    val selectedTheme by appearanceSettingsManager.appTheme.collectAsStateWithLifecycle(
                        AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name
                    )
                    val themes = listOf(
                        AppearanceSettingsManager.Companion.AppTheme.DARK.name,
                        AppearanceSettingsManager.Companion.AppTheme.LIGHT.name,
                        AppearanceSettingsManager.Companion.AppTheme.SYSTEM.name
                    )
                    val themeStrings = listOf(
                        R.string.Theme_Dark, R.string.Theme_Light, R.string.Theme_System
                    )
                    SettingsDialogButton(
                        stringResource(R.string.Dialog_Theme),
                        stringResource(
                            id = themeStrings.getOrElse(themes.indexOf(selectedTheme)) { R.string.Theme_System }
                        ),
                        ImageVector.vectorResource(R.drawable.s_a_palette),
                        toggleEvent = {
                            showThemesDialog = true
                        }
                    )

                    //Background Style
                    val backgroundType by appearanceSettingsManager.npBackgroundFlow.collectAsStateWithLifecycle(
                        NowPlayingBackground.SIMPLE_ANIMATED_BLUR
                    )

                    val backgroundTypeLabels = mapOf(
                        NowPlayingBackground.PLAIN to R.string.Background_Plain,
                        NowPlayingBackground.STATIC_BLUR to R.string.Background_Blur,
                        NowPlayingBackground.ANIMATED_BLUR to R.string.Background_Anim,
                        NowPlayingBackground.SIMPLE_ANIMATED_BLUR to R.string.Background_Anim_Simple
                    )
                    SettingsDialogButton(
                        stringResource(R.string.Setting_Background),
                        stringResource(
                            backgroundTypeLabels[backgroundType]
                                ?: androidx.media3.session.R.string.error_message_invalid_state
                        ),
                        ImageVector.vectorResource(R.drawable.s_a_background),
                        toggleEvent = {
                            showBackgroundDialog = true
                        }
                    )

                    //Navbar Items
                    val navBarItemsEnabled =
                        LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION
                    val enabledNavbarItems =
                        appearanceSettingsManager.bottomNavItemsFlow.collectAsStateWithLifecycle(
                            emptyList()
                        ).value
                            .filter { it.enabled }
                            .joinToString(", ") { it.title }
                    SettingsDialogButton(
                        stringResource(R.string.Setting_Navbar_Items),
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
                            "recently_played" to R.string.recently_played,
                            "recently_added" to R.string.recently_added,
                            "most_played" to R.string.most_played,
                            "random_songs" to R.string.random_songs
                        )
                    }
                    val enabledHomeItems =
                        appearanceSettingsManager.homeItemsItemsFlow.collectAsStateWithLifecycle(
                            emptyList()
                        ).value
                            .filter { it.enabled }
                            .joinToString(", ") {
                                context.getString(
                                    titleMap[it.key]
                                        ?: androidx.media3.session.R.string.error_message_fallback
                                )
                            }

                    SettingsDialogButton(
                        stringResource(R.string.Setting_Home_Items),
                        enabledHomeItems,
                        ImageVector.vectorResource(R.drawable.s_a_home_items),
                        toggleEvent = {
                            showHomeItemsDialog = true
                        }
                    )

                    // Now Playing Title Alignment
                    val nowPlayingTitleAlignment by appearanceSettingsManager.nowPlayingTitleAlignment.collectAsStateWithLifecycle(
                        NowPlayingTitleAlignment.LEFT
                    )
                    val alignmentLabels = mapOf(
                        NowPlayingTitleAlignment.LEFT to R.string.NowPlayingTitleAlignment_Left,
                        NowPlayingTitleAlignment.CENTER to R.string.NowPlayingTitleAlignment_Center,
                        NowPlayingTitleAlignment.RIGHT to R.string.NowPlayingTitleAlignment_Right
                    )
                    SettingsDialogButton(
                        stringResource(R.string.Setting_NowPlayingTitleAlignment),
                        stringResource(
                            alignmentLabels[nowPlayingTitleAlignment]
                                ?: R.string.NowPlayingTitleAlignment_Left
                        ), // Default to Left if not found
                        Icons.Rounded.Menu, // Placeholder icon
                        toggleEvent = {
                            showNowPlayingTitleAlignmentDialog = true
                        }
                    )
                }

                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    //Lyrics blur Info
                    val nowPlayingLyricsBlur =
                        appearanceSettingsManager.nowPlayingLyricsBlurFlow.collectAsStateWithLifecycle(
                            true
                        )
                    SettingsSwitch(
                        nowPlayingLyricsBlur.value,
                        stringResource(R.string.Setting_NowPlayingLyricsBlur),
                        Icons.Rounded.Menu,
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setNowPlayingLyricsBlur(!nowPlayingLyricsBlur.value)
                            }
                        },
                        enabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
                    )

                    // Lyrics Animation Speed
                    val lyricsAnimationSpeed =
                        appearanceSettingsManager.lyricsAnimationSpeedFlow.collectAsStateWithLifecycle(
                            defaultLyricsAnimationSpeed
                        )
                    val interactionSource = remember { MutableInteractionSource() }

                    // We invert the slider's value so that sliding right means faster animation (a lower duration).
                    // The value is stored as duration in ms, so a lower value is faster.
                    // Slider's value goes from min (left) to max (right).
                    // We map [minSpeed, maxSpeed] to [maxSlider, minSlider]
                    // So slider value becomes (max + min) - real_value
                    val sliderValue = (maxLyricsAnimationSpeed + minLyricsAnimationSpeed) - lyricsAnimationSpeed.value.toFloat()


                    Column(
                        Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Text(
                            text = stringResource(R.string.Setting_LyricsAnimationSpeed),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 6.dp).padding(top = 10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Slider(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 10.dp)
                                .onKeyEvent { keyEvent ->
                                    when (keyEvent.key) {
                                        Key.DirectionRight if keyEvent.type == KeyEventType.KeyDown -> {
                                            coroutineScope.launch {
                                                appearanceSettingsManager
                                                    .setLyricsAnimationSpeed(
                                                        (lyricsAnimationSpeed.value - lyricsAnimationSpeedStep).coerceAtLeast(
                                                            minLyricsAnimationSpeed
                                                        )
                                                    )
                                            }
                                            true
                                        }

                                        Key.DirectionLeft if keyEvent.type == KeyEventType.KeyDown -> {
                                            coroutineScope.launch {
                                                appearanceSettingsManager
                                                    .setLyricsAnimationSpeed(
                                                        (lyricsAnimationSpeed.value + lyricsAnimationSpeedStep).coerceAtMost(
                                                            maxLyricsAnimationSpeed
                                                        )
                                                    )
                                            }
                                            true
                                        }

                                        else -> false
                                    }
                                },
                            interactionSource = interactionSource,
                            value = sliderValue,
                            steps = 5,
                            onValueChange = { uiValue ->
                                val real = ((maxLyricsAnimationSpeed + minLyricsAnimationSpeed) - uiValue).coerceIn(minLyricsAnimationSpeed.toFloat(), maxLyricsAnimationSpeed.toFloat())
                                coroutineScope.launch {
                                    appearanceSettingsManager.setLyricsAnimationSpeed(real.toInt())
                                }
                            },

                            valueRange = minLyricsAnimationSpeed.toFloat()..maxLyricsAnimationSpeed.toFloat()
                        )
                    }
                }

                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    //More Song Info
                    val showMoreInfo =
                        appearanceSettingsManager.showMoreInfoFlow.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        showMoreInfo.value,
                        stringResource(R.string.Setting_MoreInfo),
                        ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setShowMoreInfo(!showMoreInfo.value)
                            }
                        }
                    )

                    //Show Navidrome Logo
                    val showNavidromeLogo =
                        appearanceSettingsManager.showNavidromeLogoFlow.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        showNavidromeLogo.value,
                        stringResource(R.string.Setting_NavidromeLogo),
                        ImageVector.vectorResource(R.drawable.s_m_navidrome),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setShowNavidromeLogo(!showNavidromeLogo.value)
                            }
                        }
                    )

                    //Show Provider Dividers
                    val showProviderDividers =
                        appearanceSettingsManager.showProviderDividersFlow.collectAsStateWithLifecycle(
                            true
                        )
                    SettingsSwitch(
                        showProviderDividers.value,
                        stringResource(R.string.Setting_ProviderDividers),
                        ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setShowProviderDividers(!showProviderDividers.value)
                            }
                        }
                    )

                    //Refresh Ripple
                    val refreshRipple =
                        appearanceSettingsManager.refreshAnimationFlow.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        refreshRipple.value,
                        stringResource(R.string.Setting_RefreshAnimation),
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
                        appearanceSettingsManager.showTrackNumbersFlow.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        showTrackNumbers.value,
                        stringResource(R.string.Setting_TrackNumbersAlbum),
                        ImageVector.vectorResource(R.drawable.s_p_scrobble),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setShowTrackNumbers(!showTrackNumbers.value)
                            }
                        }
                    )

                    // Strip track numbers from titles
                    val stripTrackNumbers =
                        appearanceSettingsManager.stripTrackNumbersFromTitlesFlow.collectAsStateWithLifecycle(false)
                    SettingsSwitch(
                        stripTrackNumbers.value,
                        stringResource(R.string.Setting_StripTrackNumbers),
                        ImageVector.vectorResource(R.drawable.round_music_note_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                appearanceSettingsManager.setStripTrackNumbersFromTitles(!stripTrackNumbers.value)
                            }
                        }
                    )

                    // Generated Artwork
                    val generatedArtworkEnabled =
                        artworkSettingsManager.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)
                    SettingsSwitch(
                        generatedArtworkEnabled.value,
                        stringResource(R.string.Setting_GeneratedArtwork),
                        ImageVector.vectorResource(R.drawable.s_a_palette),
                        toggleEvent = {
                            coroutineScope.launch {
                                artworkSettingsManager.setGeneratedArtworkEnabled(!generatedArtworkEnabled.value)
                            }
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
            NowPlayingTitleAlignmentDialog(setShowDialog = { showNowPlayingTitleAlignmentDialog = it })
    }
}