package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.elements.AlbumGrid
import com.craftworks.music.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.runBlocking
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

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                /*
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        )
                        .size(48.dp)
                )
                Spacer(Modifier.width(6.dp))
                */
                Text(
                    text = stringResource(titleRes),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    modifier = Modifier.padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )
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
                    val encodedImage = URLEncoder.encode(album.coverArt, "UTF-8")
                    navHostController.navigate(Screen.AlbumDetails.route + "/${album.navidromeID}/$encodedImage") {
                        launchSingleTop = true
                    }
                },
                onGetAlbum = { runBlocking { viewModel.getAlbumSongs(it) } }
            )
        }
    }
}