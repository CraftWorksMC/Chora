package com.craftworks.music.ui.elements

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.data.model.MediaData
import com.craftworks.music.data.model.albumList
import com.craftworks.music.data.model.songsList
import com.craftworks.music.data.model.toAlbum
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
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
    onSongSelected: (itemsList: List<MediaItem>, index: Int) -> Unit,
    isSearch: Boolean? = false,
    viewModel: SongsScreenViewModel? = null
){
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
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Group songs by their source (Local or Navidrome)
        val groupedSongs = songList.groupBy { song ->
            if (song.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_")) "Local" else "Navidrome"
        }

        groupedSongs.forEach { (groupName, songsInGroup) ->
            if (showDividers && groupedSongs.size > 1) {
                item {
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth(),
                            //.background(MaterialTheme.colorScheme.background),
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
                            //.background(MaterialTheme.colorScheme.background)
                            .padding(8.dp)
                    )
                }
            }
            itemsIndexed(songsInGroup) { index, song ->
                HorizontalSongCard(
                    song = song,
                    onClick = {
                        onSongSelected(songsInGroup, index)
                    }
                )
            }
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
    onAlbumSelected: (album: MediaData.Album) -> Unit,
    isSearch: Boolean? = false,
    sort: String? = "alphabeticalByName",
    viewModel: AlbumScreenViewModel = viewModel(),
){
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val showDividers by SettingsManager(LocalContext.current).showProviderDividersFlow.collectAsStateWithLifecycle(true)

    // Group songs by their source (Local or Navidrome)
    val groupedAlbums = albums.groupBy { song ->
        if (song.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_")) "Local" else "Navidrome"
    }

    if (NavidromeManager.checkActiveServers() && isSearch == false) {
        LaunchedEffect(gridState) {
            if (albumList.size % 50 != 0) return@LaunchedEffect

            snapshotFlow {
                val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val totalItemsCount = gridState.layoutInfo.totalItemsCount

                lastVisibleItemIndex != null && totalItemsCount > 0 &&
                        (totalItemsCount - lastVisibleItemIndex) <= 10
            }
                .filter { it }
                .collect {
                    viewModel.getMoreAlbums(sort, 50)
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
        if (showDividers && groupedAlbums.size > 1) {
            groupedAlbums.forEach { (groupName, albumsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column (Modifier.padding(start = 12.dp)) {
                        HorizontalDivider(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth(),
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
                                .padding(8.dp)
                        )
                    }

                }
                itemsIndexed(albumsInGroup) { index, album ->
                    AlbumCard(album = album,
                        mediaController = mediaController,
                        onClick = {
                            onAlbumSelected(album.toAlbum())
                        },
                        onPlay = {
                            coroutineScope.launch {
                                val mediaItems = viewModel.getAlbum(album.mediaMetadata.extras?.getString("navidromeID") ?: "")
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
        else {
            items(
                items = albums,
                key = { it.mediaId }
            ) { album ->
                AlbumCard(album = album,
                    mediaController = mediaController,
                    onClick = {
                        onAlbumSelected(album.toAlbum())
                    },
                    onPlay = {
                        coroutineScope.launch {
                            val mediaItems = viewModel.getAlbum(album.mediaMetadata.extras?.getString("navidromeID") ?: "")
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
}

@ExperimentalFoundationApi
@Composable
fun AlbumGrid(
    albums: List<MediaItem>,
    mediaController: MediaController?,
    onAlbumSelected: (album: MediaData.Album) -> Unit,
    onGetAlbum: (albumID: String) -> List<MediaItem>
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val showDividers by SettingsManager(LocalContext.current).showProviderDividersFlow.collectAsStateWithLifecycle(true)

    // Group songs by their source (Local or Navidrome)
    val groupedAlbums = albums.groupBy { song ->
        if (song.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_")) "Local" else "Navidrome"
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .padding(end = 12.dp),
        state = gridState
    ) {
        if (showDividers && groupedAlbums.size > 1) {
            groupedAlbums.forEach { (groupName, albumsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column (Modifier.padding(start = 12.dp)) {
                        HorizontalDivider(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth(),
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
                                .padding(8.dp)
                        )
                    }

                }
                itemsIndexed(albumsInGroup) { index, album ->
                    AlbumCard(album = album,
                        mediaController = mediaController,
                        onClick = {
                            onAlbumSelected(album.toAlbum())
                        },
                        onPlay = {
                            coroutineScope.launch {
                                val mediaItems = onGetAlbum(album.mediaMetadata.extras?.getString("navidromeID") ?: "")
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
        else {
            items(
                items = albums,
                key = { it.mediaId }
            ) { album ->
                AlbumCard(album = album,
                    mediaController = mediaController,
                    onClick = {
                        onAlbumSelected(album.toAlbum())
                    },
                    onPlay = {
                        coroutineScope.launch {
                            val mediaItems = onGetAlbum(album.mediaMetadata.extras?.getString("navidromeID") ?: "")
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
}

@ExperimentalFoundationApi
@Composable
fun AlbumRow(
    albums: List<MediaItem>,
    mediaController: MediaController?,
    onAlbumSelected: (album: MediaData.Album) -> Unit,
    onPlay: (album: MediaItem) -> Unit,
){
    val showProviderDividers by SettingsManager(LocalContext.current).showProviderDividersFlow.collectAsStateWithLifecycle(true)
    val dividerIndex = albums.indexOfFirst { it.mediaMetadata.extras?.getString("navidromeID")!!.startsWith("Local_") }

    LazyRow(
        modifier = Modifier.fillMaxSize().heightIn(min = 172.dp),
        contentPadding = PaddingValues(
            end = 12.dp
        )
    ) {
        itemsIndexed(
            items = albums,
            key = { _, album -> album.mediaId }
        ) { index, album ->
            // Show divider between local and navidrome albums
            if (showProviderDividers) {
                if (index == dividerIndex && index != albums.lastIndex && index != 0) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp),
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
                    onAlbumSelected(album.toAlbum())
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
    artists: List<MediaData.Artist>,
    onArtistSelected: (artist: MediaData.Artist) -> Unit
){
    val gridState = rememberLazyGridState()
    val showProviderDividers by SettingsManager(LocalContext.current).showProviderDividersFlow.collectAsStateWithLifecycle(true)

    val groupedArtists = artists.groupBy { artist ->
        if (artist.navidromeID.startsWith("Local_")) "Local" else "Navidrome"
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        state = gridState
    ) {
        if (showProviderDividers && groupedArtists.size > 1) {
            groupedArtists.forEach { (groupName, artistsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(start = 12.dp)) {
                        HorizontalDivider(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth(),
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
                                .padding(8.dp)
                        )
                    }

                }
                itemsIndexed(artistsInGroup) { index, artist ->
                    ArtistCard(artist = artist, onClick = {
                        onArtistSelected(artist)
                    })
                }
            }
        } else {
            items(
                items = artists,
                key = { it.navidromeID }
            ) { artist ->
                ArtistCard(artist = artist, onClick = {
                    onArtistSelected(artist)
                })
            }
        }
    }
}
//endregion

//region Playlists
@ExperimentalFoundationApi
@Composable
fun PlaylistGrid(playlists: List<MediaItem>, onPlaylistSelected: (playlist: MediaItem) -> Unit){
    LazyVerticalGrid(
        columns = GridCells.Adaptive(128.dp),
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
    ) {
        item {
            val favouritesPlaylist = MediaItem.Builder()
                .setMediaId("favourites")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Favourites")
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setArtworkUri(("android.resource://com.craftworks.music/" + R.drawable.favourites).toUri())
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .setExtras(Bundle().apply {
                            putString("navidromeID", "favourites")
                        })
                        .build()
                )
                .build()

            PlaylistCard(playlist = favouritesPlaylist,
                onClick = {
                    onPlaylistSelected(favouritesPlaylist)
                }
            )
        }
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