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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.ProviderFeature
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.ui.elements.dialogs.tv.GenericListDialog
import com.craftworks.music.ui.elements.tv.TvArtistCard
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvArtistScreen(
    navHostController: NavHostController = rememberNavController(),
    viewModel: ArtistsScreenViewModel = hiltViewModel(),
) {
    val allArtistList by viewModel.allArtists.collectAsStateWithLifecycle()

    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val sortTranslationBindings = mapOf(
        AlbumArtistListSort.ALBUM_COUNT to R.string.sort_by_album_count,
        AlbumArtistListSort.DURATION to R.string.sort_by_duration,
        AlbumArtistListSort.FAVORITE to R.string.sort_by_favorite,
        AlbumArtistListSort.NAME to R.string.sort_by_name,
        AlbumArtistListSort.PLAY_COUNT to R.string.sort_by_play_count,
        AlbumArtistListSort.RANDOM to R.string.sort_by_random,
        AlbumArtistListSort.RATING to R.string.sort_by_rating,
        AlbumArtistListSort.RECENTLY_ADDED to R.string.sort_by_recently_added,
        AlbumArtistListSort.RELEASE_DATE to R.string.sort_by_release_date,
        AlbumArtistListSort.SONG_COUNT to R.string.sort_by_song_count,
    )
    var showSortDialog by remember { mutableStateOf(false) }
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRequester(focusRequester)
            .focusRestorer(focusRequester),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(span = { GridItemSpan(5) }) {
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

                if (currentProvider?.supportedAlbumArtistSort.orEmpty().size > 1) {
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

                if (currentProvider?.supportArtistSortOrder ?: true) {
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
        items(allArtistList) { artist ->
            TvArtistCard(
                artist = artist,
                modifier = Modifier.onFocusChanged {
                    focusRequester.saveFocusedChild()
                },
                onClick = {
                    focusRequester.saveFocusedChild()
                    navHostController.navigate(Screen.ArtistDetails(artist.id, artist.imageUrl ?: artist.imageId?.let {artist.getProvider()?.getImageUrl(it)} ?: "")) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }

    if (showSortDialog && currentProvider != null)
        GenericListDialog(
            titleRes = R.string.button_sort_by,
            label = { sortTranslationBindings[it]?.let { id -> stringResource(id) } ?: "" },
            setShowDialog = { showSortDialog = it },
            options = currentProvider!!.supportedAlbumArtistSort,
            selectedOption = sort,
            onOptionSelected = { viewModel.setSorting(it) },
            leftAligned = true,
            modifier = Modifier.padding(20.dp).width(280.dp).fillMaxHeight()
        )
}