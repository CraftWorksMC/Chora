package com.craftworks.music

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import com.craftworks.music.ui.playing.NowPlayingContent
import com.craftworks.music.ui.screens.AlbumDetails
import com.craftworks.music.ui.screens.AlbumScreen
import com.craftworks.music.ui.screens.ArtistDetails
import com.craftworks.music.ui.screens.ArtistsScreen
import com.craftworks.music.ui.screens.HomeListsScreen
import com.craftworks.music.ui.screens.HomeScreen
import com.craftworks.music.ui.screens.PlaylistDetails
import com.craftworks.music.ui.screens.PlaylistScreen
import com.craftworks.music.ui.screens.RadioScreen
import com.craftworks.music.ui.screens.SettingScreen
import com.craftworks.music.ui.screens.SongsScreen
import com.craftworks.music.ui.screens.QueueScreen
import com.craftworks.music.ui.screens.onboarding.OnboardingWizard
import com.craftworks.music.ui.screens.settings.S_AppearanceScreen
import com.craftworks.music.ui.screens.settings.S_ArtworkScreen
import com.craftworks.music.ui.screens.settings.S_DataScreen
import com.craftworks.music.ui.screens.settings.S_PlaybackScreen
import com.craftworks.music.ui.screens.settings.S_ProviderScreen
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import com.craftworks.music.ui.viewmodels.RadioScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import java.net.URLDecoder

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    bottomPadding: Dp,
    mediaController: MediaController?,
    showOnboarding: Boolean = false
) {
    val context = LocalContext.current

    // Update playlist list from local settings (using synchronized list)
    // FRAGILE PATTERN: Global mutable state is used here (playlistList).
    // This is fragile and not recommended. A better approach is to use a
    // single source of truth from a repository providing a StateFlow.
    @Suppress("DEPRECATION")
    val localPlaylists by LocalDataSettingsManager(context).localPlaylists.collectAsStateWithLifecycle(mutableListOf())

    LaunchedEffect(localPlaylists) {
        @Suppress("DEPRECATION")
        synchronized(playlistList) {
            playlistList.clear()
            playlistList.addAll(localPlaylists)
        }
    }

    val useLrcLib by MediaProviderSettingsManager(context).lrcLibLyricsFlow.collectAsStateWithLifecycle(true)
    LaunchedEffect(useLrcLib) {
        LyricsState.setUseLrcLib(useLrcLib)
    }

    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp + WindowInsets.safeDrawing.asPaddingValues().calculateLeftPadding(
        LayoutDirection.Ltr)

    val animationSpec = MaterialTheme.LocalMotionScheme.current.slowSpatialSpec<Float>()

    // Determine start destination based on onboarding state
    val startDestination = if (showOnboarding) Screen.Onboarding.route else Screen.Home.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(bottom = bottomPadding, start = leftPadding),
        enterTransition = {
            fadeIn(animationSpec)
            //scaleIn(tween(300), 0.95f) + fadeIn(tween(400))
        },
        exitTransition = {
            fadeOut(animationSpec)
            //fadeOut(tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec)
            //scaleIn(tween(300), 1.05f) + fadeIn(tween(400))
        },
        popExitTransition = {
            fadeOut(animationSpec)
            //scaleOut(tween(300), 0.95f) + fadeOut(tween(400))
        },
        route = "main_graph" // This route is for scoping, but the implementation was fragile.
    ) {
        // Onboarding wizard - shown on first launch
        composable(route = Screen.Onboarding.route) {
            OnboardingWizard(
                onComplete = {
                    // Navigate to home and clear the back stack
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            val viewModel: HomeScreenViewModel = hiltViewModel()
            HomeScreen(navController, mediaController, viewModel)
        }
        composable(
            route = Screen.HomeLists.route + "/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val viewModel: HomeScreenViewModel = hiltViewModel()

            val category = backStackEntry.arguments?.getString("category") ?: "recently_played"

            val albums = when (category) {
                "recently_played" -> viewModel.recentlyPlayedAlbums.collectAsStateWithLifecycle().value
                "recently_added" -> viewModel.recentAlbums.collectAsStateWithLifecycle().value
                "most_played" -> viewModel.mostPlayedAlbums.collectAsStateWithLifecycle().value
                "random_songs" -> viewModel.shuffledAlbums.collectAsStateWithLifecycle().value
                else -> emptyList()
            }

            HomeListsScreen(
                albums = albums,
                viewModel = viewModel,
                categoryKey = category,
                navHostController = navController,
            )
        }

        composable(route = Screen.Song.route) {
            val viewModel: SongsScreenViewModel = hiltViewModel()
            SongsScreen(mediaController, viewModel)
        }
        composable(route = Screen.Radio.route) {
            val viewModel: RadioScreenViewModel = hiltViewModel()
            RadioScreen(mediaController, viewModel)
        }

        //Albums
        composable(route = Screen.Albums.route) {
            val viewModel: AlbumScreenViewModel = hiltViewModel()
            AlbumScreen(navController, mediaController, viewModel)
        }
        composable(
            route = Screen.AlbumDetails.route + "/{album}?image={image}",
            arguments = listOf(
                navArgument("album") {
                    type = NavType.StringType
                },
                navArgument("image") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("album") ?: ""
            val imageArg = backStackEntry.arguments?.getString("image")
            val albumImageUri = if (!imageArg.isNullOrEmpty()) {
                try {
                    URLDecoder.decode(imageArg, "UTF-8")
                } catch (e: Exception) {
                    Log.e("NavGraph", "Failed to decode image URL: $imageArg", e)
                    ""
                }
            } else {
                ""
            }

            AlbumDetails(
                albumId,
                albumImageUri.toUri(),
                navController,
                mediaController,
            )
        }
        //Artist
        navigation(startDestination = Screen.Artists.route, route = "artists_graph") {
            composable(route = Screen.Artists.route) {
                val viewModel: ArtistsScreenViewModel = hiltViewModel()
                ArtistsScreen(navController, mediaController, viewModel)
            }
            composable(route = Screen.ArtistDetails.route) {
                val viewModel: ArtistsScreenViewModel = hiltViewModel()
                ArtistDetails(navController, mediaController, viewModel)
            }
        }

        //Playlists
        navigation(startDestination = Screen.Playlists.route, route = "playlists_graph") {
            composable(route = Screen.Playlists.route) {
                val viewModel: PlaylistScreenViewModel = hiltViewModel()
                PlaylistScreen(navController, viewModel)
            }
            composable(route = Screen.PlaylistDetails.route) {
                val viewModel: PlaylistScreenViewModel = hiltViewModel()
                PlaylistDetails(navController, mediaController, viewModel)
            }
        }

        //Settings
        navigation(startDestination = Screen.Setting.route, route = "settings_graph") {
            composable(
                route = Screen.Setting.route,) {
                SettingScreen(navController)
            }
            composable(
                route = Screen.S_Appearance.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeIn(animationSpec)
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeOut(animationSpec)
                }
            ) {
                S_AppearanceScreen(navController)
            }
            composable(
                route = Screen.S_Artwork.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeIn(animationSpec)
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeOut(animationSpec)
                }
            ) {
                S_ArtworkScreen(navController)
            }
            composable(
                route = Screen.S_Providers.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeIn(animationSpec)
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeOut(animationSpec)
                }
            ) {
                S_ProviderScreen(navController)
            }
            composable(
                route = Screen.S_Playback.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeIn(tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeOut(tween(300))
                }
            ) {
                S_PlaybackScreen(navController)
            }
            composable(
                route = Screen.S_Data.route,
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeIn(tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                        fullWidth / 4
                    } + fadeOut(tween(300))
                }
            ) {
                S_DataScreen(navController)
            }
        }

        composable(route = Screen.NowPlayingLandscape.route) {
            val isValidOrientation = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) ||
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

            // Handle navigation in a side effect to avoid navigation during composition
            LaunchedEffect(isValidOrientation) {
                if (!isValidOrientation) {
                    navController.navigate(Screen.Home.route) {
                        launchSingleTop = true
                    }
                }
            }

            if (isValidOrientation) {
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
            }
        }

        // Queue screen
        composable(
            route = Screen.Queue.route,
            enterTransition = {
                slideInHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                    fullWidth / 4
                } + fadeIn(animationSpec)
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(durationMillis = 300)) { fullWidth ->
                    fullWidth / 4
                } + fadeOut(animationSpec)
            }
        ) {
            QueueScreen(navController)
        }
    }
}
