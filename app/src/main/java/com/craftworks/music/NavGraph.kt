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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalWindowInfo
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.playing.NowPlayingContent
import com.craftworks.music.ui.playing.NowPlayingViewModel
import com.craftworks.music.ui.playing.dpToPx
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
import com.craftworks.music.ui.screens.settings.S_AppearanceScreen
import com.craftworks.music.ui.screens.settings.S_LyricsProviderScreen
import com.craftworks.music.ui.screens.settings.S_MiscScreen
import com.craftworks.music.ui.screens.settings.S_PlaybackScreen
import com.craftworks.music.ui.screens.settings.S_ProviderScreen
import com.craftworks.music.ui.screens.tv.TvAlbumDetails
import com.craftworks.music.ui.screens.tv.TvAlbumScreen
import com.craftworks.music.ui.screens.tv.TvArtistDetailsScreen
import com.craftworks.music.ui.screens.tv.TvArtistScreen
import com.craftworks.music.ui.screens.tv.TvHomeScreen
import com.craftworks.music.ui.screens.tv.TvPlaylistDetails
import com.craftworks.music.ui.screens.tv.TvPlaylistScreen
import com.craftworks.music.ui.screens.tv.TvRadioScreen
import com.craftworks.music.ui.screens.tv.TvSearchScreen
import com.craftworks.music.ui.screens.tv.TvSettingScreen
import com.craftworks.music.ui.screens.tv.TvSongsScreen
import com.craftworks.music.ui.screens.tv.settings.TvS_AppearanceScreen
import com.craftworks.music.ui.screens.tv.settings.TvS_PlaybackScreen
import com.craftworks.music.ui.screens.tv.settings.TvS_ProviderScreen
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import com.craftworks.music.ui.viewmodels.RadioScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    bottomPadding: Dp,
    mediaController: MediaController?
) {
    val isTv = LocalConfiguration.current.uiMode and
            Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

    val orientation = LocalConfiguration.current.orientation

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val leftPadding = remember(LocalConfiguration.current.orientation, isTv) {
        if (orientation != Configuration.ORIENTATION_LANDSCAPE || isTv) {
            0.dp
        } else {
            80.dp + insets.calculateLeftPadding(LayoutDirection.Ltr)
        }
    }

    val animationSpec = MaterialTheme.LocalMaterialTheme.current.motionScheme.slowSpatialSpec<Float>()

    NavHost(
        navController = navController,
        startDestination = Screen.MainGraph,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding, start = leftPadding),
        enterTransition = {
            fadeIn(animationSpec)
        },
        exitTransition = {
            fadeOut(animationSpec)
        },
        popEnterTransition = {
            fadeIn(animationSpec)
        },
        popExitTransition = {
            fadeOut(animationSpec)
        }
    ) {
        println("Recomposing NavHost!")
        navigation<Screen.MainGraph>(startDestination = Screen.Home) {
            composable<Screen.Home> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.MainGraph>()
                }
                val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)
                if (isTv)
                    TvSideNavigation(navController, mediaController) {
                        TvHomeScreen(navController, mediaController, viewModel)
                    }
                else
                    HomeScreen(navController, mediaController, viewModel)
            }
            composable<Screen.HomeLists> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.MainGraph>()
                }
                val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)

                val category = backStackEntry.toRoute<Screen.HomeLists>().category

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

            composable<Screen.Songs> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.MainGraph>()
                }
                val viewModel: SongsScreenViewModel = hiltViewModel(parentEntry)
                if (isTv)
                    TvSideNavigation(navController, mediaController) {
                        TvSongsScreen(mediaController, navController, viewModel)
                    }
                else
                    SongsScreen(mediaController, viewModel)
            }
            composable<Screen.Radios> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.MainGraph>()
                }
                val viewModel: RadioScreenViewModel = hiltViewModel(parentEntry)
                if (isTv)
                    TvSideNavigation(navController, mediaController) {
                        TvRadioScreen(mediaController, navController, viewModel)
                    }
                else
                    RadioScreen(mediaController, viewModel)
            }

            //Albums
            navigation<Screen.Albums>(startDestination = Screen.AlbumList) {
                composable<Screen.AlbumList> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<Screen.MainGraph>()
                    }
                    val viewModel: AlbumScreenViewModel = hiltViewModel(parentEntry)
                    if (isTv)
                        TvSideNavigation(navController, mediaController) {
                            TvAlbumScreen(navController, viewModel)
                        }
                    else
                        AlbumScreen(navController, mediaController, viewModel)
                }
                composable<Screen.AlbumDetails> { backStackEntry ->
                    val albumId = backStackEntry.toRoute<Screen.AlbumDetails>().albumId
                    val imageUri = backStackEntry.toRoute<Screen.AlbumDetails>().imageUri
                    if (isTv)
                        TvAlbumDetails(
                            albumId,
                            imageUri.toUri(),
                            mediaController,
                            navController
                        )
                    else
                        AlbumDetails(
                            albumId,
                            imageUri.toUri(),
                            navController,
                            mediaController,
                        )
                }
            }
            //Artist
            navigation<Screen.Artists>(startDestination = Screen.ArtistsList) {
                composable<Screen.ArtistsList> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<Screen.MainGraph>()
                    }
                    val viewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)

                    if (isTv)
                        TvSideNavigation(navController, mediaController) {
                            TvArtistScreen(navController, viewModel)
                        }
                    else
                        ArtistsScreen(navController, viewModel)
                }
                composable<Screen.ArtistDetails> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<Screen.MainGraph>()
                    }

                    val artistId = backStackEntry.toRoute<Screen.ArtistDetails>().artistId
                    val imageUri = backStackEntry.toRoute<Screen.ArtistDetails>().imageUri

                    val viewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)

                    if (isTv)
                        TvArtistDetailsScreen(artistId, imageUri, navController, mediaController, viewModel)
                    else
                        ArtistDetails(artistId, imageUri, navController, mediaController, viewModel)
                }
            }

            //Playlists
            navigation<Screen.Playlists>(startDestination = Screen.PlaylistList) {
                composable<Screen.PlaylistList> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<Screen.MainGraph>()
                    }
                    val viewModel: PlaylistScreenViewModel = hiltViewModel(parentEntry)

                    if (isTv)
                        TvSideNavigation(navController, mediaController) {
                            TvPlaylistScreen(navController, viewModel)
                        }
                    else
                        PlaylistScreen(navController, viewModel)
                }
                composable<Screen.PlaylistDetails> { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<Screen.MainGraph>()
                    }

                    val playlistId = backStackEntry.toRoute<Screen.PlaylistDetails>().playlistId
                    val imageUri = backStackEntry.toRoute<Screen.PlaylistDetails>().imageUri

                    val viewModel: PlaylistScreenViewModel = hiltViewModel(parentEntry)

                    if (isTv)
                        TvPlaylistDetails(playlistId, imageUri, navController, mediaController, viewModel)
                    else
                        PlaylistDetails(playlistId, imageUri, navController, mediaController, viewModel)
                }
            }

            //Settings
            navigation<Screen.Settings>(startDestination = Screen.SettingsList) {
                composable<Screen.SettingsList> {
                    if (isTv)
                        TvSideNavigation(navController, mediaController) {
                            TvSettingScreen(navController)
                        }
                    else
                        SettingScreen(navController)
                }
                composable<Screen.S_Appearance>(
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
                    if (isTv)
                        TvS_AppearanceScreen()
                    else
                        S_AppearanceScreen(navController)
                }
                composable<Screen.S_Media_Providers>(
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
                    if (isTv)
                        TvS_ProviderScreen()
                    else
                        S_ProviderScreen(navController)
                }
                composable<Screen.S_Lyrics_Providers>(
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
                    if (isTv)
                        TvS_ProviderScreen()
                    else
                        S_LyricsProviderScreen(navController)
                }
                composable<Screen.S_Playback>(
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
                    if (isTv)
                        TvS_PlaybackScreen()
                    else
                        S_PlaybackScreen(navController)
                }
                composable<Screen.S_Misc>(
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
                    S_MiscScreen(navController)
                }
            }

            composable<Screen.NowPlayingLandscape> { backStackEntry ->
                if (LocalWindowInfo.current.containerSize.width < dpToPx(640)) {
                    navController.popBackStack()
                    navController.navigate(Screen.Home) {
                        launchSingleTop = true
                    }
                }

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.MainGraph>()
                }

                val viewModel: NowPlayingViewModel = hiltViewModel(parentEntry)

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
                    metadata,
                    navController,
                    viewModel
                )

                // Keep screen on
                val currentView = LocalView.current
                val disableScreenStandy by AppearanceSettingsManager(LocalContext.current).disableScreenStandby.collectAsStateWithLifecycle(true)
                DisposableEffect(Unit) {
                    if (disableScreenStandy) {
                        currentView.keepScreenOn = true
                        Log.d("NOW-PLAYING", "KeepScreenOn: True")
                    }

                    onDispose {
                        currentView.keepScreenOn = false
                        Log.d("NOW-PLAYING", "KeepScreenOn: False")
                    }
                }
            }

            composable<Screen.Search> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.MainGraph>()
                }

                val albumViewModel: AlbumScreenViewModel = hiltViewModel(parentEntry)
                val songViewModel: SongsScreenViewModel = hiltViewModel(parentEntry)
                val artistViewModel: ArtistsScreenViewModel = hiltViewModel(parentEntry)

                TvSideNavigation(navController, mediaController) {
                    TvSearchScreen(
                        navController,
                        mediaController,
                        albumViewModel,
                        songViewModel,
                        artistViewModel
                    )
                }
            }
        }
    }
}