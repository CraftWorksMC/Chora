package com.craftworks.music

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component4
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component5
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component6
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component7
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.NotificationUtil.IMPORTANCE_LOW
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.rememberDrawerState
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.settings.AppTheme
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.elements.dialogs.tv.OnboardingDialog
import com.craftworks.music.ui.playing.NowPlayingContent
import com.craftworks.music.ui.playing.NowPlayingMiniPlayer
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.theme.MusicPlayerTheme
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.verticalFadingEdges
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    lateinit var navController: NavHostController

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val selectedThemeName by AppearanceSettingsManager(this).appTheme.collectAsState(
                AppTheme.SYSTEM.name
            )
            val darkTheme = when (selectedThemeName) {
                AppTheme.DARK.name -> true
                AppTheme.LIGHT.name -> false
                else -> isSystemInDarkTheme()
            }

            MusicPlayerTheme (darkTheme) {
                navController = rememberNavController()

                val mediaController by rememberManagedMediaController()
                var metadata by remember { mutableStateOf<MediaMetadata?>(null) }

                val coroutineScope = rememberCoroutineScope()

                // Update metadata from mediaController.
                DisposableEffect(mediaController) {
                    val listener = object : Player.Listener {
                        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
                            metadata = mediaMetadata
                            super.onPlaylistMetadataChanged(mediaMetadata)
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            metadata = mediaItem?.mediaMetadata
                            super.onMediaItemTransition(mediaItem, reason)
                        }

                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            metadata = mediaMetadata
                            super.onMediaMetadataChanged(mediaMetadata)
                        }
                    }

                    metadata = mediaController?.mediaMetadata
                    mediaController?.addListener(listener)

                    onDispose {
                        mediaController?.removeListener(listener)
                    }
                }


                val positionalThreshold = dpToPx(56).toFloat()
                val velocityThreshold = dpToPx(125).toFloat()

                val scaffoldState = remember {
                    BottomSheetScaffoldState(
                        bottomSheetState = SheetState(
                            skipPartiallyExpanded = false,
                            initialValue = SheetValue.PartiallyExpanded,
                            skipHiddenState = true,
                            velocityThreshold = { positionalThreshold },
                            positionalThreshold = { velocityThreshold }
                        ), snackbarHostState = SnackbarHostState()
                    )
                }
                val peekHeight by animateDpAsState(
                    targetValue = if (metadata?.title != null) 72.dp else 0.dp,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "sheetPeekAnimation"
                )

                val isTv = LocalConfiguration.current.uiMode and
                        Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

                if (isTv) {
                    // Set background color to colorScheme.background
                    window.decorView.setBackgroundColor(androidx.tv.material3.MaterialTheme.colorScheme.background.toArgb())

                    SetupNavGraph(
                        navController = navController,
                        bottomPadding = 0.dp,
                        mediaController = mediaController
                    )
                } else {
                    // Set background color to colorScheme.background
                    window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

                    val backCallback = object : OnBackPressedCallback(false) {
                        override fun handleOnBackPressed() {
                            coroutineScope.launch {
                                scaffoldState.bottomSheetState.partialExpand()
                            }
                        }
                    }

                    onBackPressedDispatcher.addCallback(this, backCallback)

                    Scaffold(
                        bottomBar = {
                            AnimatedBottomNavBar(navController, scaffoldState)
                        },
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize()
                    ) { paddingValues ->
                        if (LocalWindowInfo.current.containerSize.width < dpToPx(640)) {
                            BottomSheetScaffold(
                                sheetContainerColor = Color.Transparent,
                                containerColor = Color.Transparent,
                                sheetPeekHeight = peekHeight + 80.dp + WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding(),
                                //sheetShadowElevation = 6.dp,
                                sheetShape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
                                sheetDragHandle = { },
                                scaffoldState = scaffoldState,
                                sheetContent = {
                                    val coroutineScope = rememberCoroutineScope()

                                    Box {
                                        NowPlayingMiniPlayer(
                                            scaffoldState = scaffoldState,
                                            metadata = metadata,
                                            onClick = {
                                                coroutineScope.launch {
                                                    scaffoldState.bottomSheetState.expand()
                                                }
                                            })

                                        println("Recomposing sheetcontent")
                                        NowPlayingContent(
                                            mediaController = mediaController,
                                            metadata = metadata,
                                            navController,
                                            onHide = {
                                                coroutineScope.launch {
                                                    scaffoldState.bottomSheetState.partialExpand()
                                                }
                                            }
                                        )
                                    }

                                    val currentView = LocalView.current
                                    val disableScreenStandy by AppearanceSettingsManager(LocalContext.current).disableScreenStandby.collectAsStateWithLifecycle(true)
                                    DisposableEffect(scaffoldState.bottomSheetState.targetValue) {
                                        if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) {
                                            if (disableScreenStandy)
                                                currentView.keepScreenOn = true

                                            backCallback.isEnabled  = true

                                            /* Restore nav bars.
                                            @Suppress("DEPRECATION")
                                            currentView.systemUiVisibility =
                                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                            */

                                            Log.d("NOW-PLAYING", "KeepScreenOn: True")
                                        } else {
                                            currentView.keepScreenOn = false
                                            backCallback.isEnabled = false

                                            /* Restore nav bars.
                                            @Suppress("DEPRECATION")
                                            currentView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            */
                                            Log.d("NOW-PLAYING", "KeepScreenOn: False")
                                        }

                                        onDispose {
                                            currentView.keepScreenOn = false
                                            backCallback.isEnabled = false
                                            Log.d("NOW-PLAYING", "KeepScreenOn: False")
                                        }
                                    }
                                }) {
                                SetupNavGraph(
                                    navController,
                                    peekHeight + paddingValues.calculateBottomPadding(),
                                    mediaController
                                )
                            }
                        } else {
                            SetupNavGraph(
                                navController,
                                0.dp,
                                mediaController
                            )
                        }
                    }
                }

                var showNoProvidersDialog by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    showNoProvidersDialog = MediaProviderManager.allProviders.value.isEmpty()
                }

                if (showNoProvidersDialog) {
                    if (isTv) {
                        OnboardingDialog { showNoProvidersDialog = it }
                    } else {
                        com.craftworks.music.ui.elements.dialogs.OnboardingDialog() { showNoProvidersDialog = it }
//                        NoMediaProvidersDialog(
//                            setShowDialog = { showNoProvidersDialog = true },
//                            navController
//                        )
                    }
                }
            }
        }

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach { permission ->
                Log.d(
                    "PERMISSIONS", "Is '${permission.key}' permission granted? ${permission.value}"
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.ACCESS_LOCAL_NETWORK
                )
            )
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }

        createNotificationChannel(
            this,
            "download_channel",
            R.string.notification_download_name,
            R.string.notification_download_desc,
            IMPORTANCE_LOW
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvSideNavigation(
    navController: NavHostController,
    mediaController: MediaController?,
    content: @Composable () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val (home, albums, songs, artists, radios, playlists, settings) = remember { FocusRequester.createRefs() }
    val currentRoute by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)

    val orderedNavItems = AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(
        initial = listOf(
            BottomNavItem(
                "Home", R.drawable.rounded_home_24, Screen.Home
            ),
            BottomNavItem(
                stringResource((R.string.nav_albums)), R.drawable.rounded_library_music_24, Screen.Albums
            ),
            BottomNavItem(
                stringResource((R.string.nav_songs)), R.drawable.round_music_note_24, Screen.Songs, false
            ),
            BottomNavItem(
                stringResource((R.string.nav_artists)), R.drawable.rounded_artist_24, Screen.Artists
            ),
            BottomNavItem(
                stringResource((R.string.nav_radios)), R.drawable.rounded_radio, Screen.Radios
            ),
            BottomNavItem(
                stringResource((R.string.nav_playlists)), R.drawable.placeholder, Screen.Playlists
            ),
        )
    ).value

    NavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp)
                    .focusProperties {
                        enter = {
                            when (currentRoute) {
                                Screen.Home -> home
                                Screen.Albums -> albums
                                Screen.Songs -> songs
                                Screen.Artists -> artists
                                Screen.Radios -> radios
                                Screen.Playlists -> playlists
                                Screen.Settings -> settings
                                else -> FocusRequester.Default
                            }
                        }
                    }
                    .focusGroup(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavigationDrawerItem(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    selected = backStackEntry?.destination.isRouteSelected(Screen.Search),
                    onClick = {
                        if (!backStackEntry?.destination.isRouteSelected(Screen.Search)) {
                            navController.navigate(Screen.Search) {
                                launchSingleTop = true
                                restoreState = true
//                                popUpTo(navController.graph.startDestinationId) {
//                                    saveState = true
//                                }
                            }
                        }
                    },
                    leadingContent = {
                        androidx.tv.material3.Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                ) {
                    androidx.tv.material3.Text(text = stringResource(R.string.nav_search))
                }

                orderedNavItems.forEach { item ->
                    if (!item.enabled) return@forEach

                    val isSelected = backStackEntry?.destination.isRouteSelected(item.screenRoute)
                    NavigationDrawerItem(
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .focusRequester(
                                when (item.screenRoute) {
                                    Screen.Home -> home
                                    Screen.Albums -> albums
                                    Screen.Songs -> songs
                                    Screen.Artists -> artists
                                    Screen.Radios -> radios
                                    Screen.Playlists -> playlists
                                    Screen.Settings -> settings
                                    else -> FocusRequester.Default
                                }
                            ),
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(item.screenRoute) {
                                    launchSingleTop = true
                                    restoreState = true
//                                    popUpTo(navController.graph.startDestinationId) {
//                                        saveState = true
//                                    }
                                }
                            }
                        },
                        leadingContent = {
                            androidx.tv.material3.Icon(
                                imageVector = ImageVector.vectorResource(item.icon),
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    ) {
                        androidx.tv.material3.Text(text = item.title)
                    }
                }

                val isPlayingSelected =
                    backStackEntry?.destination.isRouteSelected(Screen.NowPlayingLandscape)

                var isPlayingVisible by remember { mutableStateOf(mediaController?.currentMediaItem != null) }
                LaunchedEffect(mediaController?.mediaMetadata) {
                    isPlayingVisible = mediaController?.currentMediaItem != null
                }

                if (isPlayingVisible) {
                    NavigationDrawerItem(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                        selected = isPlayingSelected,
                        onClick = {
                            if (!isPlayingSelected) {
                                navController.navigate(Screen.NowPlayingLandscape) {
                                    launchSingleTop = true
                                    restoreState = true
//                                    popUpTo(navController.graph.startDestinationId) {
//                                        saveState = true
//                                    }
                                }
                            }
                        },
                        leadingContent = {
                            androidx.tv.material3.Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.s_m_playback),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    ) {
                        androidx.tv.material3.Text(text = stringResource(R.string.nav_playing))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    NavigationDrawerItem(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                        selected = backStackEntry?.destination.isRouteSelected(Screen.Settings),
                        onClick = {
                            if (!backStackEntry?.destination.isRouteSelected(Screen.Settings))
                                navController.navigate(Screen.Settings) {
                                    launchSingleTop = true
                                    restoreState = true
//                                    popUpTo(navController.graph.startDestinationId) {
//                                        saveState = true
//                                    }
                                }
                        },
                        leadingContent = {
                            androidx.tv.material3.Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_settings_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    ) {
                        androidx.tv.material3.Text(text = stringResource(R.string.home_settings))
                    }
                    /*
                    NavigationDrawerItem(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                        selected = false,
                        onClick = {
                            (context as Activity).finish()
                            exitProcess(0)
                        },
                        leadingContent = {
                            androidx.tv.material3.Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.round_power_settings_new_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            focusedContainerColor = androidx.tv.material3.MaterialTheme.colorScheme.errorContainer,
                            focusedContentColor = androidx.tv.material3.MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        androidx.tv.material3.Text(text = stringResource(R.string.Action_Exit))
                    }
                    */
                }
            }
        },
        content = {
            Box(Modifier
                .focusRestorer()
                .focusGroup()
            ) {
                content()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Stable
fun AnimatedBottomNavBar(
    navController: NavHostController, scaffoldState: BottomSheetScaffoldState,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()

    val orderedNavItems = AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(
        initial = listOf(
            BottomNavItem(
                stringResource(R.string.nav_home), R.drawable.rounded_home_24, Screen.Home
            ), BottomNavItem(
                stringResource(R.string.nav_albums), R.drawable.rounded_library_music_24, Screen.Albums
            ), BottomNavItem(
                stringResource(R.string.nav_songs), R.drawable.round_music_note_24, Screen.Songs
            ), BottomNavItem(
                stringResource(R.string.nav_artists), R.drawable.rounded_artist_24, Screen.Artists
            ), BottomNavItem(
                stringResource(R.string.nav_radios), R.drawable.rounded_radio, Screen.Radios
            ), BottomNavItem(
                stringResource(R.string.nav_playlists), R.drawable.placeholder, Screen.Playlists
            )
        )
    ).value

    orderedNavItems.first {it.screenRoute == Screen.Radios}.enabled =
        currentProvider?.featureFlags?.contains(ProviderFeature.INTERNET_RADIO) ?: false
    orderedNavItems.first {it.screenRoute == Screen.Playlists}.enabled =
        currentProvider?.featureFlags?.contains(ProviderFeature.PLAYLISTS) ?: false

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        val expanded by remember { derivedStateOf { scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded } }

        val yTrans by animateIntAsState(
            targetValue = if (expanded) dpToPx(
                -80 - WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding().value.toInt()
            )
            else 0, label = "Fullscreen Translation"
        )

        NavigationBar(modifier = Modifier.offset { IntOffset(x = 0, y = -yTrans) }) {
            orderedNavItems.forEachIndexed { _, item ->
                if (!item.enabled) return@forEachIndexed

                NavigationBarItem(
                    selected = backStackEntry?.destination.isRouteSelected(item.screenRoute),
                    onClick = {
                        if (backStackEntry?.destination.isRouteSelected(item.screenRoute)) return@NavigationBarItem
                        navController.navigate(item.screenRoute) {
                            launchSingleTop = true
                        }
                        coroutineScope.launch {
                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                    label = { Text(text = item.title) },
                    alwaysShowLabel = false,
                    icon = {
                        Icon(ImageVector.vectorResource(item.icon), contentDescription = null)
                    })
            }
            if (LocalWindowInfo.current.containerSize.width > dpToPx(640))
                NavigationBarItem(
                    selected = backStackEntry?.destination.isRouteSelected(Screen.NowPlayingLandscape),
                    onClick = {
                        if (backStackEntry?.destination.isRouteSelected(Screen.NowPlayingLandscape)) return@NavigationBarItem
                        navController.navigate(Screen.NowPlayingLandscape) {
                            launchSingleTop = true
                        }
                        coroutineScope.launch {
                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                    label = { Text(text = stringResource(R.string.nav_playing)) },
                    alwaysShowLabel = false,
                    icon = {
                        Icon(
                            ImageVector.vectorResource(R.drawable.s_m_playback),
                            contentDescription = null
                        )
                    },
                )
        }
    } else {
        val lazyColumnState = rememberLazyListState()
        NavigationRail {
            LazyColumn(
                state = lazyColumnState,
                modifier = Modifier
                    .weight(1f)
                    .verticalFadingEdges(
                        FadingEdgesContentType.Dynamic.Lazy.List(
                            FadingEdgesScrollConfig.Dynamic(), lazyColumnState
                        ), FadingEdgesGravity.All, 64.dp
                    )
            ) {
                items(orderedNavItems) { item ->
                    if (!item.enabled) return@items

                    NavigationRailItem(
                        selected = backStackEntry?.destination.isRouteSelected(item.screenRoute),
                        onClick = {
                            if (backStackEntry?.destination.isRouteSelected(item.screenRoute))
                                return@NavigationRailItem

                            navController.navigate(item.screenRoute) {
                                launchSingleTop = true
                            }
                            coroutineScope.launch {
                                if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) scaffoldState.bottomSheetState.partialExpand()
                            }
                        },
                        label = { Text(text = item.title) },
                        alwaysShowLabel = false,
                        icon = {
                            Icon(ImageVector.vectorResource(item.icon), contentDescription = null)
                        },
                    )
                }
                item {
                    NavigationRailItem(
                        selected = backStackEntry?.destination.isRouteSelected(Screen.NowPlayingLandscape),
                        onClick = {
                            if (backStackEntry?.destination.isRouteSelected(Screen.NowPlayingLandscape))
                                return@NavigationRailItem

                            navController.navigate(Screen.NowPlayingLandscape) {
                                launchSingleTop = true
                            }
                            coroutineScope.launch {
                                if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
                                    scaffoldState.bottomSheetState.partialExpand()
                            }
                        },
                        label = { Text(text = stringResource(R.string.nav_playing)) },
                        alwaysShowLabel = false,
                        icon = {
                            Icon(
                                ImageVector.vectorResource(R.drawable.s_m_playback),
                                contentDescription = null
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun NavDestination?.isRouteSelected(route: Screen): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true

