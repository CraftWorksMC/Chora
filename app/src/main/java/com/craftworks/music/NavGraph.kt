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
import androidx.compose.foundation.layout.padding
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.craftworks.music.data.Screen
import com.craftworks.music.data.playlistList
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
import com.craftworks.music.ui.viewmodels.GlobalViewModels
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import java.net.URLDecoder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    bottomPadding: Dp,
    mediaController: MediaController?,
    homeViewModel: HomeScreenViewModel,
    albumViewModel: AlbumScreenViewModel,
    songsViewModel: SongsScreenViewModel,
    artistsViewModel: ArtistsScreenViewModel,
    playlistViewModel: PlaylistScreenViewModel
) {
    val context = LocalContext.current

    playlistList = SettingsManager(context).localPlaylists.collectAsStateWithLifecycle(mutableListOf()).value
    LyricsManager.useLrcLib = SettingsManager(context).lrcLibLyricsFlow.collectAsStateWithLifecycle(true).value

    LaunchedEffect(Unit) {
        GlobalViewModels.registerViewModel(homeViewModel)
        GlobalViewModels.registerViewModel(albumViewModel)
        GlobalViewModels.registerViewModel(songsViewModel)
        GlobalViewModels.registerViewModel(artistsViewModel)
    }

    NavHost(navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(bottom = bottomPadding),
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
        }
    ) {
        println("Recomposing NavHost!")
        composable(route = Screen.Home.route) {
            HomeScreen(navController, mediaController, homeViewModel)
        }
        composable(route = Screen.Song.route) {
            SongsScreen(mediaController, songsViewModel)
        }
        composable(route = Screen.Radio.route) {
            RadioScreen(mediaController)
        }

        //Albums
        composable(route = Screen.Albums.route) {
            AlbumScreen(navController, mediaController, albumViewModel)
        }
        composable(route = Screen.AlbumDetails.route + "/{album}/{image}",
            arguments = listOf(
                navArgument("album") {
                    type = NavType.StringType
                },
                navArgument("image") {
                    type = NavType.StringType
                }
            )
        ) {
            val albumId = it.arguments?.getString("album") ?: ""
            val albumImageUri = URLDecoder.decode(it.arguments?.getString("image"), "UTF-8")
            AlbumDetails(
                albumId,
                albumImageUri.toUri(),
                navController,
                mediaController
            )
        }
        //Artist
        composable(route = Screen.Artists.route) {
            ArtistsScreen(navController, artistsViewModel)
        }
        composable(route = Screen.ArtistDetails.route + "/{artist}",
            arguments = listOf(
                navArgument("artist") {
                    type = NavType.StringType
                }
            )
        ) {
            val artistId = it.arguments?.getString("artist") ?: ""
            ArtistDetails(navController, mediaController, artistId)
        }

        //Playlists
        composable(route = Screen.Playlists.route) {
            PlaylistScreen(navController, playlistViewModel)
        }
        composable(route = Screen.PlaylistDetails.route) {
            PlaylistDetails(navController, mediaController, playlistViewModel)
        }

        //Settings
        composable(route = Screen.Setting.route) {
            SettingScreen(navController)
        }
        composable(route = Screen.S_Appearance.route,
            enterTransition = { slideInHorizontally(animationSpec = tween(durationMillis = 200)){ fullWidth ->
                fullWidth }},
            exitTransition = {slideOutHorizontally(animationSpec = tween(durationMillis = 200)){ fullWidth ->
                fullWidth }}
        ) {
            S_AppearanceScreen(navController)
        }
        composable(route = Screen.S_Providers.route,
            enterTransition = { slideInHorizontally(animationSpec = tween(durationMillis = 200)){ fullWidth ->
                fullWidth }},
            exitTransition = {slideOutHorizontally(animationSpec = tween(durationMillis = 200)){ fullWidth ->
                fullWidth }}
        ) {
            S_ProviderScreen(navController)
        }
        composable(route = Screen.S_Playback.route,
            enterTransition = { slideInHorizontally(animationSpec = tween(durationMillis = 200)){ fullWidth ->
                fullWidth }},
            exitTransition = {slideOutHorizontally(animationSpec = tween(durationMillis = 200)){ fullWidth ->
                fullWidth }}
        ) {
            S_PlaybackScreen(navController)
        }

        composable(route = Screen.NowPlayingLandscape.route){
            if ((LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) ||
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
                    LocalContext.current,
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
            }
            else{
                navController.navigate(Screen.Home.route) {
                    launchSingleTop = true
                }
            }
        }
    }
}