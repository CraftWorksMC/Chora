package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.id
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.GenericListDialog
import com.craftworks.music.ui.elements.dialogs.tv.SongDialog
import com.craftworks.music.ui.elements.tv.TvHorizontalSongCard
import com.craftworks.music.ui.viewmodels.SongsScreenViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSongsScreen(
    mediaController: MediaController? = null,
    navHostController: NavController,
    viewModel: SongsScreenViewModel = hiltViewModel(),
) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()

    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()

    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val sortTranslationBindings = mapOf(
        SongListSort.ALBUM to R.string.sort_by_album,
        SongListSort.ALBUM_ARTIST to R.string.sort_by_album_artist,
        SongListSort.ARTIST to R.string.sort_by_artist,
        SongListSort.BPM to R.string.sort_by_bpm,
        SongListSort.CHANNELS to R.string.sort_by_channels,
        SongListSort.COMMENT to R.string.sort_by_comment,
        SongListSort.DURATION to R.string.sort_by_duration,
        SongListSort.EXPLICIT_STATUS to R.string.sort_by_explicit_status,
        SongListSort.FAVORITE to R.string.sort_by_favorite,
        SongListSort.GENRE to R.string.sort_by_genre,
        SongListSort.ID to R.string.sort_by_id,
        SongListSort.NAME to R.string.sort_by_name,
        SongListSort.PLAY_COUNT to R.string.sort_by_play_count,
        SongListSort.RANDOM to R.string.sort_by_random,
        SongListSort.RATING to R.string.sort_by_rating,
        SongListSort.RECENTLY_ADDED to R.string.sort_by_recently_added,
        SongListSort.RECENTLY_PLAYED to R.string.sort_by_recently_played,
        SongListSort.RELEASE_DATE to R.string.sort_by_release_date,
        SongListSort.YEAR to R.string.sort_by_year,
    )
    var showSortDialog by remember { mutableStateOf(false) }

    var selectedSong by remember { mutableStateOf(MediaItem.EMPTY) }
    var showSongDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

        LaunchedEffect(songs.size) {
            if (songs.size % 50 != 0) return@LaunchedEffect
            if (songs.size < 50) return@LaunchedEffect

            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?: return@snapshotFlow false
                val total = gridState.layoutInfo.totalItemsCount
                if (total < songs.size - 5) return@snapshotFlow false
                (total - lastVisible) <= 15
            }.filter { it }.collect {
                viewModel.getMoreSongs(50)
            }
        }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRequester(focusRequester)
            .focusRestorer(focusRequester),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentProvider?.featureFlags?.contains(ProviderFeature.FAVORITES) ?: false) {
                    IconButton(
                        onClick = {
                            viewModel.setShowFavoritesOnly(!showFavoritesOnly)
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (showFavoritesOnly) R.drawable.round_favorite_24 else R.drawable.round_favorite_border_24),
                            contentDescription = stringResource(R.string.button_toggle_favorites),
                        )
                    }
                }

                if (currentProvider?.supportedSongSort.orEmpty().size > 1) {
                    Button(
                        onClick = { showSortDialog = true },
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.rounded_sort_24),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )

                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))

                        Text(
                            text = sortTranslationBindings[sort]?.let { id -> stringResource(id) }
                                ?: ""
                        )
                    }
                }

                if (currentProvider?.supportSongSortOrder ?: true) {
                    IconButton(
                        onClick = {
                            viewModel.setOrder(sortOrder.invert())
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (sortOrder == SortOrder.ASC) R.drawable.arrow_upward_24px else R.drawable.arrow_downward_24px),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }
            }
        }

        itemsIndexed(songs) { index, song ->
            TvHorizontalSongCard(
                song = song,
                modifier = Modifier.onFocusChanged {
                    focusRequester.saveFocusedChild()
                },
                onClick = {
                    coroutineScope.launch {
                        SongHelper.play(songs, index, mediaController)
                        navHostController.navigate(Screen.NowPlayingLandscape) {
                            launchSingleTop = true
                        }
                    }
                },
                onLongClick = {
                    selectedSong = song
                    showSongDialog = true
                }
            )
        }
    }

    if (showSongDialog)
        SongDialog(
            song = selectedSong,
            onSetRating = { rating ->
                viewModel.setSongRating(
                    songId = selectedSong.mediaMetadata.id ?: "",
                    rating = rating
                )
            },
            onDownload = {
                viewModel.downloadSong(it)
            },
            setShowDialog = { showSongDialog = it }
        )

    if (showSortDialog && currentProvider != null)
        GenericListDialog(
            titleRes = R.string.button_sort_by,
            label = { sortTranslationBindings[it]?.let { id -> stringResource(id) } ?: "" },
            setShowDialog = { showSortDialog = it },
            options = currentProvider!!.supportedSongSort,
            selectedOption = sort,
            onOptionSelected = { viewModel.setSorting(it) },
            leftAligned = true,
            modifier = Modifier.padding(20.dp).width(280.dp).fillMaxHeight()
        )
}