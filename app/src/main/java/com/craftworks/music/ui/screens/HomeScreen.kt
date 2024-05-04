package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.data.Screen
import com.craftworks.music.data.Song
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.ui.elements.BottomSpacer
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.elements.SongsRow
import kotlinx.coroutines.delay
import java.net.URL


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
fun HomeScreen(navHostController: NavHostController = rememberNavController()) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    val context = LocalContext.current

    var recentlyPlayedSongsList = songsList.sortedByDescending { song: Song -> song.lastPlayed }.take(20)
    var recentSongsList = songsList.sortedByDescending { song: Song -> song.dateAdded }.take(20)
    var mostPlayedList = songsList.sortedByDescending { song: Song -> song.timesPlayed }.take(20)
    var shuffledSongsList = remember { mutableStateOf(songsList.take(20).shuffled()) }

    val mediaController by rememberManagedMediaController()

    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            songsList.clear()

            getSongsOnDevice(context)

            delay(100) //Avoids Crashes

            if (useNavidromeServer.value)
                getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))

            delay(1500)

            recentlyPlayedSongsList = songsList.sortedByDescending { song: Song -> song.lastPlayed }.take(10)
            recentSongsList = songsList.sortedByDescending { song: Song -> song.dateAdded }
            mostPlayedList = songsList.sortedByDescending { song: Song -> song.timesPlayed }
            shuffledSongsList = mutableStateOf(songsList.take(10).shuffled())

            state.endRefresh()
        }
    }


    Box(Modifier.nestedScroll(state.nestedScrollConnection)) {
        Column(modifier = Modifier
            .padding(start = leftPadding)
            .fillMaxWidth()
            .wrapContentHeight()
            .verticalScroll(rememberScrollState())) {

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(
                        top = WindowInsets.statusBars
                            .asPaddingValues()
                            .calculateTopPadding()
                    )
                //.height(72.dp)
            ) {
                /* GREETING */
                Box(Modifier.weight(1f)) {
                    if (useNavidromeServer.value && showNavidromeLogo.value){
                        var rotation by remember { mutableFloatStateOf(-10f) }
                        val animatedRotation by animateFloatAsState(
                            targetValue = rotation,
                            animationSpec = tween(durationMillis = 1000),
                            label = "Navidrome Logo Rotate"
                        )

                        Image(
                            painter = painterResource(R.drawable.s_m_navidrome),
                            contentDescription = "Navidrome Icon",
                            modifier = Modifier
                                .size(72.dp)
                                .offset(x = (-36).dp, y = 0.dp)
                                .shadow(24.dp, CircleShape)
                                .graphicsLayer(
                                    rotationZ = animatedRotation
                                )
                                .clickable {
                                    rotation += 360f
                                }
                        )
                    }
                    Text(
                        text = "${stringResource(R.string.welcome_text)},\n${username.value}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        modifier = Modifier
                            .padding(start =
                            if (useNavidromeServer.value && showNavidromeLogo.value)
                                42.dp
                            else
                                12.dp),
                        lineHeight = 32.sp
                    )
                }
                Box(Modifier.padding(end = 12.dp)) {
                    Button(
                        onClick = { navHostController.navigate(Screen.Setting.route) {
                            launchSingleTop = true
                        } },
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_settings_24),
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Settings",
                            modifier = Modifier
                                .height(32.dp)
                                .size(32.dp)
                        )
                    }
                }
            }

            HorizontalLineWithNavidromeCheck()

            /* RECENTLY PLAYED */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            ) {
                Text(
                    text = stringResource(R.string.recently_played) + ":",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    modifier = Modifier.padding(start = 12.dp)
                )

                /* SONGS ROW */
                SongsRow(songsList = recentlyPlayedSongsList, onSongSelected = { song ->
                    //SongHelper.currentSong = song
                    SongHelper.currentList = recentlyPlayedSongsList
                    song.media?.let { SongHelper.playStream(context = context, url = it, false, mediaController) }
                })
            }


            /* RECENTLY ADDED SONGS */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            ) {
                Text(
                    text = stringResource(R.string.recently_added) + ":",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    modifier = Modifier.padding(start = 12.dp)
                )

                /* SONGS ROW */
                SongsRow(songsList = recentSongsList, onSongSelected = { song ->
                    //SongHelper.currentSong = song
                    SongHelper.currentList = recentSongsList
                    //songState = true
                    song.media?.let { SongHelper.playStream(context = context, url = it, false, mediaController) }
                })
            }

            /* MOST PLAYED SONGS */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            ) {
                Text(
                    text = stringResource(R.string.most_played) + ":",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    modifier = Modifier.padding(start = 12.dp)
                )
                /* SONGS ROW */
                SongsRow(songsList = mostPlayedList, onSongSelected = { song ->
                    //SongHelper.currentSong = song
                    SongHelper.currentList = mostPlayedList
                    //songState = true
                    song.media?.let { SongHelper.playStream(context = context, url = it, false, mediaController) }
                })
            }

            /* EXPLORE FROM YOUR LIBRARY */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            ) {
                Text(
                    text = stringResource(R.string.random_songs) + ":",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    modifier = Modifier.padding(start = 12.dp)
                )

                /* SONGS ROW */
                SongsRow(songsList = shuffledSongsList.value, onSongSelected = { song ->
                    //SongHelper.currentSong = song
                    SongHelper.currentList = shuffledSongsList.value
                    //songState = true
                    song.media?.let { SongHelper.playStream(context = context, url = it, false, mediaController) }
                })
            }

            BottomSpacer()
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )
    }

}


