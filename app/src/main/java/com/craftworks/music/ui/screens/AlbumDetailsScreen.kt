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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.media3.common.MediaItem
import androidx.media3.common.StarRating
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.ProviderFeatures
import com.craftworks.music.data.model.favorite
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatSeconds
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.GenrePill
import com.craftworks.music.ui.elements.HorizontalSongCard
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.RatingDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import com.craftworks.music.ui.viewmodels.AlbumDetailsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
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

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    var showLoading by remember { mutableStateOf(false) }
    val currentAlbum = viewModel.songsInAlbum.collectAsStateWithLifecycle().value
    val showTrackNumbers by AppearanceSettingsManager(LocalContext.current).showTrackNumbersFlow.collectAsStateWithLifecycle(false)

    var songToRate by remember { mutableStateOf<MediaItem?>(null) }

    val context = LocalContext.current

    LaunchedEffect(selectedAlbumId) {
        showLoading = false

        viewModel.loadAlbumDetails(selectedAlbumId)

        delay(500.milliseconds)
        showLoading = true
    }

    // Loading spinner
    AnimatedVisibility(
        visible = currentAlbum.isEmpty() && showLoading,
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
        visible = currentAlbum.isNotEmpty(),
        enter = fadeIn()
    ) {
        var isStarred by remember { mutableStateOf(currentAlbum[0].mediaMetadata.favorite ?: false) }
        val requester = remember { FocusRequester() }

        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            requester.requestFocus()
        }

        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .dialogFocusable(),
            contentPadding = PaddingValues(bottom = 16.dp, top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()),
        ) {
            // Header
            item {
                Box (modifier = Modifier
                    .height(260.dp)
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
                    Row(modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, end = 12.dp, bottom = 36.dp)) {
                        // Album Name and Artist
                        Column(modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                        ){
                            Text(
                                text = currentAlbum[0].mediaMetadata.title.toString(),
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Left,
                                lineHeight = 32.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = currentAlbum[0].mediaMetadata.artist.toString() + " • " + formatSeconds(currentAlbum[0].mediaMetadata.durationMs?.div(1000)?.toInt() ?: 0),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                textAlign = TextAlign.Left
                            )

                            // Genres
                            Row(horizontalArrangement = Arrangement.Start) {
                                if (!currentAlbum[0].mediaMetadata.genre.isNullOrEmpty()) {
                                    currentAlbum[0].mediaMetadata.genre?.split(",")?.forEach {
                                        GenrePill(it)
                                    }
                                }
                            }
                        }

                        // Play, shuffle and more buttons
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    showBottomSheet = true
                                },
                                modifier = Modifier.size(46.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Rounded.MoreVert, "More") // TODO : translate?
                            }
                            if (showBottomSheet) {
                                ModalBottomSheet(
                                    onDismissRequest = { showBottomSheet = false },
                                    sheetState = sheetState
                                ) {
                                    // Content inside the bottom sheet
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                showBottomSheet = false
                                                coroutineScope.launch {
                                                    mediaController?.addMediaItems(
                                                        currentAlbum.subList(1, currentAlbum.size)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.focusRequester(requester)
                                        ) {
                                            Row (verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Icon(ImageVector.vectorResource(R.drawable.outline_queue_add_24), stringResource(R.string.action_add_to_queue))
                                                Text(stringResource(R.string.action_add_to_queue), maxLines = 1)
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                showBottomSheet = false
                                                coroutineScope.launch {
                                                    mediaController?.addMediaItems(
                                                        mediaController.currentMediaItemIndex+1,
                                                        currentAlbum.subList(1, currentAlbum.size)

                                                    )
                                                }
                                            },
                                            modifier = Modifier.focusRequester(requester)
                                        ) {
                                            Row (verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Icon(ImageVector.vectorResource(R.drawable.play_next_24px), stringResource(R.string.action_play_next))
                                                Text(stringResource(R.string.action_play_next), maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val random = currentAlbum.subList(
                                            1,
                                            currentAlbum.size
                                        ).indices.random()
                                        SongHelper.play(
                                            currentAlbum.subList(1, currentAlbum.size),
                                            random,
                                            mediaController
                                        )
                                    }
                                },
                                modifier = Modifier.size(46.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.round_shuffle_28),
                                    stringResource(R.string.action_shuffle)
                                )
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        SongHelper.play(
                                            currentAlbum.subList(1, currentAlbum.size),
                                            0,
                                            mediaController
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .focusRequester(requester),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.action_play), modifier = Modifier.size(42.dp))
                            }
                        }
                    }

                    // Star/unstar button and download album
                    if (currentAlbum[0].mediaMetadata.getProvider()?.featureFlags?.has(ProviderFeatures.FAVORITES)?:false) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (isStarred)
                                        currentAlbum[0].mediaMetadata.id?.let {
                                            viewModel.unstarAlbum(it)
                                        }
                                    else
                                        currentAlbum[0].mediaMetadata.id?.let {
                                            viewModel.starAlbum(it)
                                        }
                                    viewModel.loadAlbumDetails(selectedAlbumId)
                                    isStarred = !isStarred
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp, end = 12.dp)
                                .size(32.dp),
                            contentPadding = PaddingValues(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Crossfade(
                                targetState = isStarred
                            ) {
                                if (it) Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.round_favorite_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
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

                    if (currentAlbum[0].mediaMetadata.getProvider()?.featureFlags?.has(ProviderFeatures.DOWNLOADS)?:false) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    TODO("Download album")
                                    //downloadNavidromeAlbum(context, currentAlbum[0].mediaMetadata.title.toString(), currentAlbum.subList(1, currentAlbum.size))
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
            /*item {
                Row (modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(
                        horizontal = 12.dp
                    ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    currentAlbum.subList(1, currentAlbum.size),
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
                            Text(stringResource(R.string.action_play), maxLines = 1)
                        }
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
                            }
                        },
                        modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                            Icon(ImageVector.vectorResource(R.drawable.round_shuffle_28), "Shuffle Album")
                            Text(stringResource(R.string.action_shuffle), maxLines = 1)
                        }
                    }
                }
            }*/

            // Album Songs
            val groupedAlbums = currentAlbum.subList(1, currentAlbum.size).groupBy { song ->
                song.mediaMetadata.discNumber
            }

            if (groupedAlbums.size > 1) {
                groupedAlbums.forEach { (discNumber, albumsInGroup) ->
                    item() {
                        Column(modifier = Modifier
                            .padding(
                                horizontal = 12.dp
                            )) {
                            Text(
                                text = stringResource(R.string.album_details_disc, discNumber.toString()),
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
                    items(albumsInGroup) { song ->
                        HorizontalSongCard(
                            song = song,
                            modifier = Modifier.animateItem(),
                            showTrackNumber = showTrackNumbers,
                            onClick = {
                                coroutineScope.launch {
                                    SongHelper.play(
                                        currentAlbum.subList(1, currentAlbum.size),
                                        currentAlbum.subList(1, currentAlbum.size).indexOf(song),
                                        mediaController
                                    )
                                }
                            },
                            onAddToQueue = {
                                mediaController?.addMediaItem(song)
                            },
                            onSetRating = { songToRate = song }
                        )
                    }
                }
            }
            else {
                items(currentAlbum.subList(1, currentAlbum.size)) { song ->
                    HorizontalSongCard(
                        song = song,
                        modifier = Modifier.animateItem(),
                        showTrackNumber = showTrackNumbers,
                        onClick = {
                            coroutineScope.launch {
                                SongHelper.play(
                                    currentAlbum.subList(1, currentAlbum.size),
                                    currentAlbum.subList(1, currentAlbum.size).indexOf(song),
                                    mediaController
                                )
                            }
                        },
                        onAddToQueue = {
                            mediaController?.addMediaItem(song)
                        },
                        onSetRating = { songToRate = song }
                    )
                }
            }
        }
    }

    if(showAddSongToPlaylistDialog.value)
        AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )

    songToRate?.let { song ->
        RatingDialog(
            currentRating = (song.mediaMetadata.userRating as? StarRating)?.starRating?.toInt() ?: 0,
            onDismiss = { songToRate = null },
            onSetRating = { rating ->
                viewModel.setSongRating(song.mediaMetadata.id ?: "", rating)
                songToRate = null
            }
        )
    }
}