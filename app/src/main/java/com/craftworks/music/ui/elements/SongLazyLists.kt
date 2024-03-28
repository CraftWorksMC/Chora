package com.craftworks.music.ui.elements

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.SongHelper
import com.craftworks.music.data.Album
import com.craftworks.music.data.Artist
import com.craftworks.music.data.PlainLyrics
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.Radio
import com.craftworks.music.data.Song
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.providers.navidrome.selectedNavidromeServerIndex
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.sliderPos
import java.net.URL

//region Songs
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
                //markSongAsPlayed(song)
                if (navidromeServersList.isEmpty()) return@SongsCard
                if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
                    navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return@SongsCard
                if (useNavidromeServer.value && (navidromeServersList[selectedNavidromeServerIndex.intValue].username != "" || navidromeServersList[selectedNavidromeServerIndex.intValue].url !="" || navidromeServersList[selectedNavidromeServerIndex.intValue].url != "")){
                    try {
                        getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
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
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = bottomSpacerHeightDp()
        )
    ) {
        items(songsList) {song ->
            HorizontalSongCard(song = song, onClick = {
                isSongSelected = true
                SongHelper.stopStream()
                sliderPos.intValue = 0
                onSongSelected(song)
                //markSongAsPlayed(song)
                if (navidromeServersList.isEmpty()) return@HorizontalSongCard
                if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
                    navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return@HorizontalSongCard
                if (useNavidromeServer.value && (navidromeServersList[selectedNavidromeServerIndex.intValue].username != "" || navidromeServersList[selectedNavidromeServerIndex.intValue].url !="" || navidromeServersList[selectedNavidromeServerIndex.intValue].url != "")){
                    try {
                        getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
                    } catch (_: Exception){
                        // DO NOTHING
                    }
                }
            })
        }
    }
}
//endregion

//region Albums
@ExperimentalFoundationApi
@Composable
fun AlbumGrid(albums: List<Album>, onAlbumSelected: (album: Album) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        contentPadding = PaddingValues(
            bottom = bottomSpacerHeightDp()
        )
    ) {
        items(albums) {album ->
            AlbumCard(album = album,
                onClick = {
                    onAlbumSelected(album)
                })
        }
    }
}
@ExperimentalFoundationApi
@Composable
fun AlbumRow(albums: List<Album>, onAlbumSelected: (album: Album) -> Unit){
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 6.dp,end = 6.dp,
            top = 32.dp
        )
    ) {
        items(albums) {album ->
            AlbumCard(album = album,
                onClick = {
                    onAlbumSelected(album)
                })
        }
    }
}
//endregion

//region Artists
@ExperimentalFoundationApi
@Composable
fun ArtistsGrid(artists: List<Artist>,
                navHostController: NavHostController = rememberNavController(),
                onArtistSelected: (artist: Artist) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        contentPadding = PaddingValues(
            bottom = bottomSpacerHeightDp()
        )
    ) {
        items(artists) {artist ->
            ArtistCard(artist = artist, onClick = {
                    onArtistSelected(artist)
                })
        }
    }
}
//endregion

//region Radios
@ExperimentalFoundationApi
@Composable
fun RadiosGrid(radioList: List<Radio>, onSongSelected: (song: Song) -> Unit){
    var isSongSelected by remember { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        contentPadding = PaddingValues(
            bottom = bottomSpacerHeightDp()
        )
    ) {
        items(radioList) {radio ->
            val song = Song(
                title = radio.name,
                imageUrl = Uri.parse("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder),
                artist = radio.name,
                media = radio.media,
                duration = 0,
                album = "Internet Radio",
                year = "2024",
                isRadio = true)

            RadioCard(
                radio = radio,
                onClick = {
                isSongSelected = true
                SongHelper.stopStream()
                sliderPos.intValue = 0
                onSongSelected(song)
                PlainLyrics = "No Lyrics For Internet Radio"
            })
        }
    }
}
//endregion

//region Playlists
@ExperimentalFoundationApi
@Composable
fun PlaylistGrid(playlists: List<Playlist>, onPlaylistSelected: (playlist: Playlist) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = bottomSpacerHeightDp())
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
//endregion