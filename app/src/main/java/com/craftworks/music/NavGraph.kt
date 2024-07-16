package com.craftworks.music

import android.content.res.Configuration
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.craftworks.music.data.Screen
import com.craftworks.music.ui.elements.bottomSpacerHeightDp
import com.craftworks.music.ui.screens.AlbumDetails
import com.craftworks.music.ui.screens.AlbumScreen
import com.craftworks.music.ui.screens.ArtistDetails
import com.craftworks.music.ui.screens.ArtistsScreen
import com.craftworks.music.ui.screens.ArtistsScreenViewModel
import com.craftworks.music.ui.screens.HomeScreen
import com.craftworks.music.ui.screens.HomeScreenViewModel
import com.craftworks.music.ui.screens.PlaylistDetails
import com.craftworks.music.ui.screens.PlaylistScreen
import com.craftworks.music.ui.screens.PlaylistScreenViewModel
import com.craftworks.music.ui.screens.RadioScreen
import com.craftworks.music.ui.screens.SettingScreen
import com.craftworks.music.ui.screens.SongsScreen
import com.craftworks.music.ui.screens.settings.S_AppearanceScreen
import com.craftworks.music.ui.screens.settings.S_PlaybackScreen
import com.craftworks.music.ui.screens.settings.S_ProviderScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues,
    mediaController: MediaController?
){
    val bottomPadding: Dp =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT)
            paddingValues.calculateBottomPadding() else 0.dp

    val homeViewModel = remember { HomeScreenViewModel() }
    val artistsViewModel = remember { ArtistsScreenViewModel() }
    val playlistViewModel = remember { PlaylistScreenViewModel() }

    NavHost(navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(bottom = bottomPadding + bottomSpacerHeightDp()),
        enterTransition = {
            slideInVertically(animationSpec = tween(durationMillis = 200)) { fullHeight ->
                -fullHeight / 4
            } + fadeIn(animationSpec = tween(durationMillis = 200))
        },
        exitTransition = {
            slideOutVertically(animationSpec = tween(durationMillis = 200)) { fullHeight ->
                -fullHeight / 4
            } + fadeOut(animationSpec = tween(durationMillis = 200))
        }
    )
    {
        println("Recomposing NavHost!")
        composable(route = Screen.Home.route) {
            HomeScreen(navController, mediaController, homeViewModel)
        }
        composable(route = Screen.Song.route) {
            SongsScreen(mediaController)
        }
        composable(route = Screen.Radio.route) {
            RadioScreen(mediaController)
        }

        //Albums
        composable(route = Screen.Albums.route) {
            AlbumScreen(navController, mediaController)
        }
        composable(route = Screen.AlbumDetails.route) {
            AlbumDetails(navController, mediaController)
        }
        //Artist
        composable(route = Screen.Artists.route) {
            ArtistsScreen(navController, artistsViewModel)
        }
        composable(route = Screen.AristDetails.route) {
            ArtistDetails(navController, mediaController, artistsViewModel)
        }

        //Playlists
        composable(route = Screen.Playlists.route) {
            PlaylistScreen(navController, playlistViewModel)
        }
        composable(route = Screen.PlaylistDetails.route) {
            PlaylistDetails(navController, mediaController)
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

        composable(route = Screen.NowPlaying_TV.route){

        }
    }
}