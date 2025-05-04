package com.craftworks.music

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.data.Screen
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.dialogs.NoMediaProvidersDialog
import com.craftworks.music.ui.playing.NowPlayingContent
import com.craftworks.music.ui.playing.NowPlayingMiniPlayer
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.theme.MusicPlayerTheme
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.verticalFadingEdges
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.system.exitProcess

var sliderPos = mutableIntStateOf(0)
var shuffleSongs = mutableStateOf(false)

var showNoProviderDialog = mutableStateOf(false)

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    lateinit var navController: NavHostController

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(applicationContext, ChoraMediaLibraryService::class.java)
        this@MainActivity.startService(serviceIntent)

        //handleSearchIntent(intent)

        enableEdgeToEdge()

        val homeViewModel = HomeScreenViewModel()
        val albumViewModel = AlbumScreenViewModel()
        val songsViewModel = SongsScreenViewModel()
        val artistsViewModel = ArtistsScreenViewModel()
        val playlistViewModel = PlaylistScreenViewModel()

        homeViewModel.reloadData()
        albumViewModel.reloadData()
        songsViewModel.reloadData()
        artistsViewModel.reloadData()
        playlistViewModel.reloadData()

        setContent {
            MusicPlayerTheme {
                navController = rememberNavController()

                val mediaController by rememberManagedMediaController()
                var metadata by remember { mutableStateOf<MediaMetadata?>(null) }

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

                val scaffoldState = remember {
                    BottomSheetScaffoldState(
                        bottomSheetState = SheetState(
                            skipPartiallyExpanded = false,
                            initialValue = SheetValue.PartiallyExpanded,
                            skipHiddenState = true,
                            density = Density(this)
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
                        AnimatedBottomNavBar(navController, scaffoldState)
                    },
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    containerColor = Color.Transparent
                ) { paddingValues ->
                    if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION) && LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
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

                                    NowPlayingContent(
                                        context = this@MainActivity,
                                        mediaController = mediaController,
                                        metadata = metadata
                                    )
                                }

                                val currentView = LocalView.current
                                DisposableEffect(scaffoldState.bottomSheetState.currentValue) {
                                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
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
                            SetupNavGraph(
                                navController,
                                peekHeight + paddingValues.calculateBottomPadding(),
                                mediaController,
                                homeViewModel,
                                albumViewModel,
                                songsViewModel,
                                artistsViewModel,
                                playlistViewModel
                            )
                        }
                    } else {
                        SetupNavGraph(
                            navController,
                            0.dp,
                            mediaController,
                            homeViewModel,
                            albumViewModel,
                            songsViewModel,
                            artistsViewModel,
                            playlistViewModel
                        )
                    }
                }

                if (showNoProviderDialog.value) NoMediaProvidersDialog(
                    setShowDialog = { showNoProviderDialog.value = it }, navController
                )
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
            registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }
                override fun onActivityStarted(activity: Activity) { }
                override fun onActivityResumed(activity: Activity) { }
                override fun onActivityPaused(activity: Activity) { }
                override fun onActivityPreStopped(activity: Activity) { }
                override fun onActivityStopped(activity: Activity) { }
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { }

                @androidx.annotation.OptIn(UnstableApi::class)
                override fun onActivityDestroyed(activity: Activity) {
                    ChoraMediaLibraryService.getInstance()?.saveState()

                    this@MainActivity.stopService(serviceIntent)
                    println("Destroyed, Goodbye :(")
                }
            })
        }
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

    val orderedNavItems = SettingsManager(context).bottomNavItemsFlow.collectAsState(
        initial = //region Default Items
        listOf(
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
        //endregion
    ).value

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
                    selected = item.screenRoute == backStackEntry?.destination?.route,
                    modifier = Modifier.bounceClick(),
                    onClick = {
                        if (item.screenRoute == backStackEntry?.destination?.route) return@NavigationBarItem
                        navController.navigate(item.screenRoute) {
                            launchSingleTop = true
                            //popUpTo(navController.graph.findStartDestination().id)
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
                                //restoreState = true
                                popUpTo(navController.graph.findStartDestination().id)
//                            {
//                                saveState = true
//                            }
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
                                //popUpTo(navController.graph.findStartDestination().id)
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
    //val format = SimpleDateFormat("mm:ss", Locale.getDefault())
    //return format.format(Date(milliseconds.toLong()))
    return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
}

fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }