package com.craftworks.music

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.SongHelper.Companion.initPlayer
import com.craftworks.music.data.SyncedLyric
import com.craftworks.music.data.bottomNavigationItems
import com.craftworks.music.ui.NowPlayingContent
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.launch
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


var sliderPos = mutableIntStateOf(0)
var repeatSong = mutableStateOf(false)
var shuffleSongs = mutableStateOf(false)

var songState by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    lateinit var navController: NavHostController


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timer().scheduleAtFixedRate(object : TimerTask() {
            private val handler = Handler(Looper.getMainLooper())
            override fun run() {
                handler.post {
                    try {
                        if (SongHelper.isSeeking || SongHelper.player.isPlaying || !SongHelper.player.isLoading){
                            SongHelper.updateCurrentPos()
                        }

                        /* SYNCED LYRICS */
                        for (a in 0 until SyncedLyric.size - 1) { //Added 750ms offset
                            if (SyncedLyric[a].timestamp <= SongHelper.currentPosition + 750 &&
                                SyncedLyric[a + 1].timestamp >= SongHelper.currentPosition + 750) {

                                    SyncedLyric[a] = SyncedLyric[a].copy(isCurrentLyric = true)
                                    SyncedLyric.forEachIndexed { index, syncedLyric ->
                                        if (index != a) {
                                            SyncedLyric[index] = syncedLyric.copy(isCurrentLyric = false)
                                        }
                                    }
                            }
                        }
                    }catch (_: Exception){

                    }
                }
            }
        }, 0, 1000)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val scaffoldState = BottomSheetScaffoldState(
            bottomSheetState = SheetState(
                skipPartiallyExpanded = false,
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true
            ),
            snackbarHostState = SnackbarHostState()
        )
        val snackbarHostState = SnackbarHostState()

        setContent {

            MusicPlayerTheme {
                // BOTTOM NAVIGATION + NOW-PLAYING UI
                navController = rememberNavController()



                /*
                val bottomNavigationItems = listOf(
                    BottomNavigationItem(
                        title = "Home",
                        selectedIcon = ImageVector.vectorResource(R.drawable.rounded_home_24),
                        unselectedIcon = ImageVector.vectorResource(R.drawable.rounded_home_24),
                        screenRoute = "home_screen"
                    ),
                    BottomNavigationItem(
                        title = "Albums",
                        selectedIcon = ImageVector.vectorResource(R.drawable.rounded_library_music_24),
                        unselectedIcon = ImageVector.vectorResource(R.drawable.rounded_library_music_24),
                        screenRoute = "album_screen"
                    ),
                    BottomNavigationItem(
                        title = "Songs",
                        selectedIcon = ImageVector.vectorResource(R.drawable.round_music_note_24),
                        unselectedIcon = ImageVector.vectorResource(R.drawable.round_music_note_24),
                        screenRoute = "songs_screen"
                    ),
                    BottomNavigationItem(
                        title = "Radio",
                        selectedIcon = ImageVector.vectorResource(R.drawable.rounded_radio),
                        unselectedIcon = ImageVector.vectorResource(R.drawable.rounded_radio),
                        screenRoute = "radio_screen"
                    ),
                    BottomNavigationItem(
                        title = "Playlists",
                        selectedIcon = ImageVector.vectorResource(R.drawable.placeholder),
                        unselectedIcon = ImageVector.vectorResource(R.drawable.placeholder),
                        screenRoute = "playlist_screen"
                    )
                )
                */

                var selectedItemIndex by rememberSaveable{ mutableIntStateOf(0) }


                val coroutineScope = rememberCoroutineScope()

                val yTrans by animateDpAsState(
                    targetValue = if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) ((-80).dp - WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()) else 0.dp , label = "Fullscreen Translation")

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            val backStackEntry = navController.currentBackStackEntryAsState()
                            if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE){
                                NavigationBar (modifier = Modifier
                                    .offset(y = -yTrans)) {
                                    bottomNavigationItems.forEachIndexed { index, item ->
                                        if (!item.enabled) return@forEachIndexed
                                        NavigationBarItem(
                                            selected = item.screenRoute == backStackEntry.value?.destination?.route,
                                            modifier = Modifier.bounceClick(),
                                            onClick = {
                                                selectedItemIndex = index
                                                navController.navigate(item.screenRoute) {
                                                    launchSingleTop = true
                                                }
                                                coroutineScope.launch {
                                                    scaffoldState.bottomSheetState.partialExpand()
                                                } },
                                            label = { Text(text = item.title) },
                                            alwaysShowLabel = false,
                                            icon = {
                                                Icon(ImageVector.vectorResource(item.icon),contentDescription = item.title)
                                            }
                                        )
                                    }
                                }
                            }
                            else{
                                NavigationRail {
                                    bottomNavigationItems.forEachIndexed { index, item ->
                                        if (!item.enabled) return@forEachIndexed
                                        NavigationRailItem(
                                            selected = item.screenRoute == backStackEntry.value?.destination?.route,
                                            onClick = {
                                                selectedItemIndex = index
                                                navController.navigate(item.screenRoute) {
                                                    launchSingleTop = true
                                                }
                                                coroutineScope.launch {
                                                    scaffoldState.bottomSheetState.partialExpand()
                                                } },
                                            label = { Text(text = item.title) },
                                            alwaysShowLabel = false,
                                            icon = {
                                                Icon(ImageVector.vectorResource(item.icon),contentDescription = item.title)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        paddingValues -> SetupNavGraph(navController = navController, paddingValues)
                        BottomSheetScaffold(
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredWidth(LocalConfiguration.current.screenWidthDp.dp),
                            sheetContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                            sheetPeekHeight =
                            if (SongHelper.currentSong.title == "" &&
                                SongHelper.currentSong.duration == 0)
                                0.dp // Hide Mini-player if empty
                            else if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE){
                                72.dp + 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            }
                            else {
                                72.dp
                            },
                            sheetShadowElevation = 4.dp,
                            sheetShape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
                            sheetDragHandle = { },
                            scaffoldState = scaffoldState,
                            sheetContent = {
                                NowPlayingContent(
                                    context = this@MainActivity,
                                    scaffoldState = scaffoldState,
                                    snackbarHostState = snackbarHostState,
                                    navHostController = navController
                                )
                            }) {
                        }
                    }
                }
            }

            PlayPause()
        }

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                Log.d("PERMISSIONS", "READ_MEDIA_AUDIO: $isGranted")
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        }
        else {
            requestPermissionLauncher.launch(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        // SAVE SETTINGS ON APP EXIT
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
                initPlayer(this@MainActivity)
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityPreStopped(activity: Activity) {
                saveManager(this@MainActivity).saveSettings()
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                SongHelper.releasePlayer()
            }
        })

    }
}

@Composable
fun PlayPause() {
    if (songState && !SongHelper.player.isPlaying) {
        SongHelper.player.playWhenReady = true
    } else {
        SongHelper.pauseStream()
    }
}

fun formatMilliseconds(milliseconds: Float): String {
    val format = SimpleDateFormat("mm:ss", Locale.getDefault())
    return format.format(Date(milliseconds.toLong()))
}
fun Modifier.fadingEdge(brush: Brush) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
