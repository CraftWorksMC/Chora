package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.AlbumGrid
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.util.rememberFoldableState
import com.craftworks.music.ui.util.responsiveGridCells
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun AlbumScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: AlbumScreenViewModel = hiltViewModel(),
) {
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val foldableState = rememberFoldableState()
    val gridColumns = responsiveGridCells()

    val state = rememberPullToRefreshState()
    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)

    val onRefresh: () -> Unit = {
        viewModel.refreshAlbums()
        showRipple++
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showSortMenu by remember { mutableStateOf(false) }

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        indicator = {}
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    TopBarWithSearch(
                        headerText = stringResource(R.string.Albums),
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
                                                    val songs = viewModel.getSongsForAlbums(searchResults)
                                                    if (songs.isNotEmpty()) {
                                                        SongHelper.play(songs, 0, mediaController)
                                                    }
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
                                                    val songs = viewModel.getSongsForAlbums(searchResults)
                                                    if (songs.isNotEmpty()) {
                                                        SongHelper.play(songs.shuffled(), 0, mediaController)
                                                        mediaController?.shuffleModeEnabled = true
                                                    }
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
                                AlbumGrid(
                                    searchResults,
                                    mediaController,
                                    onAlbumSelected = { album ->
                                        val encodedImage = URLEncoder.encode(album.coverArt ?: "", "UTF-8")
                                        navHostController.navigate(Screen.AlbumDetails.route + "/${album.navidromeID}?image=$encodedImage") {
                                            launchSingleTop = true
                                        }
                                    },
                                    true,
                                    viewModel,
                                    gridColumns
                                )
                            }
                        },
                        extraAction = {
                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                                        contentDescription = stringResource(R.string.Label_Sorting),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.Label_Sort_Alphabetical)) },
                                        onClick = {
                                            viewModel.setSorting(SortOrder.ALPHABETICAL)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.recently_added)) },
                                        onClick = {
                                            viewModel.setSorting(SortOrder.NEWEST)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.recently_played)) },
                                        onClick = {
                                            viewModel.setSorting(SortOrder.RECENT)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.most_played)) },
                                        onClick = {
                                            viewModel.setSorting(SortOrder.FREQUENT)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.Label_Sort_Starred)) },
                                        onClick = {
                                            viewModel.setSorting(SortOrder.STARRED)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                AlbumGrid(
                    albums,
                    mediaController,
                    onAlbumSelected = { album ->
                        val encodedImage = URLEncoder.encode(album.coverArt ?: "", "UTF-8")
                        navHostController.navigate(Screen.AlbumDetails.route + "/${album.navidromeID}?image=$encodedImage") {
                            launchSingleTop = true
                        }
                    },
                    false,
                    viewModel,
                    gridColumns
                )
            }
        }
    }

    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )
}