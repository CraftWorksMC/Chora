package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.SongDialog
import com.craftworks.music.ui.elements.tv.TvHorizontalSongCard
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSongsScreen(
    mediaController: MediaController? = null,
    navHostController: NavController,
    viewModel: SongsScreenViewModel = hiltViewModel(),
) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val tabs = listOf(
        stringResource(R.string.songs),
        stringResource(R.string.Label_Sort_Starred),
    )
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

    val selectedTabIndex by remember(showFavoritesOnly) {
        derivedStateOf {
            if (showFavoritesOnly) 1 else 0
        }
    }

    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var showSongDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val tabFocusRequester = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (NavidromeManager.checkActiveServers()) {
        LaunchedEffect(songs.size) {
            if (songs.size % 50 != 0) return@LaunchedEffect
            if (songs.size < 50) return@LaunchedEffect

            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?: return@snapshotFlow false
                val total = gridState.layoutInfo.totalItemsCount
                if (total < songs.size - 5) return@snapshotFlow false
                (total - lastVisible) <= 15
            }.filter { it }.collect {
                viewModel.getMoreSongs(50)
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRequester(focusRequester)
            .focusRestorer(focusRequester),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
    ) {
        item() {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .focusGroup()
                    .focusRestorer(tabFocusRequester)
                    .fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, tab ->
                    key(index) {
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = {
                                if (index == 1)
                                    viewModel.setShowFavoritesOnly(true)
                                else
                                    viewModel.setShowFavoritesOnly(false)
                            },
                            modifier = if (index == selectedTabIndex)
                                Modifier.focusRequester(tabFocusRequester)
                            else
                                Modifier
                        ) {
                            Text(
                                text = tab,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(songs) { index, song ->
            TvHorizontalSongCard(
                song = song,
                modifier = Modifier.onFocusChanged {
                    focusRequester.saveFocusedChild()
                },
                onClick = {
                    coroutineScope.launch {
                        SongHelper.play(songs, index, mediaController)
                        navHostController.navigate(Screen.NowPlayingLandscape.route) {
                            launchSingleTop = true
                        }
                    }
                },
                onLongClick = {
                    selectedSong = song
                    showSongDialog = true
                }
            )
        }
    }

    if (showSongDialog)
        SongDialog(
            song = selectedSong,
            setShowDialog = { showSongDialog = it }
        )
}