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
import kotlinx.coroutines.runBlocking

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.Settings_Header_Appearance)) },
                actions = {
                    IconButton(
                        onClick = {
                            navHostController.navigate(Screen.Setting.route) {
                                launchSingleTop = true
                            }
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
                    text = stringResource(R.string.Settings_Header_Appearance),
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
                        },
                        modifier = Modifier
                            .size(56.dp, 70.dp)
                            .focusRequester(focusRequester)
                            .focusProperties { left = FocusRequester.Cancel }) {
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
                    val username by AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")

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
                    val selectedTheme by AppearanceSettingsManager(context).appTheme.collectAsState(
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
                            id = themeStrings[themes.indexOf(selectedTheme)]
                        ),
                        ImageVector.vectorResource(R.drawable.s_a_palette),
                        toggleEvent = {
                            showThemesDialog = true
                        }
                    )

                    //Background Style
                    val backgroundType by AppearanceSettingsManager(context).npBackgroundFlow.collectAsState(
                        NowPlayingBackground.STATIC_BLUR
                    )

                    val backgroundTypeLabels = mapOf(
                        NowPlayingBackground.PLAIN to R.string.Background_Plain,
                        NowPlayingBackground.STATIC_BLUR to R.string.Background_Blur,
                        NowPlayingBackground.ANIMATED_BLUR to R.string.Background_Anim,
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
                        AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(
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
                        AppearanceSettingsManager(context).homeItemsItemsFlow.collectAsState(
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
                    val nowPlayingTitleAlignment by AppearanceSettingsManager(context).nowPlayingTitleAlignment.collectAsState(
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
                        AppearanceSettingsManager(context).nowPlayingLyricsBlurFlow.collectAsState(
                            true
                        )
                    SettingsSwitch(
                        nowPlayingLyricsBlur.value,
                        stringResource(R.string.Setting_NowPlayingLyricsBlur),
                        Icons.Rounded.Menu,
                        toggleEvent = {
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setNowPlayingLyricsBlur(!nowPlayingLyricsBlur.value)
                            }
                        },
                        enabled = Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU
                    )

                    // Lyrics Animation Speed
                    val lyricsAnimationSpeed =
                        AppearanceSettingsManager(context).lyricsAnimationSpeedFlow.collectAsState(
                            1200
                        )
                    val interactionSource = remember { MutableInteractionSource() }

                    val sliderValue = 2400f - lyricsAnimationSpeed.value.toFloat() + 600f

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
                                            runBlocking {
                                                AppearanceSettingsManager(context)
                                                    .setLyricsAnimationSpeed(
                                                        (lyricsAnimationSpeed.value - 300).coerceAtLeast(
                                                            600
                                                        )
                                                    )
                                            }
                                            true
                                        }

                                        Key.DirectionLeft if keyEvent.type == KeyEventType.KeyDown -> {
                                            runBlocking {
                                                AppearanceSettingsManager(context)
                                                    .setLyricsAnimationSpeed(
                                                        (lyricsAnimationSpeed.value + 300).coerceAtMost(
                                                            2400
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
                                val real = (2400f - (uiValue - 600f)).coerceIn(600f, 2400f)
                                runBlocking {
                                    AppearanceSettingsManager(context).setLyricsAnimationSpeed(real.toInt())
                                }
                            },

                            valueRange = 600f..2400f
                        )
                    }
                }

                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    //More Song Info
                    val showMoreInfo =
                        AppearanceSettingsManager(context).showMoreInfoFlow.collectAsState(true)
                    SettingsSwitch(
                        showMoreInfo.value,
                        stringResource(R.string.Setting_MoreInfo),
                        ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                        toggleEvent = {
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setShowMoreInfo(!showMoreInfo.value)
                            }
                        }
                    )

                    //Show Navidrome Logo
                    val showNavidromeLogo =
                        AppearanceSettingsManager(context).showNavidromeLogoFlow.collectAsState(true)
                    SettingsSwitch(
                        showNavidromeLogo.value,
                        stringResource(R.string.Setting_NavidromeLogo),
                        ImageVector.vectorResource(R.drawable.s_m_navidrome_bw),
                        toggleEvent = {
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setShowNavidromeLogo(!showNavidromeLogo.value)
                            }
                        }
                    )

                    //Show Provider Dividers
                    val showProviderDividers =
                        AppearanceSettingsManager(context).showProviderDividersFlow.collectAsState(
                            true
                        )
                    SettingsSwitch(
                        showProviderDividers.value,
                        stringResource(R.string.Setting_ProviderDividers),
                        ImageVector.vectorResource(R.drawable.s_a_moreinfo),
                        toggleEvent = {
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setShowProviderDividers(!showProviderDividers.value)
                            }
                        }
                    )

                    //Refresh Ripple
                    val refreshRipple =
                        AppearanceSettingsManager(context).refreshAnimationFlow.collectAsState(true)
                    SettingsSwitch(
                        refreshRipple.value,
                        stringResource(R.string.Setting_RefreshAnimation),
                        Icons.Rounded.Refresh,
                        toggleEvent = {
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setUseRefreshAnimation(!refreshRipple.value)
                            }
                        },
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    )

                    // Track numbers in album view
                    val showTrackNumbers =
                        AppearanceSettingsManager(context).showTrackNumbersFlow.collectAsState(true)
                    SettingsSwitch(
                        showTrackNumbers.value,
                        stringResource(R.string.Setting_TrackNumbersAlbum),
                        ImageVector.vectorResource(R.drawable.rounded_format_list_numbered_24),
                        toggleEvent = {
                            coroutineScope.launch {
                                AppearanceSettingsManager(context).setShowTrackNumbers(!showTrackNumbers.value)
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