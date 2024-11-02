package com.craftworks.music

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.ComponentCaller
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.data.Screen
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.dialogs.NoMediaProvidersDialog
import com.craftworks.music.ui.playing.NowPlayingContent
import com.craftworks.music.ui.playing.NowPlayingMiniPlayer
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.launch
import java.util.Locale


var sliderPos = mutableIntStateOf(0)
var repeatSong = mutableStateOf(false)
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
        handleSearchIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val scaffoldState = BottomSheetScaffoldState(
            bottomSheetState = SheetState(
                skipPartiallyExpanded = false,
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
                density = Density(this)
            ),
            snackbarHostState = SnackbarHostState()
        )

        setContent {
            MusicPlayerTheme {
                // BOTTOM NAVIGATION + NOW-PLAYING UI
                navController = rememberNavController()
                val mediaController = rememberManagedMediaController()

                println("Recomposing EVERYTHING!!!!! VERY BAD")

                // Set background color to colorScheme.background
                window.decorView.setBackgroundColor(
                    MaterialTheme.colorScheme.background.toArgb()
                )

                Scaffold(
                    bottomBar = {
                        AnimatedBottomNavBar(navController, scaffoldState)
                    },
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    containerColor = Color.Transparent // So we can use transparent here. this eliminates 1 level of overdraw.
                ) { paddingValues ->
                    SetupNavGraph(navController, paddingValues, mediaController.value)

                    Log.d("RECOMPOSITION", "Recomposing scaffold!")

                    // No BottomSheetScaffold for Android TV
                    if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION) &&
                        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
                    ) {
                        BottomSheetScaffold(
                            sheetContainerColor = Color.Transparent,
                            containerColor = Color.Transparent,
                            sheetPeekHeight =
                            if (SongHelper.currentSong.title == "" &&
                                SongHelper.currentSong.duration == 0 &&
                                SongHelper.currentSong.imageUrl == ""
                            )
                                0.dp // Hide Mini-player if empty
                            else if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                                72.dp + 80.dp + WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding()
                            } else 72.dp,
                            sheetShadowElevation = 4.dp,
                            sheetShape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
                            sheetDragHandle = { },
                            scaffoldState = scaffoldState,
                            sheetContent = {
                                Log.d("RECOMPOSITION", "Recomposing SheetContent!")
                                Box {
                                    val coroutineScope = rememberCoroutineScope()

                                    NowPlayingMiniPlayer(scaffoldState, mediaController.value) {
                                        coroutineScope.launch {
                                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded) scaffoldState.bottomSheetState.expand()
                                            else scaffoldState.bottomSheetState.partialExpand()
                                        }
                                    }

                                    NowPlayingContent(
                                        context = this@MainActivity,
                                        navHostController = navController,
                                        mediaController = mediaController.value
                                    )
                                }
                            }) {
                        }
                    }

                    if (showNoProviderDialog.value)
                        NoMediaProvidersDialog(
                            setShowDialog = { showNoProviderDialog.value = it },
                            navController
                        )
                }
            }
        }

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                Log.d("PERMISSIONS", "Is 'READ_MEDIA_AUDIO' permission granted? $isGranted")
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            requestPermissionLauncher.launch(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        // SAVE SETTINGS ON APP EXIT
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
                ChoraMediaLibraryService().onDestroy()
                println("Destroyed, Goodbye :(")
            }
        })
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        handleSearchIntent(intent)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun handleSearchIntent(intent: Intent) {
        if (intent.action == "android.media.action.MEDIA_PLAY_FROM_SEARCH") {
            val query = intent.getStringExtra("query")

            val mediaLibraryService = ChoraMediaLibraryService()

            query?.let { searchQuery ->
                //mediaLibraryService.searchAndPlayMedia(searchQuery)
            }
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
                            popUpTo(navController.graph.findStartDestination().id)
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
        NavigationRail {
            orderedNavItems.forEach { item ->
                if (!item.enabled) return@forEach
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
            NavigationRailItem(
                selected = Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route,
                onClick = {
                    if (Screen.NowPlayingLandscape.route == backStackEntry?.destination?.route) return@NavigationRailItem
                    navController.navigate(Screen.NowPlayingLandscape.route) {
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id)
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