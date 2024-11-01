package com.craftworks.music.ui.screens

import android.content.res.Configuration
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Screen
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.ui.elements.AlbumRow
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(
    showBackground = true, showSystemUi = true, device = "spec:parent=pixel_5"
)
fun HomeScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel = viewModel()
) {
    val leftPadding =
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    val context = LocalContext.current

    val recentlyPlayedAlbums by viewModel.recentlyPlayedAlbums.collectAsState()
    val recentAlbums by viewModel.recentAlbums.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val shuffledAlbums by viewModel.shuffledAlbums.collectAsState()

    val state = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        getSongsOnDevice(context)

        viewModel.reloadData()
        isRefreshing = false
    }

    PullToRefreshBox(
        modifier = Modifier,
            //.fillMaxSize()
            //.background(MaterialTheme.colorScheme.background),
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Column(
            modifier = Modifier
                .padding(start = leftPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
            ) {
                Box(Modifier.weight(1f)) {
                    val username = SettingsManager(context).usernameFlow.collectAsState("Username")
                    val showNavidromeLogo =
                        SettingsManager(context).showNavidromeLogoFlow.collectAsState(true).value && NavidromeManager.checkActiveServers()

                    if (showNavidromeLogo) NavidromeLogo()

                    Text(
                        text = "${stringResource(R.string.welcome_text)},\n${username.value}!",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        modifier = Modifier.padding(
                            start = if (showNavidromeLogo) 42.dp else 12.dp
                        ),
                        lineHeight = 32.sp
                    )
                }
                IconButton(
                    onClick = {
                        navHostController.navigate(Screen.Setting.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, modifier = Modifier
                        .padding(end = 12.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.rounded_settings_24),
                        contentDescription = "Settings",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            HorizontalLineWithNavidromeCheck()

            Spacer(modifier = Modifier.height(12.dp))


            RecentlyPlayed(mediaController, navHostController, recentlyPlayedAlbums)
            RecentlyAdded(mediaController, navHostController, recentAlbums)
            MostPlayed(mediaController, navHostController, mostPlayedAlbums)
            Shuffled(mediaController, navHostController, shuffledAlbums)
        }
    }
}

@Composable fun NavidromeLogo(){
    var rotation by remember { mutableFloatStateOf(-10f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(durationMillis = 1200, 0 , easing = { OvershootInterpolator().getInterpolation(it) }),
        label = "Navidrome Logo Rotate"
    )
    val offsetX = dpToPx(-36)
    val clickAction = rememberUpdatedState {
        rotation += 360f
    }

    val isClickable =
        if (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION)
            Modifier.clickable { clickAction.value.invoke() }
        else
            Modifier

    Image(
        painter = painterResource(R.drawable.s_m_navidrome),
        contentDescription = "Navidrome Icon",
        modifier = Modifier
            .size(72.dp)
            .offset { IntOffset(offsetX, 0) }
            .shadow(24.dp, CircleShape)
            .graphicsLayer {
                rotationZ = animatedRotation
            }
            .then(isClickable)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun RecentlyPlayed(
    mediaController: MediaController?,
    navHostController: NavHostController,
    albums: List<MediaData.Album>
) {
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

        AlbumRow(albums = albums, mediaController = mediaController) { album ->
            navHostController.navigate(Screen.AlbumDetails.route) {
                launchSingleTop = true
            }
            selectedAlbum = album
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun RecentlyAdded(
    mediaController: MediaController?,
    navHostController: NavHostController,
    albums: List<MediaData.Album>
) {
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

        AlbumRow(albums = albums, mediaController = mediaController) { album ->
            navHostController.navigate(Screen.AlbumDetails.route) {
                launchSingleTop = true
            }
            selectedAlbum = album
        }

        /*
        if (homeScreenUseAlbums.value){
            /* ALBUMS ROW */
            //val recentlyAddedAlbums = albumList.sortedByDescending { it.created }.take(20)
            AlbumRow(albums = viewModel.recentAlbums.collectAsState().value, mediaController = mediaController) { album ->
                navHostController.navigate(Screen.AlbumDetails.route) {
                    launchSingleTop = true
                }
                selectedAlbum = album
            }
        }
        else {
            /* SONGS ROW */
            val recentSongsList = songsList.sortedByDescending { song: MediaData.Song -> song.dateAdded }.take(20)
            SongsRow(songsList = recentSongsList, onSongSelected = { song ->
                //SongHelper.currentSong = song
                SongHelper.currentList = recentSongsList
                //songState = true
                song.media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController) }
            })
        }
        */
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun MostPlayed(
    mediaController: MediaController?,
    navHostController: NavHostController,
    albums: List<MediaData.Album>
) {
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

        AlbumRow(albums = albums, mediaController = mediaController) { album ->
            navHostController.navigate(Screen.AlbumDetails.route) {
                launchSingleTop = true
            }
            selectedAlbum = album
        }

        /*
        if (homeScreenUseAlbums.value){
            /* ALBUMS ROW */
            //val mostPlayedAlbums = albumList.sortedByDescending { it.playCount }.take(20)
            AlbumRow(albums = viewModel.mostPlayedAlbums.collectAsState().value, mediaController = mediaController) { album ->
                navHostController.navigate(Screen.AlbumDetails.route) {
                    launchSingleTop = true
                }
                selectedAlbum = album
            }
        }
        else{
            /* SONGS ROW */
            val mostPlayedList = songsList.sortedByDescending { song: MediaData.Song -> song.timesPlayed }.take(20)
            SongsRow(songsList = mostPlayedList, onSongSelected = { song ->
                //SongHelper.currentSong = song
                SongHelper.currentList = mostPlayedList
                song.media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController) }
            })
        }
        */
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun Shuffled(
    mediaController: MediaController?,
    navHostController: NavHostController,
    albums: List<MediaData.Album>
) {
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

        AlbumRow(albums = albums, mediaController = mediaController) { album ->
            navHostController.navigate(Screen.AlbumDetails.route) {
                launchSingleTop = true
            }
            selectedAlbum = album
        }

        /*
        if (homeScreenUseAlbums.value){
            /* ALBUMS ROW */
//                    val shuffledAlbumsList = remember {
//                        derivedStateOf { albumList.shuffled().take(20) }
//                    }
            AlbumRow(albums = viewModel.shuffledAlbums.collectAsState().value, mediaController = mediaController) { album ->
                navHostController.navigate(Screen.AlbumDetails.route) {
                    launchSingleTop = true
                }
                selectedAlbum = album
            }
        }
        else{
            /* SONGS ROW */
            val shuffledSongsList = remember {
                derivedStateOf { songsList.shuffled().take(20) }
            }
            SongsRow(songsList = shuffledSongsList.value, onSongSelected = { song ->
                //SongHelper.currentSong = song
                SongHelper.currentList = shuffledSongsList.value
                //songState = true
                song.media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController) }
            })
        }
        */
    }
}