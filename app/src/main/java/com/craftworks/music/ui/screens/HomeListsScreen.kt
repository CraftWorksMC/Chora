package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.elements.AlbumGrid
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun HomeListsScreen(
    albums: List<MediaItem>,
    viewModel: HomeScreenViewModel,
    categoryKey: String,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null
) {
    val titleRes = when (categoryKey) {
        "recently_played" -> R.string.recently_played
        "recently_added" -> R.string.recently_added
        "most_played" -> R.string.most_played
        "random_songs" -> R.string.random_songs
        else -> R.string.recently_played
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
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
                onGetAlbum = { viewModel.getAlbumSongs(it) }
            )
        }
    }
}