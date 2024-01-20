package com.craftworks.music.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.SongHelper
import com.craftworks.music.data.Album
import com.craftworks.music.data.Screen
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.lyrics.SyncedLyric
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.lyrics.songLyrics
import com.craftworks.music.navidrome.getNavidromeSongs
import com.craftworks.music.navidrome.markSongAsPlayed
import com.craftworks.music.navidrome.navidromePassword
import com.craftworks.music.navidrome.navidromeServerIP
import com.craftworks.music.navidrome.navidromeUsername
import com.craftworks.music.playingSong
import com.craftworks.music.shuffleSongs
import com.craftworks.music.sliderPos
import com.craftworks.music.songState
import com.craftworks.music.songsList
import com.craftworks.music.ui.elements.HorizontalSongCard
import com.craftworks.music.ui.elements.SongsRow
import java.net.URL

var albumList:MutableList<Album> = mutableStateListOf()
var selectedAlbum by mutableStateOf<Album?>(
    Album(
        name = "My Album",
        artist = "My Favourite Artist",
        year = "2023",
        Uri.EMPTY)
)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AlbumDetails(navHostController: NavHostController = rememberNavController()) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red, Color.Transparent))

    val albumSongs = songsList.filter { it.album == selectedAlbum?.name }

    val otherSongsFromSameArtist = songsList.filter { it.artist == selectedAlbum?.artist }.toMutableList()
    otherSongsFromSameArtist.removeAll(albumSongs)

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding)
        .verticalScroll(rememberScrollState())
        .wrapContentHeight()) {
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
                    .alpha(0.75f)
            )
            Button(
                onClick = { navHostController.navigate(Screen.Albums.route) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(top = 12.dp, start = 12.dp)
                    .size(32.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Settings",
                    modifier = Modifier
                        .height(32.dp)
                        .size(32.dp)
                )
            }
            selectedAlbum?.name?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    lineHeight = 32.sp
                )
            }
        }
        // Album name, artist and duration
            Row (modifier = Modifier.fillMaxWidth() ,horizontalArrangement = Arrangement.Center) {
                selectedAlbum?.artist?.let {
                    Text(
                        text = "$it • ",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        textAlign = TextAlign.Center
                    )
                }
                var albumDuration: Int = 0
                for (song in albumSongs){
                    albumDuration += song.duration
                }
                Text(
                    text = formatMilliseconds(albumDuration.toFloat()),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Normal,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    textAlign = TextAlign.Center
                )
            }

        // Play and shuffle buttons
        Row (modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    playingSong.selectedSong = albumSongs[0]
                    playingSong.selectedList = albumSongs
                    songState = true
                }
            ) {
                Text("Play All")
            }
            Button(
                onClick = {
                    shuffleSongs.value = true
                    playingSong.selectedSong = albumSongs[albumSongs.indices.random()]
                    playingSong.selectedList = albumSongs
                    songState = true
                }
            ) {
                Text("Shuffle")
            }
        }

        // Songs List
        Column(modifier = Modifier
            .padding(start = 12.dp,end = 12.dp, top = 0.dp)
            .wrapContentHeight()
        ) {
            /*
            SongsHorizontalColumn(albumSongs, onSongSelected = { song ->
                playingSong.selectedSong = song
                playingSong.selectedList = albumSongs
                songState = true })*/
            Column(
                modifier = Modifier.wrapContentHeight().fillMaxWidth()
            ) {
                for(song in albumSongs){
                    HorizontalSongCard(song = song, onClick = {
                        SongHelper.stopStream()
                        sliderPos.intValue = 0
                        playingSong.selectedSong = song
                        playingSong.selectedList = albumSongs
                        songState = true
                        songLyrics.SongLyrics = "Getting Lyrics... \n No Lyrics Found"
                        SyncedLyric.clear()
                        getLyrics()
                        markSongAsPlayed(song)
                        if (useNavidromeServer.value && (navidromeUsername.value != "" || navidromePassword.value !="" || navidromeServerIP.value != "")){
                            try {
                                getNavidromeSongs(URL("${navidromeServerIP.value}/rest/search3.view?query=''&songCount=10000&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora"))
                            } catch (_: Exception){
                                // DO NOTHING
                            }
                        }
                    })
                }
            }
        }

        // More songs from Artist
        if (otherSongsFromSameArtist.isNotEmpty()){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            ) {
                Text(
                    text = "More Songs From " + selectedAlbum?.artist,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 12.dp)
                )
                SongsRow(songsList = otherSongsFromSameArtist, onSongSelected = { song ->
                    playingSong.selectedSong = song
                    playingSong.selectedList = otherSongsFromSameArtist
                    songState = true
                })
            }
        }
    }
}