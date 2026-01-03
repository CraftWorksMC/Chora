package com.craftworks.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.SongsHorizontalColumn
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.util.LayoutMode
import com.craftworks.music.ui.util.rememberFoldableState
import com.craftworks.music.ui.viewmodels.DownloadViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SongsScreen(
    mediaController: MediaController? = null,
    viewModel: SongsScreenViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel()
) {
    val allSongsList by viewModel.allSongs.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val foldableState = rememberFoldableState()
    val useMultiColumn = foldableState.layoutMode in listOf(
        LayoutMode.EXPANDED, LayoutMode.BOOK_MODE
    )
    val coroutineScope = rememberCoroutineScope()

    val state = rememberPullToRefreshState()
    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)

    var showAddSongToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<MediaItem?>(null) }

    val onRefresh: () -> Unit = {
        viewModel.refreshSongs()
        showRipple++
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        indicator = {}
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopBarWithSearch(
                    headerText = stringResource(R.string.songs),
                    scrollBehavior = scrollBehavior,
                    onSearch = { query -> viewModel.search(query) },
                    searchResults = {
                        Column {
                            if (searchResults.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                SongHelper.play(searchResults, 0, mediaController)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null
                                        )
                                        Text(stringResource(R.string.Action_Play))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                SongHelper.play(searchResults, 0, mediaController)
                                                mediaController?.shuffleModeEnabled = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.round_shuffle_28),
                                            contentDescription = null
                                        )
                                        Text(stringResource(R.string.Action_Shuffle))
                                    }
                                }
                            }
                            SongsHorizontalColumn(
                                songList = searchResults,
                                onSongSelected = { songs, index ->
                                    println("Starting song at index: $index")
                                    coroutineScope.launch {
                                        SongHelper.play(songs, index, mediaController)
                                    }
                                },
                                isSearch = true,
                                viewModel = viewModel,
                                useMultiColumn = useMultiColumn,
                                onDownloadSelected = { songs ->
                                    downloadViewModel.queueDownloads(songs.map { it.mediaMetadata })
                                },
                                onDownload = { song ->
                                    downloadViewModel.queueDownload(song.mediaMetadata)
                                }
                            )
                        }
                    },
                    extraAction = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val randomSongs = viewModel.getRandomSongs(50)
                                    if (randomSongs.isNotEmpty()) {
                                        SongHelper.play(randomSongs, 0, mediaController)
                                        mediaController?.shuffleModeEnabled = true
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.round_shuffle_28),
                                contentDescription = stringResource(R.string.Action_Shuffle)
                            )
                        }
                    }
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                SongsHorizontalColumn(
                    songList = allSongsList,
                    onSongSelected = { songs, index ->
                        println("Starting song at index: $index")
                        coroutineScope.launch {
                            SongHelper.play(songs, index, mediaController)
                        }
                    },
                    isSearch = false,
                    viewModel = viewModel,
                    useMultiColumn = useMultiColumn,
                    onDownloadSelected = { songs ->
                        downloadViewModel.queueDownloads(songs.map { it.mediaMetadata })
                    },
                    onDownload = { song ->
                        downloadViewModel.queueDownload(song.mediaMetadata)
                    }
                )
            }
        }
    }

    if (showAddSongToPlaylistDialog && songToAddToPlaylist != null) {
        AddSongToPlaylist(
            song = songToAddToPlaylist!!,
            onDismiss = { showAddSongToPlaylistDialog = false }
        )
    }

    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )
}