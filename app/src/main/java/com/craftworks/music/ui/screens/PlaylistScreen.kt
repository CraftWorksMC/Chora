package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.elements.PlaylistGrid
import com.craftworks.music.ui.elements.dialogs.DeletePlaylist
import com.craftworks.music.ui.elements.dialogs.showDeletePlaylistDialog
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun PlaylistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: PlaylistScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    val context = LocalContext.current

    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val state = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true

            viewModel.reloadData(context)

            isRefreshing = false
        }
    }


    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
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
                viewModel.setCurrentPlaylist(playlist)
                navHostController.navigate(Screen.PlaylistDetails.route) {
                    launchSingleTop = true
                }
            })

            if(showDeletePlaylistDialog.value)
                DeletePlaylist(
                    setShowDialog =  { showDeletePlaylistDialog.value = it },
                    onDeleted = { onRefresh.invoke()}
                )
        }
    }
}