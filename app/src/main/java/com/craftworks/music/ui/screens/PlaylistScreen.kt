package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.elements.PlaylistGrid
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.util.TextDisplayUtils
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun PlaylistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    val state = rememberPullToRefreshState()
    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)

    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            viewModel.loadPlaylists()
        }
        showRipple++
    }

    // FIXED: Use playlists.size as key to avoid re-launching on every list recomposition
    // This ensures images are updated only when the actual playlist count changes
    LaunchedEffect(playlists.size) {
        viewModel.updatePlaylistsImages()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showSortMenu by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(true) }

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
                    headerText = stringResource(R.string.playlists),
                    scrollBehavior = scrollBehavior,
                    onSearch = { query -> viewModel.onSearchQueryChange(query) },
                    searchResults = {
                        PlaylistGrid(searchResults, onPlaylistSelected = { playlist ->
                            viewModel.setCurrentPlaylist(playlist)
                            navHostController.navigate(Screen.PlaylistDetails.route) {
                                launchSingleTop = true
                            }
                        })
                    },
                    extraAction = {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                                    contentDescription = stringResource(R.string.Label_Sorting)
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.Label_Sort_Alphabetical) + " (A-Z)") },
                                    onClick = {
                                        sortAscending = true
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.Label_Sort_Alphabetical) + " (Z-A)") },
                                    onClick = {
                                        sortAscending = false
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(
                        top = innerPadding.calculateTopPadding()
                    )
            ) {
                val sortedPlaylists = remember(playlists, sortAscending) {
                    if (sortAscending) playlists.sortedBy { TextDisplayUtils.getSortKey(it.mediaMetadata.title?.toString()) }
                    else playlists.sortedByDescending { TextDisplayUtils.getSortKey(it.mediaMetadata.title?.toString()) }
                }
                PlaylistGrid(sortedPlaylists, onPlaylistSelected = { playlist ->
                    viewModel.setCurrentPlaylist(playlist)
                    navHostController.navigate(Screen.PlaylistDetails.route) {
                        launchSingleTop = true
                    }
                })

            }
        }
    }

    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )
}