package com.craftworks.music.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.getAlbum
import com.craftworks.music.providers.navidrome.setNavidromeStar
import com.craftworks.music.shuffleSongs
import com.craftworks.music.ui.elements.BottomSpacer
import com.craftworks.music.ui.elements.GenrePill
import com.craftworks.music.ui.elements.HorizontalSongCard
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import kotlinx.coroutines.launch

var selectedAlbum by mutableStateOf<MediaData.Album?>(MediaData.Album(navidromeID = "", parent = "", album = "", title = "", name = "", songCount = 0, duration = 0, artistId = "", artist = "", coverArt = ""))

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun AlbumDetails(
    selectedAlbumId: String = "",
    selectedAlbumImage: Uri = Uri.EMPTY,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null
) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red.copy(0.75f), Color.Transparent))

    var currentAlbum by remember {
        mutableStateOf(
            MediaData.Album(
                navidromeID = selectedAlbumId,
                parent = "",
                coverArt = "",
                songCount = 0,
                duration = 0,
                artist = "",
                artistId = ""
            )
        )
    }
    var isStarred by remember { mutableStateOf(currentAlbum.starred.isNullOrEmpty()) }
    val requester = FocusRequester()

    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        requester.requestFocus()
    }

    LaunchedEffect(selectedAlbumId) {
        currentAlbum = getAlbum(selectedAlbumId) ?: currentAlbum
        isStarred = currentAlbum.starred.isNullOrEmpty()
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = leftPadding,
            top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()
        )
        .wrapContentHeight()
        .verticalScroll(rememberScrollState())
        .dialogFocusable()
    ) {
        Box (modifier = Modifier
            .padding(horizontal = 12.dp)
            .height(224.dp)
            .fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(selectedAlbumImage)
                    .size(256)
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
                    text = currentAlbum.name ?: "Unknown",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 32.sp,
                )
                Row (modifier = Modifier.fillMaxWidth() ,horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = "${currentAlbum.artist.substringBefore(",")} • ",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = formatMilliseconds(currentAlbum.duration),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        textAlign = TextAlign.Center
                    )
                }

                // Genres
                if (!currentAlbum.genres.isNullOrEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        items(currentAlbum.genres ?: emptyList()) { genre ->
                            genre.name?.let { GenrePill(it) }
                        }
                    }
                }
            }

            // Star/unstar button
            Button(
                onClick = {
                    coroutineScope.launch {
                        setNavidromeStar(
                            star = isStarred,
                            albumId = currentAlbum.navidromeID
                        )
                        // Reload album data to update cache
                        currentAlbum = getAlbum(selectedAlbumId, true) ?: currentAlbum
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


        // Play and shuffle buttons
        Row (modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    //SongHelper.currentSong = currentAlbum.songs!![0]
                    SongHelper.currentList = currentAlbum.songs!!
                    SongHelper.playStream(
                        context,
                        Uri.parse(currentAlbum.songs!![0].media),
                        false,
                        mediaController
                    )
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

                    val random = currentAlbum.songs!!.indices.random()
                    //SongHelper.currentSong = currentAlbum.songs!![random]
                    SongHelper.currentList = currentAlbum.songs!!
                    SongHelper.playStream(
                        context,
                        Uri.parse(currentAlbum.songs!![random].media),
                        false,
                        mediaController
                    )
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

        // Songs List
        Column(modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, top = 0.dp)
            .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                for(song in currentAlbum.songs!!){
                    HorizontalSongCard(song = song, onClick = {
                        //SongHelper.currentSong = song
                        SongHelper.currentList = currentAlbum.songs!!
                        song.media?.let { SongHelper.playStream(context, Uri.parse(it), false, mediaController)}
                    })
                }
            }
        }

        if(showAddSongToPlaylistDialog.value)
            AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )

        BottomSpacer()
    }
}