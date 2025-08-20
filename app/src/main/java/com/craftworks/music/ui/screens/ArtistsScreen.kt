package com.craftworks.music.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.elements.ArtistsGrid
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.TopBarWithSearch
import com.craftworks.music.ui.playing.dpToPx
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel

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

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopBarWithSearch(
                    headerIcon = ImageVector.vectorResource(R.drawable.rounded_artist_24),
                    headerText = stringResource(R.string.Artists),
                    onSearch = { query -> viewModel.onSearchQueryChange(query) },
                ) {
                    ArtistsGrid(searchResults, onArtistSelected = { artist ->
                        viewModel.setSelectedArtist(artist)
                        navHostController.navigate(Screen.ArtistDetails.route) {
                            launchSingleTop = true
                        }
                    })
                }
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