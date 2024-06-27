package com.craftworks.music.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.Screen
import com.craftworks.music.data.artistList
import com.craftworks.music.data.selectedArtist
import com.craftworks.music.data.songsList
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.getNavidromeAlbumSongs
import com.craftworks.music.shuffleSongs
import com.craftworks.music.ui.elements.BottomSpacer
import com.craftworks.music.ui.elements.HorizontalSongCard
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.showAddSongToPlaylistDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

var selectedAlbum by mutableStateOf<MediaData.Album?>(MediaData.Album(navidromeID = "", parent = "", album = "", title = "", name = "", songCount = 0, duration = 0, artistId = "", artist = "", coverArt = ""))

@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = true, locale = "it")
@Composable
fun AlbumDetails(
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null
) {

    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red.copy(0.75f), Color.Transparent))

    var albumSongs by remember { mutableStateOf<List<MediaData.Song>>(emptyList()) }

    LaunchedEffect(selectedAlbum?.songs) {
        albumSongs = selectedAlbum?.songs!!
        if (selectedAlbum?.songs?.isNotEmpty() == true) return@LaunchedEffect
        if (useNavidromeServer.value) {
            selectedAlbum?.navidromeID?.let { albumId ->
                withContext(Dispatchers.IO){ getNavidromeAlbumSongs(albumId) } }
        }
        else {
            selectedAlbum?.songs = songsList.fastFilter { it.album == selectedAlbum?.album }
        }
    }

//    val otherSongsFromSameArtist = songsList.filter { it.artist == selectedAlbum?.artist }.toMutableList()
//    otherSongsFromSameArtist.removeAll(albumSongs)

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = leftPadding,
            top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()
        )
        .wrapContentHeight()
        .verticalScroll(rememberScrollState())) {
        Box (modifier = Modifier
            .padding(horizontal = 12.dp)
            .height(192.dp)
            .fillMaxWidth()) {
            AsyncImage(
                model = selectedAlbum?.coverArt,
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
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
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
                selectedAlbum?.name?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 32.sp,
                    )
                }
                Row (modifier = Modifier.fillMaxWidth() ,horizontalArrangement = Arrangement.Center) {
                    selectedAlbum?.artist?.let {artistName ->
                        Text(
                            text = "$artistName • ",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clickable {
                                selectedArtist = artistList.firstOrNull { it.name == artistName } ?: return@clickable
                                navHostController.navigate(Screen.AristDetails.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    var albumDuration = 0
                    for (song in albumSongs){
                        albumDuration += song.duration
                    }
                    Text(
                        text = formatMilliseconds(albumDuration),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        textAlign = TextAlign.Center
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
                    SongHelper.currentSong = albumSongs[0]
                    SongHelper.currentList = albumSongs
                    albumSongs[0].media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController)}
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
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

                    val random = albumSongs.indices.random()
                    SongHelper.currentSong = albumSongs[random]
                    SongHelper.currentList = albumSongs
                    albumSongs[random].media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController)}
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
            /*
            SongsHorizontalColumn(albumSongs, onSongSelected = { song ->
                playingSong.selectedSong = song
                SongHelper.currentList = albumSongs
                songState = true })*/
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                for(song in albumSongs){
                    HorizontalSongCard(song = song, onClick = {
                        SongHelper.currentSong = song
                        SongHelper.currentList = albumSongs
                        song.media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController)}
                    })
                }
            }
        }

        /*
        // More songs from Artist
        if (otherSongsFromSameArtist.isNotEmpty()){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.Label_MoreSongsFrom) + " " + selectedAlbum?.artist,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 12.dp)
                )
                SongsRow(songsList = otherSongsFromSameArtist, onSongSelected = { song ->
                    SongHelper.currentSong = song
                    SongHelper.currentList = otherSongsFromSameArtist
                    song.media?.let { SongHelper.playStream(Uri.parse(it), false, mediaController)}
                })
            }
        }
        */

        if(showAddSongToPlaylistDialog.value)
            AddSongToPlaylist(setShowDialog =  { showAddSongToPlaylistDialog.value = it } )

        BottomSpacer()
    }
}