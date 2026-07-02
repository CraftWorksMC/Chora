package com.craftworks.music.ui.screens.tv

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.tv.TvAlbumCard
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
@Preview
fun TvArtistDetailsScreen(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: ArtistsScreenViewModel = hiltViewModel()
) {
    val showLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val artist = viewModel.selectedArtist.collectAsStateWithLifecycle().value
    val artistAlbums = viewModel.artistAlbums.collectAsStateWithLifecycle().value

    AnimatedVisibility(
        visible = showLoading || artist == null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
        }
    }

    AnimatedVisibility(
        visible = !showLoading,
        enter = fadeIn()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val playRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            playRequester.requestFocus()
        }
        val groupedAlbums =
            artistAlbums.groupBy { it.mediaMetadata.recordingYear }
                .toSortedMap(compareByDescending { it })

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(5) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artist?.imageUrl)  // TODO("Call provider's getImageUrl")
                            .diskCacheKey(artist?.id)
                            .crossfade(true)
                            .build(),
                        fallback = painterResource(R.drawable.rounded_artist_24),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                    )

                    // Artist Name
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = artist?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Biography
                        Text(
                            text = artist?.biography?.split("<a target")?.first() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val allArtistSongsList = artistAlbums.map {
                                            it.mediaMetadata.extras?.getString("navidromeID").let {
                                                val album = viewModel.getAlbum(it ?: "")
                                                if (album.isNotEmpty())
                                                    album.subList(1, album.size)
                                                else
                                                    emptyList()
                                            }
                                        }

                                        SongHelper.play(
                                            allArtistSongsList.flatten(),
                                            0,
                                            mediaController
                                        )
                                        navHostController.navigate(Screen.NowPlayingLandscape.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
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
                                        val allArtistSongsList = artistAlbums.map {
                                            it.mediaMetadata.extras?.getString("navidromeID").let {
                                                val album = viewModel.getAlbum(it ?: "")
                                                if (album.isNotEmpty())
                                                    album.subList(1, album.size)
                                                else
                                                    emptyList()
                                            }
                                        }

                                        mediaController?.shuffleModeEnabled = true
                                        val random = allArtistSongsList.indices.random()
                                        SongHelper.play(
                                            allArtistSongsList.flatten(),
                                            random,
                                            mediaController
                                        )
                                        navHostController.navigate(Screen.NowPlayingLandscape.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f),
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
                }
            }

            groupedAlbums.forEach { (groupName, albumsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = groupName.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                    )
                }
                items(albumsInGroup) { album ->
                    TvAlbumCard(
                        album = album,
                        onClick = {
                            val encodedImage = URLEncoder.encode(album.mediaMetadata.artworkUri.toString(), "UTF-8")
                            navHostController.navigate(Screen.AlbumDetails.route + "/${album.mediaMetadata.extras?.getString("id")}/$encodedImage") {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }
    }
}