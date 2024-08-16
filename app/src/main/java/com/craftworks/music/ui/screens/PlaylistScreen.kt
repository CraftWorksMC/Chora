package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Screen
import com.craftworks.music.data.playlistList
import com.craftworks.music.providers.local.localPlaylistImageGenerator
import com.craftworks.music.providers.navidrome.NavidromeManager
import com.craftworks.music.providers.navidrome.getNavidromePlaylistDetails
import com.craftworks.music.providers.navidrome.getNavidromePlaylists
import com.craftworks.music.saveManager
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.elements.PlaylistGrid
import com.craftworks.music.ui.elements.dialogs.DeletePlaylist
import com.craftworks.music.ui.elements.dialogs.showDeletePlaylistDialog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

var selectedPlaylist by mutableStateOf(
    MediaData.Playlist(
        navidromeID = "Local",
        name = "Awesome Playlist",
        coverArt = "",
        created = "", changed = "",
        duration = 42, songCount = 5
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun PlaylistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: PlaylistScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    val state = rememberPullToRefreshState()
    val context = LocalContext.current

    val playlists by viewModel.allPlaylists.collectAsState()

    if (state.isRefreshing) {
        LaunchedEffect(true) {
            //saveManager(context).saveSettings()
            //delay(500)
            playlistList.clear()

            saveManager(context).loadPlaylists()

            viewModel.reloadData()

            delay(500)
            for (playlist in playlistList){
                if (playlist.navidromeID == "Local"){
                    val playlistImage = playlist.songs?.let { localPlaylistImageGenerator(it, context) } ?: ""
                    playlist.coverArt = playlistImage.toString()
                }
            }
            state.endRefresh()
        }
    }


    /* RADIO ICON + TEXT */
    Box(modifier = Modifier
        .nestedScroll(state.nestedScrollConnection)
        .fillMaxWidth()
        .fillMaxHeight()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(
                start = leftPadding,
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )) {
            /* HEADER */
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.placeholder),
                    contentDescription = "Songs Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(R.string.playlists),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize
                )
            }

            HorizontalLineWithNavidromeCheck()

            PlaylistGrid(playlists, onPlaylistSelected = { playlist ->
                navHostController.navigate(Screen.PlaylistDetails.route) {
                    launchSingleTop = true
                }
                selectedPlaylist = playlist
            })

            if(showDeletePlaylistDialog.value)
                DeletePlaylist(setShowDialog =  { showDeletePlaylistDialog.value = it } )
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )
    }
}

class PlaylistScreenViewModel : ViewModel(), ReloadableViewModel {
    private val _allPlaylists = MutableStateFlow<List<MediaData.Playlist>>(emptyList())
    val allPlaylists: StateFlow<List<MediaData.Playlist>> = _allPlaylists.asStateFlow()

    private var _selectedPlaylist = MutableStateFlow<MediaData.Playlist?>(null)
    var selectedPlaylist: StateFlow<MediaData.Playlist?> = _selectedPlaylist

    fun setCurrentPlaylist(playlist: MediaData.Playlist){
        _selectedPlaylist.value = playlist
    }

    override fun reloadData() {
        viewModelScope.launch {
            coroutineScope {
                if (NavidromeManager.checkActiveServers()){
                    val allPlaylistsDeferred  = async { getNavidromePlaylists() }

                    _allPlaylists.value = allPlaylistsDeferred.await()

                    _allPlaylists.value += playlistList
                }
                else {
                    _allPlaylists.value = playlistList
                }
            }
        }
    }

    fun fetchPlaylistDetails(playlistId: String) {
        if (!NavidromeManager.checkActiveServers()) return

        viewModelScope.launch {
            coroutineScope {
                val selectedPlaylistDeferred  = async { getNavidromePlaylistDetails(playlistId) }

                _selectedPlaylist.value = selectedPlaylistDeferred.await()[0]

                com.craftworks.music.ui.screens.selectedPlaylist = _selectedPlaylist.value!!
            }
        }
    }
}