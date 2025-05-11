package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.shuffleSongs
import com.craftworks.music.ui.elements.SongsHorizontalColumn
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.viewmodels.PlaylistScreenViewModel

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun PlaylistDetails(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = rememberManagedMediaController().value,
    viewModel: PlaylistScreenViewModel = PlaylistScreenViewModel()
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red, Color.Transparent))

    val requester = FocusRequester()

    val playlistMetadata = viewModel.selectedPlaylist.collectAsStateWithLifecycle().value?.mediaMetadata
    val playlistSongs = viewModel.selectedPlaylistSongs.collectAsStateWithLifecycle().value

    val playlistDuration = remember(playlistSongs) { playlistSongs.sumOf { it.mediaMetadata.durationMs ?: 0 }  }

    LaunchedEffect(playlistMetadata?.extras?.getString("navidromeID")) {
        requester.requestFocus()
        viewModel.fetchPlaylistDetails()
    }

    println("artwork uri: ${playlistMetadata?.artworkUri}; artwork data: ${playlistMetadata?.artworkData}")

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding,
            top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()
        )
        .dialogFocusable()
    ) {
        Box (modifier = Modifier
            .padding(horizontal = 12.dp)
            .height(192.dp)
            .fillMaxWidth()) {
            SubcomposeAsyncImage (
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (playlistMetadata?.extras?.getString("navidromeID")?.startsWith("Local") == true)
                        playlistMetadata.artworkData else
                        playlistMetadata?.artworkUri)
                    .size(256)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.FillWidth,
                contentDescription = "Playlist cover art",
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
            // Playlist name
            Column(modifier = Modifier.align(Alignment.BottomCenter)){
                Text(
                    text = playlistMetadata?.title.toString(),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 32.sp,
                )
                Text(
                    text = formatMilliseconds((playlistDuration / 1000).toInt()),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Normal,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }


        // Play and shuffle buttons
        Row (modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    SongHelper.play(playlistSongs, 0, mediaController)
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
                    shuffleSongs.value = true
                    mediaController?.shuffleModeEnabled = true

                    val random = playlistSongs.indices.random()
                    SongHelper.play(playlistSongs, random, mediaController)
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

        Column(modifier = Modifier.padding(12.dp, top = 0.dp, end = 12.dp)) {
            SongsHorizontalColumn(playlistSongs, onSongSelected = { songs, index ->
                SongHelper.play(songs, index, mediaController)
            })
        }
    }
}