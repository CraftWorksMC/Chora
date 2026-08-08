package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumListSort
import com.craftworks.music.data.model.ProviderFeatures
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.ui.elements.AlbumGrid
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
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

    val state = rememberPullToRefreshState()
    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)

    val onRefresh: () -> Unit = {
        viewModel.getAlbums()
        showRipple++
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showSortMenu by remember { mutableStateOf(false) }

    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()

    val sortTranslationBindings = mapOf(
        AlbumListSort.ALBUM_ARTIST to R.string.sort_by_album_artist,
        AlbumListSort.ARTIST to R.string.sort_by_artist,
        AlbumListSort.DURATION to R.string.sort_by_duration,
        AlbumListSort.EXPLICIT_STATUS to R.string.sort_by_explicit_status,
        AlbumListSort.FAVORITE to R.string.sort_by_favorite,
        AlbumListSort.NAME to R.string.sort_by_name,
        AlbumListSort.PLAY_COUNT to R.string.sort_by_play_count,
        AlbumListSort.RANDOM to R.string.sort_by_random,
        AlbumListSort.RATING to R.string.sort_by_rating,
        AlbumListSort.RECENTLY_ADDED to R.string.sort_by_recently_added,
        AlbumListSort.RECENTLY_PLAYED to R.string.sort_by_recently_played,
        AlbumListSort.RELEASE_DATE to R.string.sort_by_release_date,
        AlbumListSort.SONG_COUNT to R.string.sort_by_song_count,
        AlbumListSort.YEAR to R.string.sort_by_year,
    )

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    TopBarWithSearch(
                        headerText = stringResource(R.string.nav_albums),
                        scrollBehavior = scrollBehavior,
                        onSearch = { query -> viewModel.search(query) },
                        searchResults = {
                            AlbumGrid(
                                searchResults,
                                mediaController,
                                onAlbumSelected = { album ->
                                    val encodedImage = URLEncoder.encode(album.mediaMetadata.artworkUri.toString(), "UTF-8")
                                    navHostController.navigate(Screen.AlbumDetails.route + "/${album.mediaMetadata.extras?.getString("id")}/$encodedImage") {
                                        launchSingleTop = true
                                    }
                                },
                                true,
                                viewModel
                            )
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
                                if (currentProvider?.supportAlbumSortOrder ?: false) {
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
                                if (currentProvider?.supportedAlbumSort.orEmpty().size > 1) {
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
                                            currentProvider?.supportedAlbumSort.orEmpty().map {
                                                return@map DropdownMenuItem(
                                                    text = {
                                                        Text(sortTranslationBindings[it]?.let { id ->
                                                            stringResource(
                                                                id
                                                            )
                                                        } ?: it.name)
                                                    },
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
                        val encodedImage = URLEncoder.encode(album.mediaMetadata.artworkUri.toString(), "UTF-8")
                        navHostController.navigate(Screen.AlbumDetails.route + "/${album.mediaMetadata.extras?.getString("id")}/$encodedImage") {
                            launchSingleTop = true
                        }
                    },
                    false,
                    viewModel
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