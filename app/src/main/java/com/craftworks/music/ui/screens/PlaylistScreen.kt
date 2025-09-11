package com.craftworks.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.ui.elements.PlaylistGrid
import com.craftworks.music.ui.elements.RippleEffect
import com.craftworks.music.ui.elements.dialogs.DeletePlaylist
import com.craftworks.music.ui.elements.dialogs.showDeletePlaylistDialog
import com.craftworks.music.ui.playing.dpToPx
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

    val state = rememberPullToRefreshState()
    val isRefreshing by viewModel.isLoading.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showRipple by remember { mutableIntStateOf(0) }
    val rippleXOffset = LocalWindowInfo.current.containerSize.width / 2
    val rippleYOffset = dpToPx(12)

    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            viewModel.loadPlaylists()
        }
        showRipple++
    }

    LaunchedEffect(playlists) {
        viewModel.updatePlaylistsImages(context)
    }

    PullToRefreshBox(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )) {
            /* HEADER */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(70.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.placeholder),
                    contentDescription = "Songs Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.playlists),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize
                )
            }

            PlaylistGrid(playlists, onPlaylistSelected = { playlist ->
                viewModel.setCurrentPlaylist(playlist)
                navHostController.navigate(Screen.PlaylistDetails.route) {
                    launchSingleTop = true
                }
            })

            if(showDeletePlaylistDialog.value)
                DeletePlaylist(
                    setShowDialog =  { showDeletePlaylistDialog.value = it },
                    viewModel
                )
        }
    }

    RippleEffect(
        center = Offset(rippleXOffset.toFloat(), rippleYOffset.toFloat()),
        color = MaterialTheme.colorScheme.surfaceVariant,
        key = showRipple
    )
}