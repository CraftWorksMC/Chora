package com.craftworks.music

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.playlistList
import com.craftworks.music.lyrics.LyricsManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.playing.NowPlayingContent
import com.craftworks.music.ui.screens.AlbumDetails
import com.craftworks.music.ui.screens.AlbumScreen
import com.craftworks.music.ui.screens.ArtistDetails
import com.craftworks.music.ui.screens.ArtistsScreen
import com.craftworks.music.ui.screens.HomeScreen
import com.craftworks.music.ui.screens.PlaylistDetails
import com.craftworks.music.ui.screens.PlaylistScreen
import com.craftworks.music.ui.screens.RadioScreen
import com.craftworks.music.ui.screens.SettingScreen
import com.craftworks.music.ui.screens.SongsScreen
import com.craftworks.music.ui.screens.settings.S_AppearanceScreen
import com.craftworks.music.ui.screens.settings.S_PlaybackScreen
import com.craftworks.music.ui.screens.settings.S_ProviderScreen
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import java.net.URLDecoder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    bottomPadding: Dp,
    mediaController: MediaController?
) {
    val context = LocalContext.current

    playlistList =
        SettingsManager(context).localPlaylists.collectAsStateWithLifecycle(mutableListOf()).value
    LyricsManager.useLrcLib =
        SettingsManager(context).lrcLibLyricsFlow.collectAsStateWithLifecycle(true).value

    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp + WindowInsets.safeDrawing.asPaddingValues().calculateLeftPadding(
        LayoutDirection.Ltr)

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(bottom = bottomPadding, start = leftPadding),
        enterTransition = {
            scaleIn(tween(300), 0.95f) + fadeIn(tween(400))
        },
        exitTransition = {
            fadeOut(tween(300))
        },
        popEnterTransition = {
            scaleIn(tween(300), 1.05f) + fadeIn(tween(400))
        },
        popExitTransition = {
            scaleOut(tween(300), 0.95f) + fadeOut(tween(400))
        },
        route = "main_graph"
    ) {
        println("Recomposing NavHost!")
        composable(route = Screen.Home.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)
            HomeScreen(navController, mediaController, viewModel)
        }
        composable(route = Screen.Song.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: SongsScreenViewModel = hiltViewModel(parentEntry)
            SongsScreen(mediaController, viewModel)
        }
        composable(route = Screen.Radio.route) {
            RadioScreen(mediaController)
        }

        //Albums
        composable(route = Screen.Albums.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("main_graph")
            }
            val viewModel: AlbumScreenViewModel = hiltViewModel(parentEntry)
            AlbumScreen(navController, mediaController, viewModel)
        }
        composable(
            route = Screen.AlbumDetails.route + "/{album}/{image}",
            arguments = listOf(
                navArgument("album") {
                    type = NavType.StringType
                },
                navArgument("image") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("album") ?: ""
            val albumImageUri = URLDecoder.decode(backStackEntry.arguments?.getString("image"), "UTF-8")
            AlbumDetails(
                albumId,
                albumImageUri.toUri(),
                navController,
                mediaController,
            )
        }
        //Artist
        navigation(startDestination = Screen.Artists.route, route = "artists_graph") {
            composable(route = Screen.Artists.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("artists_graph")
                }
                val viewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)
                ArtistsScreen(navController, viewModel)
            }
            composable(route = Screen.ArtistDetails.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("artists_graph")
                }
                val viewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)
                ArtistDetails(navController, mediaController, viewModel)
            }
        }

        //Playlists
        navigation(startDestination = Screen.Playlists.route, route = "playlists_graph") {
            composable(route = Screen.Playlists.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("playlists_graph")
                }
                val viewModel: PlaylistScreenViewModel = hiltViewModel(parentEntry)
                PlaylistScreen(navController, viewModel)
            }
            composable(route = Screen.PlaylistDetails.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("playlists_graph")
                }
                val viewModel: PlaylistScreenViewModel = hiltViewModel(parentEntry)

                PlaylistDetails(navController, mediaController, viewModel)
            }
        }

        //Settings
        navigation(startDestination = Screen.Setting.route, route = "settings_graph") {
            composable(route = Screen.Setting.route) {
                SettingScreen(navController)
            }
            composable(
                route = Screen.S_Appearance.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
                        fullWidth
                    }
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
                        fullWidth
                    }
                }
            ) {
                S_AppearanceScreen(navController)
            }
            composable(
                route = Screen.S_Providers.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
                        fullWidth
                    }
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
                        fullWidth
                    }
                }
            ) {
                S_ProviderScreen(navController)
            }
            composable(
                route = Screen.S_Playback.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
                        fullWidth
                    }
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
                        fullWidth
                    }
                }
            ) {
                S_PlaybackScreen(navController)
            }
        }

        composable(route = Screen.NowPlayingLandscape.route) {
            if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) ||
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            ) {
                var metadata by remember { mutableStateOf<MediaMetadata?>(null) }

                // Update metadata from mediaController.
                LaunchedEffect(mediaController) {
                    if (mediaController?.currentMediaItem != null) {
                        metadata = mediaController.currentMediaItem?.mediaMetadata
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

                NowPlayingContent(
                    mediaController,
                    metadata
                )

                // Keep screen on
                val currentView = LocalView.current
                DisposableEffect(Unit) {
                    currentView.keepScreenOn = true
                    Log.d("NOW-PLAYING", "KeepScreenOn: True")
                    onDispose {
                        currentView.keepScreenOn = false
                        Log.d("NOW-PLAYING", "KeepScreenOn: False")
                    }
                }
            } else {
                navController.navigate(Screen.Home.route) {
                    launchSingleTop = true
                }
            }
        }
    }
}