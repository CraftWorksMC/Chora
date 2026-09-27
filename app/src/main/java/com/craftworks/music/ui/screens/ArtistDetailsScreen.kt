package com.craftworks.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.data.model.id
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.ActionButtonType
import com.craftworks.music.ui.elements.AlbumCard
import com.craftworks.music.ui.elements.SongListActionButtons
import com.craftworks.music.ui.elements.dialogs.AddToPlaylist
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.viewmodels.ArtistsScreenViewModel
import com.craftworks.music.utils.bleedHorizontal
import com.craftworks.music.utils.fadingEdge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun ArtistDetails(
    selectedArtistId: String,
    selectedArtistImage: String? = null,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: ArtistsScreenViewModel = hiltViewModel()
) {
    var showLoading by remember { mutableStateOf(false) }

    val artist = viewModel.selectedArtist.collectAsStateWithLifecycle().value
    val artistAlbums = viewModel.artistAlbums.collectAsStateWithLifecycle().value
    val actionButtons = viewModel.actionButtons.collectAsStateWithLifecycle(emptyList()).value
    val context = LocalContext.current
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red.copy(0.75f), Color.Transparent))

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showArtistBiographyDialog by remember { mutableStateOf(false) }
    var addToPlaylistSongs by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    val getArtistSongs : suspend () -> List<MediaItem> = {
        artistAlbums.flatMap {
            it.mediaMetadata.id.let { id ->
                val album = viewModel.getAlbum(id ?: "")
                if (album.isNotEmpty())
                    album.subList(1, album.size)
                else
                    emptyList()
            }
        }
    }

    LaunchedEffect(selectedArtistId) {
        showLoading = false

        viewModel.loadArtistDetails(selectedArtistId)

        delay(500.milliseconds)
        showLoading = true
    }

    // Loading spinner
    AnimatedVisibility(
        visible = artist?.name?.isBlank() == true && showLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Text(
                text = "Loading",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    // Main Content
    AnimatedVisibility(
        visible = artist?.name?.isNotBlank() == true,
        enter = fadeIn()
    ) {
        var isStarred by remember { mutableStateOf(artist?.userFavorite ?: false) }

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .dialogFocusable(),
            columns = GridCells.Adaptive(96.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 12.dp),
        ) {
            // Group songs by their source (Local or Navidrome)
            val groupedAlbums =
                artistAlbums.groupBy { it.mediaMetadata.recordingYear }
                    .toSortedMap(compareByDescending { it })

            // Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .height(320.dp)
                        .fillMaxWidth()
                ) {
                    //Image and Name
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artist?.imageUrl)
                            .diskCacheKey(selectedArtistId)
                            .diskCachePolicy(CachePolicy.READ_ONLY)
                            .placeholderMemoryCacheKey(selectedArtistId)
                            .crossfade(true)
                            .build(),
                        contentScale = ContentScale.FillWidth,
                        contentDescription = "Artist Image",
                        modifier = Modifier
                            .bleedHorizontal(12.dp)
                            .fadingEdge(imageFadingEdge)
                            .blur(12.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = WindowInsets.safeDrawing.asPaddingValues()
                                    .calculateTopPadding(),
                                bottom = 12.dp
                            )
                            .padding(horizontal = 12.dp)
                    ) {
                        // Back button
                        FilledTonalIconButton(
                            onClick = { navHostController.popBackStack() },
                            modifier = Modifier
                                .size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "back"
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Album Name and Artist
                            Text(
                                text = artist?.name.toString(),
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.headlineMediumEmphasized,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Description
                            artist?.biography?.let { biography ->
                                Text(
                                    text = biography,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Light,
                                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                    textAlign = TextAlign.Start,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showArtistBiographyDialog = true
                                        }
                                        .padding(6.dp)
                                )
                            }

                            // Play, shuffle and more buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                            ) {
                                val actions: Map<ActionButtonType, () -> Unit> = mapOf(
                                    ActionButtonType.SHUFFLE to {
                                        coroutineScope.launch {
                                            val allArtistSongsList = getArtistSongs()

                                            mediaController?.shuffleModeEnabled = true
                                            val random = allArtistSongsList.indices.random()
                                            SongHelper.play(
                                                allArtistSongsList,
                                                random,
                                                mediaController
                                            )
                                        }
                                    },
                                    ActionButtonType.FAVORITE to {
                                        coroutineScope.launch {
                                            artist?.id?.let {
                                                if (isStarred)
                                                    viewModel.unstarArtist(it)
                                                else
                                                    viewModel.starArtist(it)
                                            }
                                            if (selectedArtistId != null) viewModel.loadArtistDetails(
                                                selectedArtistId
                                            )
                                            isStarred = !isStarred
                                        }
                                    },
                                    ActionButtonType.ADD_TO_QUEUE to {
                                        coroutineScope.launch {
                                            SongHelper.enqueue(getArtistSongs(), mediaController)
                                        }
                                    },
                                    ActionButtonType.PLAY_NEXT to {
                                        coroutineScope.launch {
                                            SongHelper.playNext(getArtistSongs(), mediaController)
                                        }
                                    },
                                    ActionButtonType.ADD_TO_PLAYLIST to {
                                        coroutineScope.launch {
                                            addToPlaylistSongs = getArtistSongs()
                                            showAddToPlaylistDialog = true
                                        }
                                    },
                                    ActionButtonType.DOWNLOAD to {
                                        coroutineScope.launch {
                                            viewModel.downloadArtist(getArtistSongs())
                                        }
                                    },
                                )

                                SongListActionButtons(
                                    buttons = actionButtons.map {
                                        it.apply {
                                            this.onClick = actions[this.type] ?: {}
                                        }
                                    },
                                    providerFeatures = artist?.getProvider()?.featureFlags,
                                    playAction = {
                                        coroutineScope.launch {
                                            SongHelper.play(
                                                getArtistSongs(),
                                                0,
                                                mediaController
                                            )
                                        }
                                    },
                                    isStarred = isStarred
                                )
                            }
                        }
                    }
                }
            }

            /* Discography header */
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.artist_details_discography),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            groupedAlbums.forEach { (groupName, albumsInGroup) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = groupName.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = 12.dp)
                    )
                }
                itemsIndexed(albumsInGroup) { index, album ->
                    AlbumCard(
                        album = album,
                        onClick = {
                            navHostController.navigate(
                                Screen.AlbumDetails(
                                    album.mediaMetadata.id ?: "",
                                    album.mediaMetadata.artworkUri.toString()
                                )
                            ) {
                                launchSingleTop = true
                            }
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
    }
    if (showAddToPlaylistDialog) {
        AddToPlaylist(
            onDismissRequest = { showAddToPlaylistDialog = false },
            mediaToAddToPlaylist = addToPlaylistSongs
        )
    }

    if (showArtistBiographyDialog) {
        BasicAlertDialog(
            onDismissRequest = { showArtistBiographyDialog = false},
            modifier = Modifier,
            content = {
                val scrollState = rememberScrollState()

                Surface(
                    modifier = Modifier.wrapContentWidth().wrapContentHeight(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = artist?.name.toString(),
                            style = MaterialTheme.typography.headlineMediumEmphasized,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = artist?.biography.toString()
                        )
                    }
                }
            }
        )
    }
}