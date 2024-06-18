package com.craftworks.music.ui.elements

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Artist
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.PlainLyrics
import com.craftworks.music.data.Playlist
import com.craftworks.music.data.Radio
import com.craftworks.music.data.albumList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.songsList
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.providers.navidrome.albumList
import com.craftworks.music.providers.navidrome.getNavidromeAlbums
import com.craftworks.music.providers.navidrome.sendNavidromeGETRequest
import com.craftworks.music.sliderPos
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

//region Songs
@Composable
fun SongsRow(songsList: List<MediaData.Song>, onSongSelected: (song: MediaData.Song) -> Unit){
    var isSongSelected by remember { mutableStateOf(false) }
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(songsList) {song ->
            //region Make mediaItem from Song
            val mediaMetadata = MediaMetadata.Builder()
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(Uri.parse(song.imageUrl))
                .setReleaseYear(song.year)
                .setExtras(Bundle().apply {
                    putInt("duration", song.duration)
                    putString("MoreInfo", "${song.format} • ${song.bitrate}")
                    putString("NavidromeID", song.navidromeID)
                    putBoolean("isRadio", song.isRadio ?: false)
                })
                .build()
            val songMediaItem = MediaItem.Builder()
                .setMediaId(song.media.toString())
                .setMediaMetadata(mediaMetadata)
                .setUri(song.media)
                .build()
            //endregion

            SongsCard(song = songMediaItem, onClick = {
                isSongSelected = true
                onSongSelected(song)
                //markSongAsPlayed(song)
//                if (navidromeServersList.isEmpty()) return@SongsCard
//                if (navidromeServersList[selectedNavidromeServerIndex.intValue].username == "" ||
//                    navidromeServersList[selectedNavidromeServerIndex.intValue].url == "") return@SongsCard
//                if (useNavidromeServer.value && (navidromeServersList[selectedNavidromeServerIndex.intValue].username != "" || navidromeServersList[selectedNavidromeServerIndex.intValue].url !="" || navidromeServersList[selectedNavidromeServerIndex.intValue].url != "")){
//                    try {
//                        getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
//                    } catch (_: Exception){
//                        // DO NOTHING
//                    }
//                }
            })
        }
    }
}
@Composable
fun SongsHorizontalColumn(songList: List<MediaData.Song>, onSongSelected: (song: MediaData.Song) -> Unit, isSearch: Boolean? = false){
    var isSongSelected by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Load more songs at scroll
    if (useNavidromeServer.value && isSearch == false){
        LaunchedEffect(listState) {

            if (songsList.size % 100 != 0) return@LaunchedEffect

            snapshotFlow {
                val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItemsCount = listState.layoutInfo.totalItemsCount

                lastVisibleItemIndex != null && totalItemsCount > 0 &&
                        (totalItemsCount - lastVisibleItemIndex) <= 10
            }
                .filter { it }
                .collect {
                    coroutineScope.launch {
                        val songOffset = songsList.size
                        songsList.addAll(sendNavidromeGETRequest(
                            navidromeServersList[selectedNavidromeServerIndex.intValue].url,
                            navidromeServersList[selectedNavidromeServerIndex.intValue].username,
                            navidromeServersList[selectedNavidromeServerIndex.intValue].password,
                            "search3.view?query=''&songCount=100&songOffset=$songOffset&artistCount=0&albumCount=0&f=json"
                        ).filterIsInstance<MediaData.Song>())
                    }
                }
        }
    }

    LazyColumn(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = bottomSpacerHeightDp()
        ),
        state = listState
    ) {
        items(songList) { song ->
            HorizontalSongCard(song = song, onClick = {
                isSongSelected = true
                sliderPos.intValue = 0
                onSongSelected(song)
            })
        }
    }
}
//endregion

//region Albums
@ExperimentalFoundationApi
@Composable
fun AlbumGrid(
    albums: List<MediaData.Album>,
    mediaController: MediaController?,
    onAlbumSelected: (album: MediaData.Album) -> Unit,
    isSearch: Boolean? = false,
    sort: String? = "alphabeticalByName"){

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    if (useNavidromeServer.value && isSearch == false) {
        LaunchedEffect(gridState) {
            if (albumList.size % 100 != 0) return@LaunchedEffect

            snapshotFlow {
                val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItemsCount = gridState.layoutInfo.totalItemsCount

                lastVisibleItemIndex != null && totalItemsCount > 0 &&
                        (totalItemsCount - lastVisibleItemIndex) <= 10
            }
                .filter { it }
                .collect {
                    coroutineScope.launch {
                        val albumOffset = albumList.size
                        albumList.addAll(getNavidromeAlbums(sort, 100, albumOffset))
                    }
                }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .padding(end = 12.dp),
        contentPadding = PaddingValues(
            bottom = bottomSpacerHeightDp()
        ),
        state = gridState
    ) {
        items(albums) {album ->
            AlbumCard(album = album,
                mediaController = mediaController,
                onClick = {
                    onAlbumSelected(album)
                })
        }
    }
}
@ExperimentalFoundationApi
@Composable
fun AlbumRow(albums: List<MediaData.Album>, mediaController: MediaController?, onAlbumSelected: (album: MediaData.Album) -> Unit){
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            //start = 6.dp,end = 6.dp,
            top = 32.dp,
            end = 12.dp
        )
    ) {
        items(albums) {album ->
            AlbumCard(album = album,
                mediaController = mediaController,
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
fun ArtistsGrid(artists: List<MediaData.Artist>,
                navHostController: NavHostController = rememberNavController(),
                onArtistSelected: (artist: MediaData.Artist) -> Unit){
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
fun RadiosGrid(radioList: List<Radio>, onSongSelected: (song: MediaData.Song) -> Unit){
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
            val song = MediaData.Song(
                title = radio.name,
                imageUrl = "android.resource://com.craftworks.music/" + R.drawable.radioplaceholder,
                artist = radio.name,
                media = radio.media.toString(),
                duration = 0,
                album = "Internet Radio",
                year = 2024,
                isRadio = true,
                albumId = "", dateAdded = "", format = "MP3", path = "", parent = "", bpm = 0, navidromeID = "Radio", size = 0)

            RadioCard(
                radio = radio,
                onClick = {
                isSongSelected = true
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