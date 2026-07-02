package com.craftworks.music.ui.screens.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.formatSeconds
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.elements.dialogs.tv.SongDialog
import com.craftworks.music.ui.elements.tv.TvHorizontalSongCard
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel
import kotlinx.coroutines.launch


@Preview(showBackground = true, showSystemUi = false)
@Composable
fun TvPlaylistDetails(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = rememberManagedMediaController().value,
    viewModel: PlaylistScreenViewModel = hiltViewModel()
) {
    val playlistMetadata =
        viewModel.selectedPlaylist.collectAsStateWithLifecycle().value?.mediaMetadata

    val playlistSongs = viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle().value
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value

    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var showSongDialog by remember { mutableStateOf(false) }

    val playlistDuration =
        remember(playlistSongs) { playlistSongs.sumOf { it.mediaMetadata.durationMs ?: 0 } }

    val playRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { playRequester.requestFocus() }

    AnimatedVisibility(
        visible = !isLoading,
        enter = fadeIn()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val playRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { playRequester.requestFocus() }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .padding(vertical = 24.dp)
                    .focusRestorer(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlistMetadata?.artworkUri)
                        .diskCacheKey(playlistMetadata?.extras?.getString("navidromeID"))
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.placeholder),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Column(
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Title
                    Text(
                        text = playlistMetadata?.title?.toString() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // duration
                    Text(
                        text = formatSeconds((playlistDuration / 1000).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button (
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    playlistSongs,
                                    0,
                                    mediaController
                                )
                                navHostController.navigate(Screen.NowPlayingLandscape.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(playRequester),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.Action_Play))
                    }

                    OutlinedButton(
                        onClick = {
                            mediaController?.shuffleModeEnabled = true
                            coroutineScope.launch {
                                val random = playlistSongs.indices.random()
                                SongHelper.play(
                                    playlistSongs,
                                    random,
                                    mediaController
                                )
                                navHostController.navigate(Screen.NowPlayingLandscape.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.round_shuffle_28),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.Action_Shuffle))
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusGroup()
                    .focusRestorer(),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlistSongs) { song ->
                    TvHorizontalSongCard (
                        song = song,
                        showTrackNumber = false,
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(playlistSongs, playlistSongs.indexOf(song), mediaController)
                                navHostController.navigate(Screen.NowPlayingLandscape.route) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onLongClick = {
                            selectedSong = song
                            showSongDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showSongDialog)
        SongDialog(
            song = selectedSong,
            setShowDialog = { showSongDialog = it }
        )
}