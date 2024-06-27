package com.craftworks.music

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.data.bottomNavigationItems
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.NowPlayingContent
import com.craftworks.music.ui.dpToPx
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.dialogs.CreateMediaProviderDialog
import com.craftworks.music.ui.elements.dialogs.NoMediaProvidersDialog
import com.craftworks.music.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.launch
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale


var sliderPos = mutableIntStateOf(0)
var repeatSong = mutableStateOf(false)
var shuffleSongs = mutableStateOf(false)

var showNoProviderDialog = mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    lateinit var navController: NavHostController

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        saveManager(this).loadNavidromeProviders()
//        saveManager(this).loadLocalProviders()

        val serviceIntent = Intent(applicationContext, ChoraMediaLibraryService::class.java)
        this@MainActivity.startService(serviceIntent)

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
                val mediaController = rememberManagedMediaController()

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    println("Recomposing EVERYTHING!!!!! VERY BAD")
                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            AnimatedBottomNavBar(navController, scaffoldState)
                        }
                    ) {
                        paddingValues -> SetupNavGraph(navController, paddingValues, mediaController.value)

                        BottomSheetScaffold(
                            modifier = Modifier.fillMaxWidth(),
                            sheetContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                            sheetPeekHeight =
                                if (SongHelper.currentSong.title == "" &&
                                    SongHelper.currentSong.duration == 0 &&
                                    SongHelper.currentSong.imageUrl == "")
                                    0.dp // Hide Mini-player if empty
                                else if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE){
                                    72.dp + 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                }
                                else 72.dp,
                            sheetShadowElevation = 4.dp,
                            sheetShape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
                            sheetDragHandle = { },
                            scaffoldState = scaffoldState,
                            sheetContent = {
                                NowPlayingContent(
                                    context = this@MainActivity,
                                    scaffoldState = scaffoldState,
                                    snackbarHostState = snackbarHostState,
                                    navHostController = navController,
                                    mediaController = mediaController.value
                                )
                            }) {
                        }
                    }
                }

                if(showNoProviderDialog.value)
                    NoMediaProvidersDialog(setShowDialog = { showNoProviderDialog.value = it }, navController)
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
                ChoraMediaLibraryService().onDestroy()
                println("Destroyed, Goodbye :(")
            }
        })

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedBottomNavBar(
    navController: NavHostController,
    scaffoldState : BottomSheetScaffoldState
){
    val backStackEntry = navController.currentBackStackEntryAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedItemIndex by rememberSaveable{ mutableIntStateOf(0) }

    val yTrans by animateIntAsState(
        targetValue =
            if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded)
                dpToPx(-80 - WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt())
            else 0,
        label = "Fullscreen Translation")

    if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE){
        NavigationBar (modifier = Modifier
            .offset { IntOffset(x=0, y= -yTrans.toInt()) }
        ) {
            bottomNavigationItems.forEachIndexed { index, item ->
                if (!item.enabled) return@forEachIndexed
                NavigationBarItem(
                    selected = item.screenRoute == backStackEntry.value?.destination?.route,
                    modifier = Modifier.bounceClick(),
                    onClick = {
                        if (selectedItemIndex == index) return@NavigationBarItem
                        selectedItemIndex = index
                        navController.navigate(item.screenRoute) {
                            // Save state + only add screen once to graph
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        coroutineScope.launch {
                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
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
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }

                            launchSingleTop = true
                            restoreState = true
                        }
                        coroutineScope.launch {
                            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded)
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
