package com.craftworks.music.ui.screens

import android.content.res.Configuration
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.setNavidromeStar
import com.craftworks.music.ui.elements.GenrePill
import com.craftworks.music.ui.elements.HorizontalSongCard
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import com.craftworks.music.ui.viewmodels.AlbumDetailsViewModel
import kotlinx.coroutines.launch

var selectedAlbum by mutableStateOf<MediaData.Album?>(MediaData.Album(navidromeID = "", parent = "", album = "", title = "", name = "", songCount = 0, duration = 0, artistId = "", artist = "", coverArt = ""))

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
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red.copy(0.75f), Color.Transparent))

    var showLoading by remember { mutableStateOf(false) }
    val currentAlbum = viewModel.songsInAlbum.collectAsStateWithLifecycle().value

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
                .fillMaxSize()
                .padding(start = leftPadding),
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
        var isStarred by remember { mutableStateOf(currentAlbum[0].mediaMetadata.extras?.getString("starred").isNullOrEmpty()) }
        val requester = remember { FocusRequester() }

        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            requester.requestFocus()
        }

        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = leftPadding + 12.dp,
                end = 12.dp,
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
                            .size(128)
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
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Settings",
                            modifier = Modifier
                                .height(32.dp)
                                .size(32.dp)
                        )
                    }
                    // Album Name and Artist
                    Column(modifier = Modifier.align(Alignment.BottomCenter)){
                        Text(
                            text = currentAlbum[0].mediaMetadata.title.toString(),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 32.sp,
                        )
                        Text(
                            text = currentAlbum[0].mediaMetadata.artist.toString() + " • " + formatMilliseconds(currentAlbum[0].mediaMetadata.durationMs?.div(1000)?.toInt() ?: 0),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Genres
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (!currentAlbum[0].mediaMetadata.genre.isNullOrEmpty()) {
                                currentAlbum[0].mediaMetadata.genre?.split(",")?.forEach {
                                    GenrePill(it.toString())
                                }
                            }
                        }
                    }

                    // Star/unstar button, NAVIDROME ONLY
                    if (currentAlbum[0].mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == false) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    setNavidromeStar(
                                        star = isStarred,
                                        albumId = currentAlbum[0].mediaMetadata.extras?.getString("navidromeID").toString()
                                    )
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
                                    imageVector = ImageVector.vectorResource(R.drawable.round_favorite_border_24),
                                    contentDescription = "Star Album",
                                    modifier = Modifier
                                        .height(28.dp)
                                        .size(28.dp)
                                )
                                else Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.round_favorite_24),
                                    contentDescription = "Unstar Album",
                                    modifier = Modifier
                                        .height(28.dp)
                                        .size(28.dp)
                                )
                            }
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
                                    currentAlbum.subList(1, currentAlbum.size),
                                    0,
                                    mediaController
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground),
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
                    Button(
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground),
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
            items(currentAlbum.subList(1, currentAlbum.size)) { song ->
                HorizontalSongCard(
                    song = song,
                    modifier = Modifier.animateItem(),
                    onClick = {
                        coroutineScope.launch {
                            SongHelper.play(
                                currentAlbum.subList(1, currentAlbum.size),
                                currentAlbum.subList(1, currentAlbum.size).indexOf(song),
                                mediaController
                            )
                        }
                    }
                )
            }
        }
    }

    if(showAddSongToPlaylistDialog.value)
        AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )

}