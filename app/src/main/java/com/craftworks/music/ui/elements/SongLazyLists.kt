package com.craftworks.music.ui.elements

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.albumList
import com.craftworks.music.data.songsList
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.flow.filter

//region Songs
@Composable
fun SongsRow(songsList: List<MediaData.Song>, onSongSelected: (song: MediaData.Song) -> Unit){
    var isSongSelected by remember { mutableStateOf(false) }
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(songsList, key = { it.navidromeID }) {song ->
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
                    putBoolean("isRadio", song.isRadio == true)
                })
                .build()
            val songMediaItem = MediaItem.Builder()
                .setMediaId(song.media.toString())
                .setMediaMetadata(mediaMetadata)
                .setUri(song.media)
                .build()
            //endregion

            SongsCard(
                song = songMediaItem, onClick = {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsHorizontalColumn(
    songList: List<MediaData.Song>,
    onSongSelected: (song: MediaData.Song) -> Unit,
    isSearch: Boolean? = false,
    viewModel: SongsScreenViewModel? = null
){
    var isSongSelected by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val showDividers by SettingsManager(LocalContext.current).showProviderDividersFlow.collectAsStateWithLifecycle(true)

    // Load more songs at scroll
    if (NavidromeManager.checkActiveServers() && isSearch == false){
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
                    if (viewModel == null) return@collect
                    viewModel.getMoreSongs(100)
                }
        }
    }

    LazyColumn(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        state = listState
    ) {
        // Group songs by their source (Local or Navidrome)
        val groupedSongs = songList.groupBy { song ->
            if (song.navidromeID.startsWith("Local_")) "Local" else "Navidrome"
        }

        groupedSongs.forEach { (groupName, songsInGroup) ->
            if (showDividers && groupedSongs.size > 1) {
                stickyHeader {
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
                    Text(
                        text = when (groupName) {
                            "Navidrome" -> stringResource(R.string.Source_Navidrome)
                            "Local" -> stringResource(R.string.Source_Local)
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background) // Add background color for visibility
                            .padding(8.dp)
                    )
                }
            }
            items(songsInGroup) { song ->
                HorizontalSongCard(song = song, onClick = {
                    isSongSelected = true
                    onSongSelected(song)
                })
            }
        }
    }
}
//endregion

//region Albums
@ExperimentalFoundationApi
@Composable
fun AlbumGrid(
    //albums: List<MediaData.Album>,
    mediaController: MediaController?,
    onAlbumSelected: (album: MediaData.Album) -> Unit,
    isSearch: Boolean? = false,
    sort: String? = "alphabeticalByName",
    viewModel: AlbumScreenViewModel = viewModel(),
){
    val gridState = rememberLazyGridState()

    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val showDividers by SettingsManager(LocalContext.current).showProviderDividersFlow.collectAsStateWithLifecycle(true)

    if (NavidromeManager.checkActiveServers() && isSearch == false) {
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
                    viewModel.getMoreAlbums(sort, 100)
                }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .padding(end = 12.dp),
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
fun AlbumRow(
    albums: List<MediaData.Album>,
    mediaController: MediaController?,
    onAlbumSelected: (album: MediaData.Album) -> Unit
){
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 32.dp,
            end = 12.dp
        )
    ) {
        /*
        items(albums) {album ->
            AlbumCard(album = album,
                mediaController = mediaController,
                onClick = {
                    onAlbumSelected(album)
                })
        }
        */

        itemsIndexed(albums) { index, album ->
            // Show divider between local and navidrome albums
            if (SettingsManager(LocalContext.current).showProviderDividersFlow.collectAsStateWithLifecycle(true).value) {
                val dividerIndex = albums.indexOfFirst { it.navidromeID.startsWith("Local_") }
                if (index == dividerIndex && index != albums.lastIndex && index != 0) {
                    Row(
                        modifier = Modifier.padding(12.dp, 12.dp, 0.dp, 0.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        VerticalDivider(
                            modifier = Modifier
                                .height(172.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                        Text(
                            text = stringResource(R.string.Source_Local),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .rotateVertically(),
                        )
                    }
                }
            }

            AlbumCard(
                album = album,
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
                onArtistSelected: (artist: MediaData.Artist) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
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
fun RadiosGrid(radioList: List<MediaData.Radio>, onRadioSelected: (song: MediaData.Song) -> Unit){
    //var isSongSelected by remember { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
    ) {
        items(radioList) {radio ->
            val song = MediaData.Song(
                title = radio.name,
                imageUrl = "android.resource://com.craftworks.music/" + R.drawable.radioplaceholder,
                artist = radio.name,
                media = radio.media,
                duration = 0,
                album = "Internet Radio",
                year = 2024,
                isRadio = true,
                albumId = "", dateAdded = "", format = "MP3", path = "", parent = "", bpm = 0, navidromeID = radio.navidromeID, size = 0)

            RadioCard(
                radio = radio,
                onClick = {
                //isSongSelected = true
                //sliderPos.intValue = 0
                onRadioSelected(song)
                }
            )
        }
    }
}
//endregion

//region Playlists
@ExperimentalFoundationApi
@Composable
fun PlaylistGrid(playlists: List<MediaData.Playlist>, onPlaylistSelected: (playlist: MediaData.Playlist) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
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