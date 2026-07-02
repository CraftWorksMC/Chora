package com.craftworks.music.ui.screens.tv

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.formatSeconds
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.SongDialog
import com.craftworks.music.ui.elements.tv.TvHorizontalSongCard
import com.craftworks.music.ui.viewmodels.AlbumDetailsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvAlbumDetails(
    selectedAlbumId: String = "",
    selectedAlbumImage: Uri = Uri.EMPTY,
    mediaController: MediaController? = null,
    navHostController: NavHostController = rememberNavController(),
    viewModel: AlbumDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentAlbum = viewModel.songsInAlbum.collectAsStateWithLifecycle().value
    val showTrackNumbers by AppearanceSettingsManager(context)
        .showTrackNumbersFlow.collectAsStateWithLifecycle(false)

    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var showSongDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedAlbumId) {
        viewModel.loadAlbumDetails(selectedAlbumId)
    }

    AnimatedVisibility(
        visible = currentAlbum.isEmpty(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
        }
    }

    AnimatedVisibility(
        visible = currentAlbum.isNotEmpty(),
        enter = fadeIn()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val playRequester = remember { FocusRequester() }

        var isStarred by remember {
            mutableStateOf(
                currentAlbum[0].mediaMetadata.extras
                    ?.getString("starred")?.isNotEmpty() ?: false
            )
        }

        LaunchedEffect(Unit) { playRequester.requestFocus() }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .padding(vertical = 24.dp)
                    .focusRestorer()
                    .focusGroup(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedAlbumImage.toString().replace("size=128", "size=500"))
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .placeholderMemoryCacheKey(selectedAlbumImage.toString())
                        .crossfade(true)
                        .build(),
                    fallback = painterResource(R.drawable.placeholder),
                    contentScale = ContentScale.Crop,
                    contentDescription = currentAlbum[0].mediaMetadata.title?.toString(),
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                // Title
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentAlbum[0].mediaMetadata.title?.toString() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Artist · duration
                    Text(
                        text = (currentAlbum[0].mediaMetadata.artist?.toString() ?: "") +
                                " · " +
                                formatSeconds(
                                    currentAlbum[0].mediaMetadata.durationMs
                                        ?.div(1000)?.toInt() ?: 0
                                ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Genres
                    if (!currentAlbum[0].mediaMetadata.genre.isNullOrEmpty()) {
                        Text(
                            text = currentAlbum[0].mediaMetadata.genre.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button (
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    currentAlbum.subList(1, currentAlbum.size),
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
                                val random = currentAlbum.subList(1, currentAlbum.size).indices.random()
                                SongHelper.play(
                                    currentAlbum.subList(1, currentAlbum.size),
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

            val songs = currentAlbum.subList(1, currentAlbum.size)
            val groupedSongs = songs.groupBy { it.mediaMetadata.discNumber }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .focusGroup()
                    .fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (groupedSongs.size > 1) {
                    groupedSongs.forEach { (discNumber, disc) ->
                        item {
                            Column {
                                Text(
                                    text = stringResource(R.string.Album_Disc_Number) +
                                            discNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.2f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        items(disc) { song ->
                            TvHorizontalSongCard (
                                song = song,
                                showTrackNumber = showTrackNumbers,
                                onClick = {
                                    coroutineScope.launch {
                                        SongHelper.play(songs, songs.indexOf(song), mediaController)
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
                } else {
                    items(songs) { song ->
                        TvHorizontalSongCard (
                            song = song,
                            showTrackNumber = showTrackNumbers,
                            onClick = {
                                coroutineScope.launch {
                                    SongHelper.play(songs, songs.indexOf(song), mediaController)
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
    }

    if (showSongDialog)
        SongDialog(
            song = selectedSong,
            setShowDialog = { showSongDialog = it }
        )
}