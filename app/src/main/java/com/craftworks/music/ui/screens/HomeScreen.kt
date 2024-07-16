package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Screen
import com.craftworks.music.data.albumList
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.navidrome.getNavidromeAlbums
import com.craftworks.music.ui.dpToPx
import com.craftworks.music.ui.elements.AlbumRow
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@Preview(showBackground = true, showSystemUi = true,
    device = "spec:parent=pixel_5"
)
fun HomeScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: HomeScreenViewModel = viewModel()
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    val context = LocalContext.current

    val recentlyPlayedAlbums by viewModel.recentlyPlayedAlbums.collectAsState()
    val recentAlbums by viewModel.recentAlbums.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val shuffledAlbums by viewModel.shuffledAlbums.collectAsState()

    val state = rememberPullToRefreshState()

    if (state.isRefreshing) {
        LaunchedEffect(true) {

            getSongsOnDevice(context)

            viewModel.fetchAlbums()

            state.endRefresh()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(state.nestedScrollConnection)
    ) {
        Column(modifier = Modifier
            .padding(start = leftPadding)
            .fillMaxWidth()
            .wrapContentHeight()
            .verticalScroll(rememberScrollState())) {

            /* GREETING */
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
                    NavidromeLogo()
                    WelcomeText()
                }
                Box(Modifier.padding(end = 12.dp)) {
                    IconButton(onClick = { navHostController.navigate(Screen.Setting.route) {
                        launchSingleTop = true
                    } },
                        modifier = Modifier
                            .size(48.dp)) {
                        Icon(ImageVector.vectorResource(R.drawable.rounded_settings_24),
                            contentDescription = "Settings",
                            modifier = Modifier.size(32.dp))
                    }
                }
            }

            HorizontalLineWithNavidromeCheck()

            Spacer(modifier = Modifier.height(12.dp))


            RecentlyPlayed(mediaController, navHostController, recentlyPlayedAlbums)
            RecentlyAdded(mediaController, navHostController, recentAlbums)
            MostPlayed(mediaController, navHostController, mostPlayedAlbums)
            Shuffled(mediaController, navHostController, shuffledAlbums)

            /* EXPLORE FROM YOUR LIBRARY */


            //BottomSpacer()
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )
    }

}

@Composable fun NavidromeLogo(){
    if (useNavidromeServer.value && showNavidromeLogo.value){
        var rotation by remember { mutableFloatStateOf(-10f) }
        val animatedRotation by animateFloatAsState(
            targetValue = rotation,
            animationSpec = tween(durationMillis = 1000),
            label = "Navidrome Logo Rotate"
        )
        val offsetX = dpToPx(-36)

        val isClickable =
            if (LocalConfiguration.current.uiMode == Configuration.UI_MODE_TYPE_TELEVISION)
                Modifier.clickable { rotation += 360f }
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
}

@Composable fun WelcomeText(){
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

        /*
        if (homeScreenUseAlbums.value){
            /* ALBUMS ROW */
            //val recentlyPlayedAlbums = albumList.sortedByDescending { it.played }.take(20)

            AlbumRow(albums = viewModel.recentlyPlayedAlbums.collectAsState().value, mediaController = mediaController) { album ->
                navHostController.navigate(Screen.AlbumDetails.route) {
                    launchSingleTop = true
                }
                selectedAlbum = album
            }
        }
        else {
            /* SONGS ROW */
            val recentlyPlayedSongsList = songsList.sortedByDescending { song: MediaData.Song -> song.lastPlayed }.take(20)
            SongsRow(songsList = recentlyPlayedSongsList, onSongSelected = { song ->
                //SongHelper.currentSong = song
                SongHelper.currentList = recentlyPlayedSongsList
                song.media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController) }
            })
        }
        */
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


class HomeScreenViewModel : ViewModel() {
    private val _recentlyPlayedAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val recentlyPlayedAlbums: StateFlow<List<MediaData.Album>> = _recentlyPlayedAlbums.asStateFlow()

    private val _recentAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val recentAlbums: StateFlow<List<MediaData.Album>> = _recentAlbums.asStateFlow()

    private val _mostPlayedAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val mostPlayedAlbums: StateFlow<List<MediaData.Album>> = _mostPlayedAlbums.asStateFlow()

    private val _shuffledAlbums = MutableStateFlow<List<MediaData.Album>>(emptyList())
    val shuffledAlbums: StateFlow<List<MediaData.Album>> = _shuffledAlbums.asStateFlow()

    init {
        fetchAlbums()
    }

    fun fetchAlbums() {
        viewModelScope.launch {
            coroutineScope {
                if (useNavidromeServer.value) {
                    val recentlyPlayedDeferred  = async { getNavidromeAlbums("recent", 20) }
                    val recentDeferred          = async { getNavidromeAlbums("newest", 20) }
                    val mostPlayedDeferred      = async { getNavidromeAlbums("frequent", 20) }
                    val shuffledDeferred        = async { getNavidromeAlbums("random", 20) }

                    _recentlyPlayedAlbums.value = recentlyPlayedDeferred.await()
                    _recentAlbums.value = recentDeferred.await()
                    _mostPlayedAlbums.value = mostPlayedDeferred.await()
                    _shuffledAlbums.value = shuffledDeferred.await()
                }
                else {
                    _recentlyPlayedAlbums.value = albumList.sortedBy { it.played }.take(20)
                    _recentAlbums.value = albumList.sortedBy { it.created }.take(20)
                    _mostPlayedAlbums.value = albumList.sortedBy { it.playCount }.take(20)
                    _shuffledAlbums.value = albumList.shuffled().take(20)
                }
            }
        }
    }
}