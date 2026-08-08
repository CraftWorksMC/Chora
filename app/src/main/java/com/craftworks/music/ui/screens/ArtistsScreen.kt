package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.ProviderFeatures
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.ui.elements.ArtistsGrid
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun ArtistsScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: ArtistsScreenViewModel = hiltViewModel()
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allArtistList by viewModel.allArtists.collectAsStateWithLifecycle()

    val state = rememberPullToRefreshState()
    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)

    val onRefresh: () -> Unit = {
        viewModel.getArtists()
        showRipple++
    }

    var showSortMenu by remember { mutableStateOf(false) }

    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()


    val sortTranslationBindings = mapOf(
        AlbumArtistListSort.ALBUM_COUNT to R.string.sort_by_album_count,
        AlbumArtistListSort.DURATION to R.string.sort_by_duration,
        AlbumArtistListSort.FAVORITE to R.string.sort_by_favorite,
        AlbumArtistListSort.NAME to R.string.sort_by_name,
        AlbumArtistListSort.PLAY_COUNT to R.string.sort_by_play_count,
        AlbumArtistListSort.RANDOM to R.string.sort_by_random,
        AlbumArtistListSort.RATING to R.string.sort_by_rating,
        AlbumArtistListSort.RECENTLY_ADDED to R.string.sort_by_recently_added,
        AlbumArtistListSort.RELEASE_DATE to R.string.sort_by_release_date,
        AlbumArtistListSort.SONG_COUNT to R.string.sort_by_song_count,
    )

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopBarWithSearch(
                    headerText = stringResource(R.string.nav_artists),
                    scrollBehavior = scrollBehavior,
                    onSearch = { query -> viewModel.onSearchQueryChange(query) },
                    searchResults = {
                        ArtistsGrid(searchResults, onArtistSelected = { artist ->
                            viewModel.setSelectedArtist(artist)
                            navHostController.navigate(Screen.ArtistDetails.route) {
                                launchSingleTop = true
                            }
                        })
                    },
                    extraAction = {
                        Row {
                            if (currentProvider?.featureFlags?.has(ProviderFeatures.FAVORITES) ?: false) {
                                Box {
                                    IconButton(
                                        onClick = { viewModel.setShowFavoritesOnly(!showFavoritesOnly) }
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(if (showFavoritesOnly) androidx.media3.session.R.drawable.media3_icon_heart_filled else androidx.media3.session.R.drawable.media3_icon_heart_unfilled),
                                            contentDescription = stringResource(R.string.button_toggle_favorites),
                                        )
                                    }
                                }
                            }
                            if (currentProvider?.supportAlbumArtistSortOrder ?: false) {
                                Box {
                                    IconButton(
                                        onClick = { viewModel.setOrder(sortOrder.invert()) }
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(if (sortOrder == SortOrder.ASC) R.drawable.arrow_upward_24px else R.drawable.arrow_downward_24px),
                                            contentDescription = stringResource(R.string.button_toggle_sort_order),
                                        )
                                    }
                                }
                            }
                            if (currentProvider?.supportedAlbumArtistSort.orEmpty().size > 1) {
                                Box {
                                    IconButton(
                                        onClick = { showSortMenu = true }
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                                            contentDescription = stringResource(R.string.button_sort_by),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        currentProvider?.supportedAlbumArtistSort.orEmpty().map {
                                            return@map DropdownMenuItem(
                                                text = { Text(sortTranslationBindings[it]?.let { id -> stringResource(id) } ?: it.name) },
                                                onClick = {
                                                    viewModel.setSorting(it)
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
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
                ArtistsGrid(allArtistList, onArtistSelected = { artist ->
                    viewModel.setSelectedArtist(artist)
                    navHostController.navigate(Screen.ArtistDetails.route) {
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