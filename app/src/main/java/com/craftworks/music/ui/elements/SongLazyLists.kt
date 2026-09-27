package com.craftworks.music.ui.elements

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.id
import com.craftworks.music.data.model.providerId
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.viewmodels.AlbumScreenViewModel
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

//region Songs
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsHorizontalColumn(
    songList: List<MediaItem>,
    isSearch: Boolean? = false,
    showFavoritesOnly: Boolean = false,
    viewModel: SongsScreenViewModel? = null,
    mediaController: MediaController? = null,
){
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    // Load more songs at scroll
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItemsCount = listState.layoutInfo.totalItemsCount

            lastVisibleItemIndex != null && totalItemsCount > 0 &&
                    (totalItemsCount - lastVisibleItemIndex) <= 25
        }
            .filter { it }
            .collect {
                if (viewModel == null) return@collect
                viewModel.getMoreSongs(100)
            }
    }

    LazyColumn(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {

        itemsIndexed(songList) { index, song ->
            HorizontalSongCard(
                song = song,
                onClick = {
                    println("Starting song at index: $index")
                    scope.launch {
                        SongHelper.play(songList, index, mediaController)
                    }
                },
                mediaController = mediaController
            )
        }
    }
}
//endregion

//region Albums
@ExperimentalFoundationApi
@Composable
fun AlbumGrid(
    albums: List<MediaItem>,
    mediaController: MediaController?,
    onAlbumSelected: (album: MediaItem) -> Unit,
    isSearch: Boolean? = false,
    viewModel: AlbumScreenViewModel = viewModel(),
){
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Group songs by their source (Local or Navidrome)
    val groupedAlbums = albums.groupBy {
        it.mediaMetadata.providerId
    }

    LaunchedEffect(gridState) {
        if (albums.size % 50 != 0) return@LaunchedEffect

        snapshotFlow {
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItemsCount = gridState.layoutInfo.totalItemsCount

            lastVisibleItemIndex != null && totalItemsCount > 0 &&
                    (totalItemsCount - lastVisibleItemIndex) <= 10
        }
            .filter { it }
            .collect {
                viewModel.getMoreAlbums(50)
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(
            items = albums,
            key = { it.mediaId }
        ) { album ->
            AlbumCard(album = album,
                onClick = {
                    onAlbumSelected(album)
                },
                onPlay = {
                    coroutineScope.launch {
                        val mediaItems = viewModel.getAlbum(album.mediaMetadata.id ?: "")
                        if (mediaItems.isNotEmpty())
                            SongHelper.play(
                                mediaItems = mediaItems.subList(1, mediaItems.size),
                                index = 0,
                                mediaController = mediaController
                            )
                    }
                }
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun AlbumGrid(
    albums: List<MediaItem>,
    mediaController: MediaController?,
    onAlbumSelected: (album: MediaItem) -> Unit,
    onGetAlbum: (albumID: String) -> List<MediaItem>
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(
            items = albums,
            key = { it.mediaId }
        ) { album ->
            AlbumCard(album = album,
                onClick = {
                    onAlbumSelected(album)
                },
                onPlay = {
                    coroutineScope.launch {
                        val mediaItems = onGetAlbum(album.mediaMetadata.id ?: "")
                        if (mediaItems.isNotEmpty())
                            SongHelper.play(
                                mediaItems = mediaItems.subList(1, mediaItems.size),
                                index = 0,
                                mediaController = mediaController
                            )
                    }
                }
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun AlbumRow(
    albums: List<MediaItem>,
    onAlbumSelected: (album: MediaItem) -> Unit,
    onPlay: (album: MediaItem) -> Unit,
){
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .heightIn(min = 172.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = albums,
            key = { _, album -> album.mediaId }
        ) { index, album ->
            AlbumCard(
                album = album,
                onClick = {
                    onAlbumSelected(album)
                },
                onPlay = {
                    onPlay(album)
                },
                modifier = Modifier.animateItem()
            )
        }
    }
}
//endregion

//region Artists
@ExperimentalFoundationApi
@Composable
fun ArtistsGrid(
    artists: List<MediaModel.Artist>,
    onArtistSelected: (artist: MediaModel.Artist) -> Unit,
    onGetMoreArtists: () -> Unit
){
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItemsCount = gridState.layoutInfo.totalItemsCount

            lastVisibleItemIndex != null && totalItemsCount > 0 &&
                    (totalItemsCount - lastVisibleItemIndex) <= 10
        }
            .filter { it }
            .collect { onGetMoreArtists() }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(
            items = artists,
            key = { it.id }
        ) { artist ->
            ArtistCard(artist = artist, onClick = {
                onArtistSelected(artist)
            })
        }
    }
}
//endregion

//region Playlists
@ExperimentalFoundationApi
@Composable
fun PlaylistGrid(playlists: List<MediaItem>, onPlaylistSelected: (playlist: MediaItem) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(12.dp)
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