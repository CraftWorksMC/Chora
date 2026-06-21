package com.craftworks.music.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaItem
import com.craftworks.music.data.model.ProviderFeatures
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatSeconds
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.downloadNavidromeAlbum
import com.craftworks.music.ui.elements.GenrePill
import com.craftworks.music.ui.elements.HorizontalSongCard
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import com.craftworks.music.ui.viewmodels.AlbumDetailsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun AlbumDetails(
    selectedAlbumId: String = "",
    selectedAlbumImage: Uri = Uri.EMPTY,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: AlbumDetailsViewModel = hiltViewModel()
) {
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red.copy(0.75f), Color.Transparent))

    var showLoading by remember { mutableStateOf(false) }
    val currentAlbum = viewModel.albumDetails.collectAsStateWithLifecycle().value
    val showTrackNumbers by AppearanceSettingsManager(LocalContext.current).showTrackNumbersFlow.collectAsStateWithLifecycle(false)

    val context = LocalContext.current

    LaunchedEffect(selectedAlbumId) {
        viewModel.loadAlbumDetails(selectedAlbumId)
    }

    // Loading spinner
    AnimatedVisibility(
        visible = showLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Text(
                text = "Loading",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    // Main Content
    AnimatedVisibility(
        visible = currentAlbum != null,
        enter = fadeIn()
    ) {
        val requester = remember { FocusRequester() }

        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            requester.requestFocus()
        }

        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp
            )
            .dialogFocusable(),
            contentPadding = PaddingValues(bottom = 16.dp, top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()),
        ) {
            // Header
            item {
                Box (modifier = Modifier
                    .height(224.dp)
                    .fillMaxWidth()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedAlbumImage)
                            .diskCacheKey(selectedAlbumId)
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(R.drawable.placeholder),
                        fallback = painterResource(R.drawable.placeholder),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = "Album Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadingEdge(imageFadingEdge)
                            .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
                            .blur(8.dp)
                    )
                    Button(
                        onClick = { navHostController.popBackStack() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(top = 12.dp, start = 12.dp)
                            .size(32.dp),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Settings",
                            modifier = Modifier
                                .height(32.dp)
                                .size(32.dp)
                        )
                    }
                    // Album Name and Artist
                    Column(modifier = Modifier.align(Alignment.BottomCenter)){
                        Text(
                            text = currentAlbum?.name ?: "",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 32.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = currentAlbum?.albumArtistName.toString() + " • " + formatSeconds(currentAlbum?.duration?.div(1000) ?: 0),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Genres
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (!currentAlbum?.genres.isNullOrEmpty()) {
                                currentAlbum.genres.forEach {
                                    GenrePill(it.toString())
                                }
                            }
                        }
                    }

                    // Star/unstar button
                    if (viewModel.provider?.featureFlags?.value?.any(ProviderFeatures.FAVORITES)?:false) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (currentAlbum == null) return@launch
                                    if (currentAlbum.userFavorite ?: false)
                                        viewModel.unstarAlbum(
                                            currentAlbum.id
                                        )
                                    else
                                        viewModel.starAlbum(
                                            currentAlbum.id
                                        )
                                    currentAlbum.userFavorite != currentAlbum.userFavorite;
                                    viewModel.loadAlbumDetails(selectedAlbumId)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 12.dp)
                                .size(32.dp),
                            contentPadding = PaddingValues(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Crossfade(
                                targetState = currentAlbum!!.userFavorite
                            ) {
                                if (it) Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.round_favorite_24),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .size(28.dp)
                                )
                                else
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.round_favorite_border_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .height(28.dp)
                                            .size(28.dp)
                                    )
                            }
                        }
                    }

                    // Download album
                    if (viewModel.provider?.featureFlags?.value?.any(ProviderFeatures.DOWNLOADS)?:false) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    TODO("Download album")
                                    //downloadNavidromeAlbum(context, currentAlbum?.name.toString(), currentAlbum?.subList(1, currentAlbum.size))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 52.dp)
                                .size(32.dp),
                            contentPadding = PaddingValues(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
                                contentDescription = "Unstar Album",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(28.dp)
                                    .size(28.dp)
                            )
                        }
                    }
                }
            }

            // Play and shuffle buttons
            item {
                Row (modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    currentAlbum?.songs?:listOf(),
                                    0,
                                    mediaController
                                )
                            }
                        },
                        modifier = Modifier
                            .widthIn(min = 128.dp, max = 320.dp)
                            .focusRequester(requester)
                    ) {
                        Row (verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, "Play Album")
                            Text(stringResource(R.string.Action_Play), maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            mediaController?.shuffleModeEnabled = true
                            coroutineScope.launch {
                            SongHelper.play(
                                (currentAlbum?.songs?:listOf()).shuffled(),
                                0,
                                mediaController
                            )
                                }
                        },
                        modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                            Icon(ImageVector.vectorResource(R.drawable.round_shuffle_28), "Shuffle Album")
                            Text(stringResource(R.string.Action_Shuffle), maxLines = 1)
                        }
                    }
                }
            }

            // Album Songs
            val groupedAlbums = (currentAlbum?.songs ?: listOf()).groupBy { song ->
                song.discNumber
            }

            if (groupedAlbums.size > 1) {
                groupedAlbums.forEach { (discNumber, songsInGroup) ->
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.Album_Disc_Number) + discNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(1.dp)
                                    .fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }
                    }
                    items(songsInGroup) {
                        HorizontalSongCard(
                            song = it,
                            modifier = Modifier.animateItem(),
                            showTrackNumber = showTrackNumbers,
                            onClick = {
                                coroutineScope.launch {
                                    SongHelper.play(
                                        currentAlbum?.songs?:listOf(),
                                        currentAlbum?.songs?.indexOf(it)?:0,
                                        mediaController
                                    )
                                }
                            },
                            onAddToQueue = {
                                mediaController?.addMediaItem(song)
                            }
                        )
                    }
                }
            }
            else {
                items(currentAlbum?.songs?:listOf()) {
                    HorizontalSongCard(
                        song = it,
                        modifier = Modifier.animateItem(),
                        showTrackNumber = showTrackNumbers,
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    currentAlbum?.songs?:listOf(),
                                    currentAlbum?.songs?.indexOf(it)?:0,
                                    mediaController
                                )
                            }
                        },
                        onAddToQueue = {
                            mediaController?.addMediaItem(song)
                        }
                    )
                }
            }
        }
    }

    if(showAddSongToPlaylistDialog.value)
        AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )

}