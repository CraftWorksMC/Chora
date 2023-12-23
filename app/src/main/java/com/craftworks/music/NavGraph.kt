package com.craftworks.music

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
        modifier = Modifier.padding(top = 32.dp, bottom = (72.dp + /* Stupid PaddingValues Error. */ (paddingValues.calculateBottomPadding() * 0)) ),
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    )
    {
        composable(route = Screen.Home.route) {
            HomeScreen()
        }
        composable(route = Screen.Song.route) {
            SongsScreen()
        }
        composable(route = Screen.Radio.route) {
            RadioScreen()
        }
        composable(route = Screen.Playlists.route) {
            PlaylistScreen()
        }
        composable(route = Screen.Setting.route) {
            SettingScreen()
        }
    }
}