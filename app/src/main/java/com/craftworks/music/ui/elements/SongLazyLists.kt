package com.craftworks.music.ui.elements

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.craftworks.music.SongHelper
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.Radio
import com.craftworks.music.data.Song
import com.craftworks.music.lyrics.SyncedLyric
import com.craftworks.music.lyrics.getLyrics
import com.craftworks.music.lyrics.songLyrics.SongLyrics
import com.craftworks.music.navidrome.getNavidromeSongs
import com.craftworks.music.navidrome.markSongAsPlayed
import com.craftworks.music.navidrome.navidromePassword
import com.craftworks.music.navidrome.navidromeServerIP
import com.craftworks.music.navidrome.navidromeUsername
import com.craftworks.music.sliderPos
import com.craftworks.music.ui.screens.useNavidromeServer
import java.net.URL

@Composable
fun SongsRow(songsList: List<Song>, onSongSelected: (song: Song) -> Unit){
    var isSongSelected by remember { mutableStateOf(false) }
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(songsList) {song ->
            SongsCard(song = song, onClick = {
                isSongSelected = true
                SongHelper.stopStream()
                sliderPos.intValue = 0
                onSongSelected(song)
                SongLyrics = "Getting Lyrics... / No Lyrics Found"
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

@Composable
fun SongsHorizontalColumn(songsList: List<Song>, onSongSelected: (song: Song) -> Unit){
    var isSongSelected by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp)
    ) {
        items(songsList) {song ->
            HorizontalSongCard(song = song, onClick = {
                isSongSelected = true
                SongHelper.stopStream()
                sliderPos.intValue = 0
                onSongSelected(song)
                SongLyrics = "Getting Lyrics... / No Lyrics Found"
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

@ExperimentalFoundationApi
@Composable
fun RadiosGrid(radioList: List<Radio>, onSongSelected: (song: Song) -> Unit){
    var isSongSelected by remember { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.FixedSize(128.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(radioList) {radio ->
            val song = Song(
                imageUrl = radio.imageUrl,
                title = radio.name,
                media = radio.media,
                artist = "Internet Radio",
                duration = 0,
                album = "Internet Radio",
                year = "2023",
                isRadio = true)

            SongsCard(
                song = song,
                onClick = {
                isSongSelected = true
                SongHelper.stopStream()
                sliderPos.intValue = 0
                onSongSelected(song)
                SongLyrics = "No Lyrics For Internet Radio"
            })
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun PlaylistGrid(playlists: List<Playlist>, onPlaylistSelected: (playlist: Playlist) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.FixedSize(152.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(playlists) {playlist ->
            PlaylistCard(playlist = playlist,
                onClick = {
                    onPlaylistSelected(playlist)
                    Log.d("PLAYLISTS", "CLICKED PLAYLIST!")
                })
        }
    }
}