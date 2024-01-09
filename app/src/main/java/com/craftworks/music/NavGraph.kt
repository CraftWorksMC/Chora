package com.craftworks.music

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.craftworks.music.data.Screen
import com.craftworks.music.ui.screens.HomeScreen
import com.craftworks.music.ui.screens.PlaylistDetails
import com.craftworks.music.ui.screens.PlaylistScreen
import com.craftworks.music.ui.screens.RadioScreen
import com.craftworks.music.ui.screens.SettingScreen
import com.craftworks.music.ui.screens.SongsScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
){
    NavHost(navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.padding(top = 32.dp, bottom = (0.dp + /* Stupid PaddingValues Error. */ (paddingValues.calculateBottomPadding() * 0)) ),
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
        composable(route = Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(route = Screen.Song.route) {
            SongsScreen()
        }
        composable(route = Screen.Radio.route) {
            RadioScreen()
        }
        composable(route = Screen.Playlists.route) {
            PlaylistScreen(navController)
        }
        composable(route = Screen.PlaylistDetails.route) {
            PlaylistDetails(navController)
        }
        composable(route = Screen.Setting.route) {
            SettingScreen(navController)
        }
    }
}