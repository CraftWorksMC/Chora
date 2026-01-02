package com.craftworks.music

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.OnboardingSettingsManager
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.elements.dialogs.NoMediaProvidersDialog
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
import java.util.Locale
import kotlin.system.exitProcess
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.craftworks.music.ui.elements.DownloadQueueModal
import com.craftworks.music.ui.elements.FloatingDownloadIndicator
import com.craftworks.music.ui.elements.FloatingSyncIndicator
import com.craftworks.music.ui.viewmodels.DownloadViewModel
import com.craftworks.music.ui.viewmodels.SyncIndicatorViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided")
}
val LocalFoldingFeatures = staticCompositionLocalOf<List<FoldingFeature>> {
    emptyList()
}

var showNoProviderDialog = mutableStateOf(false)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    lateinit var navController: NavHostController
    private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private lateinit var serviceIntent: Intent

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceIntent = Intent(applicationContext, ChoraMediaLibraryService::class.java)
        this@MainActivity.startService(serviceIntent)

        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            var foldingFeatures by remember { mutableStateOf<List<FoldingFeature>>(emptyList()) }

            // Track folding features for foldable devices
            LaunchedEffect(Unit) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { layoutInfo ->
                        foldingFeatures = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>()
                    }
            }

            CompositionLocalProvider(
                LocalWindowSizeClass provides windowSizeClass,
                LocalFoldingFeatures provides foldingFeatures
            ) {
            MusicPlayerTheme {
                navController = rememberNavController()

                // Check onboarding state
                val onboardingSettingsManager = remember { OnboardingSettingsManager(this@MainActivity) }
                val showOnboarding by onboardingSettingsManager.shouldShowOnboardingFlow.collectAsState(initial = false)

                val mediaController by rememberManagedMediaController()
                var metadata by remember { mutableStateOf<MediaMetadata?>(null) }

                // Download Manager
                val downloadViewModel: DownloadViewModel = hiltViewModel()
                var showDownloadModal by remember { mutableStateOf(false) }

                // Sync Indicator
                val syncIndicatorViewModel: SyncIndicatorViewModel = hiltViewModel()
                val isSyncing by syncIndicatorViewModel.isSyncing.collectAsStateWithLifecycle()
                val isPaused by syncIndicatorViewModel.isPaused.collectAsStateWithLifecycle()

                val coroutineScope = rememberCoroutineScope()

                // Update metadata from mediaController.
                LaunchedEffect(mediaController) {
                    if (mediaController?.currentMediaItem != null) {
                        metadata = mediaController?.currentMediaItem?.mediaMetadata
                    }
                }
                DisposableEffect(mediaController) {
                    val listener = object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            super.onMediaItemTransition(mediaItem, reason)
                            metadata = mediaController?.currentMediaItem?.mediaMetadata
                        }
                    }

                    mediaController?.addListener(listener)

                    onDispose {
                        mediaController?.removeListener(listener)
                    }
                }
                // Set background color to colorScheme.background
                window.decorView.setBackgroundColor(
                    MaterialTheme.colorScheme.background.toArgb()
                )

                val positionalThreshold = dpToPx(56).toFloat()
                val velocityThreshold = dpToPx(125).toFloat()

                // Compute layout mode variables early so they can be used for scaffoldState keying
                val configuration = LocalConfiguration.current
                val isCompactWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
                val isTV = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
                val isTableTopMode = foldingFeatures.any { it.state == FoldingFeature.State.HALF_OPENED && it.orientation == FoldingFeature.Orientation.HORIZONTAL }
                // Show bottom sheet on all non-TV devices (including foldables when unfolded)
                val useBottomSheet = !isTV && !isTableTopMode && !showOnboarding

                // Key scaffoldState on useBottomSheet to reset sheet state when switching
                // between bottom sheet layout and non-bottom-sheet layout (e.g., fold/unfold)
                val scaffoldState = remember(useBottomSheet) {
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

                Scaffold(
                    bottomBar = {
                        // Hide bottom nav during onboarding
                        if (!showOnboarding) {
                            AnimatedBottomNavBar(navController, scaffoldState)
                        }
                    },
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    containerColor = Color.Transparent
                ) { paddingValues ->
                    if (useBottomSheet) {
                        BottomSheetScaffold(
                            sheetContainerColor = Color.Transparent,
                            containerColor = Color.Transparent,
                            sheetPeekHeight = peekHeight + 80.dp + WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding(),
                            //sheetShadowElevation = 6.dp,
                            sheetShape = RectangleShape,
                            sheetDragHandle = { },
                            scaffoldState = scaffoldState,
                            sheetContent = {
                                val coroutineScope = rememberCoroutineScope()
                                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                                val isExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded

                                // Handle back press to close sheet when expanded
                                BackHandler(enabled = isExpanded) {
                                    coroutineScope.launch {
                                        scaffoldState.bottomSheetState.partialExpand()
                                    }
                                }

                                // Animated top gap - only when expanded
                                val topGap by animateDpAsState(
                                    targetValue = if (isExpanded) screenHeight * 0.06f else 0.dp,
                                    animationSpec = tween(300),
                                    label = "topGapAnimation"
                                )

                                // Animated horizontal padding - 4dp always to match navbar
                                val horizontalPadding by animateDpAsState(
                                    targetValue = 4.dp,
                                    animationSpec = tween(300),
                                    label = "horizontalPaddingAnimation"
                                )

                                // Animated corner radius - bottom corners 0 when collapsed
                                val bottomCornerRadius by animateDpAsState(
                                    targetValue = if (isExpanded) 12.dp else 0.dp,
                                    animationSpec = tween(300),
                                    label = "bottomCornerAnimation"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent)
                                        .padding(start = horizontalPadding, end = horizontalPadding, top = topGap)
                                        .clip(RoundedCornerShape(12.dp, 12.dp, bottomCornerRadius, bottomCornerRadius))
                                ) {
                                    NowPlayingContent(
                                        mediaController = mediaController,
                                        metadata = metadata
                                    )

                                    // Drag handle on top of card - only when expanded
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isExpanded,
                                        modifier = Modifier.align(Alignment.TopCenter)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(32.dp)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(alpha = 0.5f))
                                            )
                                        }
                                    }

                                    NowPlayingMiniPlayer(
                                        scaffoldState = scaffoldState,
                                        metadata = metadata,
                                        mediaController = mediaController,
                                        onClick = {
                                            coroutineScope.launch {
                                                scaffoldState.bottomSheetState.expand()
                                            }
                                        })
                                }

                                val currentView = LocalView.current
                                DisposableEffect(scaffoldState.bottomSheetState.targetValue) {
                                    if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) {
                                        currentView.keepScreenOn = true
                                        Log.d("NOW-PLAYING", "KeepScreenOn: True")
                                    } else {
                                        currentView.keepScreenOn = false
                                        Log.d("NOW-PLAYING", "KeepScreenOn: False")
                                    }

                                    onDispose {
                                        currentView.keepScreenOn = false
                                        Log.d("NOW-PLAYING", "KeepScreenOn: False")
                                    }
                                }
                            }) {
                            // Don't add peekHeight - let content scroll behind transparent miniplayer
                            SetupNavGraph(
                                navController,
                                paddingValues.calculateBottomPadding(),
                                mediaController,
                                showOnboarding
                            )
                        }
                    } else {
                        SetupNavGraph(
                            navController,
                            0.dp,
                            mediaController,
                            showOnboarding
                        )
                    }
                }

                // Suppress no-provider dialog during onboarding
                if (showNoProviderDialog.value && !showOnboarding) NoMediaProvidersDialog(
                    setShowDialog = { showNoProviderDialog.value = it }, navController
                )

                // Floating Download Indicator - hide during onboarding
                // Adjust bottom padding based on whether the mini player is visible
                if (!showOnboarding) {
                    val hasMiniPlayer = useBottomSheet && metadata?.title != null
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        FloatingDownloadIndicator(
                            activeDownloads = downloadViewModel.activeDownloads,
                            onClick = { showDownloadModal = true },
                            modifier = Modifier
                                .padding(end = 16.dp, bottom = if (hasMiniPlayer) 160.dp else 96.dp)
                        )
                    }
                }

                // Floating Sync Indicator - hide during onboarding
                if (!showOnboarding) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        FloatingSyncIndicator(
                            isSyncing = isSyncing || isPaused,
                            onClick = {
                                navController.navigate(Screen.S_Data.route) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 16.dp)
                        )
                    }
                }

                // Download Queue Modal
                if (showDownloadModal) {
                    DownloadQueueModal(
                        activeDownloads = downloadViewModel.activeDownloads,
                        completedDownloads = downloadViewModel.completedDownloads,
                        failedDownloads = downloadViewModel.failedDownloads,
                        onDismiss = { showDownloadModal = false },
                        onCancelDownload = downloadViewModel::cancelDownload,
                        onPauseDownload = downloadViewModel::pauseDownload,
                        onResumeDownload = downloadViewModel::resumeDownload,
                        onRetryDownload = downloadViewModel::retryDownload,
                        onDeleteDownload = downloadViewModel::deleteDownload,
                        onClearCompleted = downloadViewModel::clearCompleted
                    )
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        // SAVE SETTINGS ON APP EXIT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }
                override fun onActivityStarted(activity: Activity) { }
                override fun onActivityResumed(activity: Activity) { }
                override fun onActivityPaused(activity: Activity) { }
                override fun onActivityPreStopped(activity: Activity) { }
                override fun onActivityStopped(activity: Activity) { }
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }

                @androidx.annotation.OptIn(UnstableApi::class)
                override fun onActivityDestroyed(activity: Activity) {
                    if (activity === this@MainActivity) {
                        ChoraMediaLibraryService.getInstance()?.saveState()
                        this@MainActivity.stopService(serviceIntent)
                        println("Destroyed, Goodbye :(")
                    }
                }
            }
            activityLifecycleCallbacks?.let { registerActivityLifecycleCallbacks(it) }
        }
    }

    override fun onDestroy() {
        // Unregister lifecycle callbacks to prevent memory leak
        activityLifecycleCallbacks?.let {
            unregisterActivityLifecycleCallbacks(it)
            activityLifecycleCallbacks = null
        }
        super.onDestroy()
    }
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

    val orderedNavItems = AppearanceSettingsManager(context).bottomNavItemsFlow.collectAsState(
        initial = listOf(
            BottomNavItem(
                "Home", R.drawable.rounded_home_24, "home_screen"
            ), BottomNavItem(
                "Albums", R.drawable.rounded_library_music_24, "album_screen"
            ), BottomNavItem(
                "Songs", R.drawable.round_music_note_24, "songs_screen"
            ), BottomNavItem(
                "Artists", R.drawable.rounded_artist_24, "artists_screen"
            ), BottomNavItem(
                "Radios", R.drawable.rounded_radio, "radio_screen"
            ), BottomNavItem(
                "Playlists", R.drawable.placeholder, "playlist_screen"
            )
        )
    ).value

    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        val expanded by remember { derivedStateOf { scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded } }

        val yTrans by animateFloatAsState(
            targetValue = if (expanded) -dpToPx(
                80 + WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding().value.toInt()
            ).toFloat()
            else 0f, label = "Fullscreen Translation"
        )

        NavigationBar(
            modifier = Modifier
                .graphicsLayer {
                    translationY = -yTrans
                }
                .padding(horizontal = 4.dp)
        ) {
            orderedNavItems.forEachIndexed { _, item ->
                if (!item.enabled) return@forEachIndexed
                NavigationBarItem(
                    selected = item.screenRoute == backStackEntry?.destination?.route,
                    //modifier = Modifier.bounceClick(),
                    onClick = {
                        if (item.screenRoute == backStackEntry?.destination?.route) return@NavigationBarItem
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
                        Icon(ImageVector.vectorResource(item.icon), contentDescription = item.title)
                    })
            }
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
                        selected = item.screenRoute == backStackEntry?.destination?.route,
                        onClick = {
                            if (item.screenRoute == backStackEntry?.destination?.route) return@NavigationRailItem
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
                            Icon(ImageVector.vectorResource(item.icon), contentDescription = item.title)
                        },
                    )
                }
                item {
                    NavigationRailItem(
                        selected = Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route,
                        onClick = {
                            if (Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route) return@NavigationRailItem
                            navController.navigate(Screen.NowPlayingLandscape.route) {
                                launchSingleTop = true
                            }
                            coroutineScope.launch {
                                if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) scaffoldState.bottomSheetState.partialExpand()
                            }
                        },
                        label = { Text(text = "Playing") },
                        alwaysShowLabel = false,
                        icon = {
                            Icon(
                                ImageVector.vectorResource(R.drawable.s_m_playback),
                                contentDescription = "Playing"
                            )
                        },
                    )
                }
            }
            // Show the exit button only on TV
            if (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) {
                NavigationRailItem(
                    selected = false,
                    onClick = {
                        (context as Activity).finish()
                        exitProcess(0)
                    },
                    label = { Text(stringResource(R.string.Action_Exit)) },
                    alwaysShowLabel = false,
                    icon = {
                        Icon(
                            ImageVector.vectorResource(R.drawable.round_power_settings_new_24),
                            contentDescription = "Exit App"
                        )
                    },
                )
            }
        }
    }
}

fun formatMilliseconds(seconds: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
}

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
