package com.craftworks.music.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.fadingEdge
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.ArtworkSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.providers.navidrome.downloadNavidromeSongs
import com.craftworks.music.providers.navidrome.setNavidromeStar
import com.craftworks.music.ui.elements.GeneratedAlbumArtStatic
import com.craftworks.music.ui.elements.GenrePill
import com.craftworks.music.ui.elements.HorizontalSongCard
import com.craftworks.music.ui.elements.SelectionActionBar
import com.craftworks.music.ui.elements.SwipeableToQueueSongCard
import com.craftworks.music.ui.elements.dialogs.AddSongToPlaylist
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import com.craftworks.music.ui.util.LayoutMode
import com.craftworks.music.ui.util.rememberFoldableState
import com.craftworks.music.ui.viewmodels.AlbumDetailsViewModel
import com.craftworks.music.ui.viewmodels.DownloadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
@Composable
fun AlbumDetails(
    selectedAlbumId: String = "",
    selectedAlbumImage: Uri = Uri.EMPTY,
    navHostController: NavHostController = rememberNavController(),
    mediaController: MediaController? = null,
    viewModel: AlbumDetailsViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel()
) {
    val foldableState = rememberFoldableState()
    val useSplitLayout = foldableState.layoutMode in listOf(
        LayoutMode.EXPANDED, LayoutMode.BOOK_MODE
    )
    val useTableTopLayout = foldableState.layoutMode == LayoutMode.TABLE_TOP

    var showLoading by remember { mutableStateOf(false) }
    val currentAlbum = viewModel.songsInAlbum.collectAsStateWithLifecycle().value
    val showTrackNumbers by AppearanceSettingsManager(LocalContext.current).showTrackNumbersFlow.collectAsStateWithLifecycle(false)

    // Selection state
    var isInSelectionMode by rememberSaveable { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<String>() }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedSongIds.clear()
    }

    LaunchedEffect(selectedAlbumId) {
        viewModel.loadAlbumDetails(selectedAlbumId)
    }

    // Loading spinner
    AnimatedVisibility(
        visible = showLoading,
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
        visible = currentAlbum.isNotEmpty(),
        enter = fadeIn()
    ) {
        val firstAlbum = currentAlbum.firstOrNull()
        var isStarred by remember(firstAlbum) {
            mutableStateOf(firstAlbum?.mediaMetadata?.extras?.getString("starred")?.isNotEmpty() == true)
        }
        val requester = remember { FocusRequester() }

        if (firstAlbum == null) return@AnimatedVisibility

        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val songs = if (currentAlbum.size > 1) currentAlbum.subList(1, currentAlbum.size) else emptyList()
        val isNavidrome = firstAlbum.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == false

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }

        when {
            useSplitLayout -> {
                // Side-by-side layout for EXPANDED and BOOK_MODE
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            start = 12.dp,
                            end = 12.dp
                        )
                        .dialogFocusable()
                ) {
                    // Left pane: Album header (40%)
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(end = 8.dp)
                    ) {
                        AlbumHeader(
                            selectedAlbumImage = selectedAlbumImage,
                            selectedAlbumId = selectedAlbumId,
                            firstAlbum = firstAlbum,
                            isStarred = isStarred,
                            isNavidrome = isNavidrome,
                            onBackClick = { navHostController.popBackStack() },
                            onStarClick = {
                                coroutineScope.launch {
                                    val newStarredState = !isStarred
                                    setNavidromeStar(
                                        star = newStarredState,
                                        albumId = firstAlbum.mediaMetadata.extras?.getString("navidromeID").toString()
                                    )
                                    isStarred = newStarredState
                                    viewModel.loadAlbumDetails(selectedAlbumId)
                                }
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        AlbumActionButtons(
                            songs = songs,
                            mediaController = mediaController,
                            coroutineScope = coroutineScope,
                            requester = requester,
                            isNavidrome = isNavidrome,
                            context = context,
                            vertical = true
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    // Right pane: Song list (60%)
                    AlbumSongList(
                        songs = songs,
                        showTrackNumbers = showTrackNumbers,
                        mediaController = mediaController,
                        coroutineScope = coroutineScope,
                        modifier = Modifier
                            .weight(0.6f)
                            .padding(start = 8.dp),
                        isInSelectionMode = isInSelectionMode,
                        selectedSongIds = selectedSongIds,
                        onEnterSelectionMode = { songId ->
                            isInSelectionMode = true
                            selectedSongIds.add(songId)
                        },
                        onSelectionChange = { songId, selected ->
                            if (selected) selectedSongIds.add(songId)
                            else selectedSongIds.remove(songId)
                        },
                        onDownloadSelected = {
                            val selected = songs.filter { selectedSongIds.contains(it.mediaId) }
                            downloadViewModel.queueDownloads(selected.map { it.mediaMetadata })
                            exitSelectionMode()
                        }
                    )
                }
            }
            useTableTopLayout -> {
                // Table-top layout: header on top, songs below (for horizontal fold)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            start = 12.dp,
                            end = 12.dp
                        )
                        .dialogFocusable()
                ) {
                    // Top pane: Album header (above fold)
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        AlbumHeader(
                            selectedAlbumImage = selectedAlbumImage,
                            selectedAlbumId = selectedAlbumId,
                            firstAlbum = firstAlbum,
                            isStarred = isStarred,
                            isNavidrome = isNavidrome,
                            onBackClick = { navHostController.popBackStack() },
                            onStarClick = {
                                coroutineScope.launch {
                                    val newStarredState = !isStarred
                                    setNavidromeStar(
                                        star = newStarredState,
                                        albumId = firstAlbum.mediaMetadata.extras?.getString("navidromeID").toString()
                                    )
                                    isStarred = newStarredState
                                    viewModel.loadAlbumDetails(selectedAlbumId)
                                }
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        AlbumActionButtons(
                            songs = songs,
                            mediaController = mediaController,
                            coroutineScope = coroutineScope,
                            requester = requester,
                            isNavidrome = isNavidrome,
                            context = context,
                            vertical = false
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    // Bottom pane: Song list (below fold)
                    AlbumSongList(
                        songs = songs,
                        showTrackNumbers = showTrackNumbers,
                        mediaController = mediaController,
                        coroutineScope = coroutineScope,
                        modifier = Modifier.weight(0.55f),
                        isInSelectionMode = isInSelectionMode,
                        selectedSongIds = selectedSongIds,
                        onEnterSelectionMode = { songId ->
                            isInSelectionMode = true
                            selectedSongIds.add(songId)
                        },
                        onSelectionChange = { songId, selected ->
                            if (selected) selectedSongIds.add(songId)
                            else selectedSongIds.remove(songId)
                        },
                        onDownloadSelected = {
                            val selected = songs.filter { selectedSongIds.contains(it.mediaId) }
                            downloadViewModel.queueDownloads(selected.map { it.mediaMetadata })
                            exitSelectionMode()
                        }
                    )
                }
            }
            else -> {
                // Compact layout: single column (original behavior)
                AlbumDetailsCompact(
                    selectedAlbumImage = selectedAlbumImage,
                    selectedAlbumId = selectedAlbumId,
                    firstAlbum = firstAlbum,
                    songs = songs,
                    isStarred = isStarred,
                    isNavidrome = isNavidrome,
                    showTrackNumbers = showTrackNumbers,
                    mediaController = mediaController,
                    coroutineScope = coroutineScope,
                    requester = requester,
                    viewModel = viewModel,
                    navHostController = navHostController,
                    onStarredChange = { isStarred = it },
                    isInSelectionMode = isInSelectionMode,
                    selectedSongIds = selectedSongIds,
                    onEnterSelectionMode = { songId ->
                        isInSelectionMode = true
                        selectedSongIds.add(songId)
                    },
                    onSelectionChange = { songId, selected ->
                        if (selected) selectedSongIds.add(songId)
                        else selectedSongIds.remove(songId)
                    },
                    onDownloadSelected = {
                        val selected = songs.filter { selectedSongIds.contains(it.mediaId) }
                        downloadViewModel.queueDownloads(selected.map { it.mediaMetadata })
                        exitSelectionMode()
                    }
                )
            }
        }
    }

}

@Composable
private fun AlbumHeader(
    selectedAlbumImage: Uri,
    selectedAlbumId: String,
    firstAlbum: MediaItem,
    isStarred: Boolean,
    isNavidrome: Boolean,
    onBackClick: () -> Unit,
    onStarClick: () -> Unit
) {
    val context = LocalContext.current
    val imageFadingEdge = Brush.verticalGradient(listOf(Color.Red.copy(0.75f), Color.Transparent))

    // Read artwork settings
    val artworkSettings = remember { ArtworkSettingsManager(context) }
    val generatedArtworkEnabled by artworkSettings.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)
    val fallbackMode by artworkSettings.fallbackModeFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT)
    val artworkStyle by artworkSettings.artworkStyleFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ArtworkStyle.GRADIENT)
    val colorPalette by artworkSettings.colorPaletteFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ColorPalette.MATERIAL_YOU)
    val showInitials by artworkSettings.showInitialsFlow.collectAsStateWithLifecycle(true)

    // Use navigation param if available, otherwise fall back to album's artwork
    val imageToShow = if (selectedAlbumImage != Uri.EMPTY && selectedAlbumImage.toString().isNotEmpty()) {
        selectedAlbumImage
    } else {
        firstAlbum.mediaMetadata.artworkUri
    }

    // Check if we need generated art
    val artworkUri = imageToShow?.toString()
    val hasArtwork = !artworkUri.isNullOrEmpty()
    val needsGeneratedArt = generatedArtworkEnabled && (
        fallbackMode == ArtworkSettingsManager.FallbackMode.ALWAYS ||
        !hasArtwork ||
        (fallbackMode == ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT &&
            artworkUri != null && (
            artworkUri.contains("placeholder") ||
            artworkUri.endsWith("/coverArt") ||
            artworkUri.contains("coverArt?id=&") ||
            (artworkUri.contains("coverArt?size=") && !artworkUri.contains("id="))))
    )

    val albumTitle = firstAlbum.mediaMetadata.title?.toString() ?: "Album"
    val albumArtist = firstAlbum.mediaMetadata.artist?.toString()

    Box(
        modifier = Modifier
            .height(224.dp)
            .fillMaxWidth()
    ) {
        if (needsGeneratedArt) {
            // Use generated artwork
            GeneratedAlbumArtStatic(
                title = albumTitle,
                artist = albumArtist,
                size = 224.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadingEdge(imageFadingEdge)
                    .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
                    .blur(8.dp),
                artworkStyle = artworkStyle,
                colorPalette = colorPalette,
                showInitialsOverride = showInitials
            )
        } else {
            // Try to load actual artwork with generated art as fallback
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageToShow)
                    .diskCacheKey(selectedAlbumId)
                    .memoryCacheKey(selectedAlbumId)
                    .crossfade(true)
                    .build(),
                contentScale = ContentScale.FillWidth,
                contentDescription = "Album Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .fadingEdge(imageFadingEdge)
                    .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
                    .blur(8.dp),
                error = {
                    if (generatedArtworkEnabled) {
                        GeneratedAlbumArtStatic(
                            title = albumTitle,
                            artist = albumArtist,
                            size = 224.dp,
                            modifier = Modifier.fillMaxWidth(),
                            artworkStyle = artworkStyle,
                            colorPalette = colorPalette,
                            showInitialsOverride = showInitials
                        )
                    }
                }
            )
        }

        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(top = 12.dp, start = 12.dp)
                .size(32.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Back",
                modifier = Modifier.size(32.dp)
            )
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(
                text = firstAlbum.mediaMetadata.title.toString(),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 32.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = firstAlbum.mediaMetadata.artist.toString() + " • " + formatMilliseconds(
                    firstAlbum.mediaMetadata.durationMs?.div(1000)?.toInt() ?: 0
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Normal,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (!firstAlbum.mediaMetadata.genre.isNullOrEmpty()) {
                    firstAlbum.mediaMetadata.genre?.split(",")?.forEach {
                        GenrePill(it.toString())
                    }
                }
            }
        }

        if (isNavidrome) {
            Button(
                onClick = onStarClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(32.dp),
                contentPadding = PaddingValues(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Crossfade(targetState = isStarred) {
                    if (it) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.round_star_24),
                            contentDescription = "Unstar Album",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.round_star_border_24),
                            contentDescription = "Star Album",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumActionButtons(
    songs: List<MediaItem>,
    mediaController: MediaController?,
    coroutineScope: CoroutineScope,
    requester: FocusRequester,
    isNavidrome: Boolean,
    context: android.content.Context,
    vertical: Boolean
) {
    if (vertical) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (songs.isNotEmpty()) {
                        coroutineScope.launch {
                            SongHelper.play(songs, 0, mediaController)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(Icons.Rounded.PlayArrow, "Play Album")
                    Text(stringResource(R.string.Action_Play), maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = {
                    if (songs.isNotEmpty()) {
                        mediaController?.shuffleModeEnabled = true
                        coroutineScope.launch {
                            val random = songs.indices.random()
                            SongHelper.play(songs, random, mediaController)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(ImageVector.vectorResource(R.drawable.round_shuffle_28), "Shuffle Album")
                    Text(stringResource(R.string.Action_Shuffle), maxLines = 1)
                }
            }

            if (songs.isNotEmpty() && isNavidrome) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            downloadNavidromeSongs(context, songs.map { it.mediaMetadata })
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                        Icon(ImageVector.vectorResource(R.drawable.rounded_download_24), "Download Album")
                        Text(
                            stringResource(R.string.Action_Download) + " " + stringResource(R.string.Albums),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (songs.isNotEmpty()) {
                        coroutineScope.launch {
                            SongHelper.play(songs, 0, mediaController)
                        }
                    }
                },
                modifier = Modifier
                    .widthIn(min = 128.dp, max = 320.dp)
                    .focusRequester(requester)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(Icons.Rounded.PlayArrow, "Play Album")
                    Text(stringResource(R.string.Action_Play), maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = {
                    if (songs.isNotEmpty()) {
                        mediaController?.shuffleModeEnabled = true
                        coroutineScope.launch {
                            val random = songs.indices.random()
                            SongHelper.play(songs, random, mediaController)
                        }
                    }
                },
                modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(ImageVector.vectorResource(R.drawable.round_shuffle_28), "Shuffle Album")
                    Text(stringResource(R.string.Action_Shuffle), maxLines = 1)
                }
            }
        }

        if (songs.isNotEmpty() && isNavidrome) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        downloadNavidromeSongs(context, songs.map { it.mediaMetadata })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                    Icon(ImageVector.vectorResource(R.drawable.rounded_download_24), "Download Album")
                    Text(
                        stringResource(R.string.Action_Download) + " " + stringResource(R.string.Albums),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumSongList(
    songs: List<MediaItem>,
    showTrackNumbers: Boolean,
    mediaController: MediaController?,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    isInSelectionMode: Boolean = false,
    selectedSongIds: MutableList<String> = mutableListOf(),
    onEnterSelectionMode: (String) -> Unit = {},
    onSelectionChange: (String, Boolean) -> Unit = { _, _ -> },
    onDownloadSelected: () -> Unit = {}
) {
    // Read artwork settings at parent level for performance
    val context = LocalContext.current
    val artworkSettings = remember { ArtworkSettingsManager(context) }
    val generatedArtworkEnabled by artworkSettings.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)
    val fallbackMode by artworkSettings.fallbackModeFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT)
    val artworkStyle by artworkSettings.artworkStyleFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ArtworkStyle.GRADIENT)
    val colorPalette by artworkSettings.colorPaletteFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ColorPalette.MATERIAL_YOU)
    val showInitials by artworkSettings.showInitialsFlow.collectAsStateWithLifecycle(true)

    // Use remember to avoid recomputation on every recomposition
    val groupedAlbums = remember(songs) { songs.groupBy { it.mediaMetadata.discNumber } }

    Column(modifier = modifier.fillMaxWidth()) {
        // Selection Action Bar
        AnimatedVisibility(
            visible = isInSelectionMode,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SelectionActionBar(
                selectedCount = selectedSongIds.size,
                onCancel = {
                    selectedSongIds.clear()
                },
                onSelectAll = {
                    selectedSongIds.clear()
                    selectedSongIds.addAll(songs.map { it.mediaId })
                },
                onDownload = onDownloadSelected
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (groupedAlbums.size > 1) {
                groupedAlbums.forEach { (discNumber, albumsInGroup) ->
                    item(key = "disc_header_$discNumber") {
                        Column {
                            Text(
                                text = stringResource(R.string.Album_Disc_Number) + discNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(1.dp)
                                    .fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }
                    }
                    items(albumsInGroup, key = { it.mediaId }) { song ->
                        HorizontalSongCard(
                            song = song,
                            modifier = Modifier.animateItem(),
                            showTrackNumber = showTrackNumbers,
                            isInSelectionMode = isInSelectionMode,
                            isSelected = selectedSongIds.contains(song.mediaId),
                            onEnterSelectionMode = {
                                onEnterSelectionMode(song.mediaId)
                            },
                            onSelectionChange = { selected ->
                                onSelectionChange(song.mediaId, selected)
                            },
                            generatedArtworkEnabled = generatedArtworkEnabled,
                            fallbackMode = fallbackMode,
                            artworkStyle = artworkStyle,
                            colorPalette = colorPalette,
                            showInitials = showInitials,
                            onClick = {
                                coroutineScope.launch {
                                    val index = songs.indexOf(song)
                                    if (index != -1) {
                                        SongHelper.play(songs, index, mediaController)
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                items(songs, key = { it.mediaId }) { song ->
                    HorizontalSongCard(
                        song = song,
                        modifier = Modifier.animateItem(),
                        showTrackNumber = showTrackNumbers,
                        isInSelectionMode = isInSelectionMode,
                        isSelected = selectedSongIds.contains(song.mediaId),
                        onEnterSelectionMode = {
                            onEnterSelectionMode(song.mediaId)
                        },
                        onSelectionChange = { selected ->
                            onSelectionChange(song.mediaId, selected)
                        },
                        generatedArtworkEnabled = generatedArtworkEnabled,
                        fallbackMode = fallbackMode,
                        artworkStyle = artworkStyle,
                        colorPalette = colorPalette,
                        showInitials = showInitials,
                        onClick = {
                            coroutineScope.launch {
                                val index = songs.indexOf(song)
                                if (index != -1) {
                                    SongHelper.play(songs, index, mediaController)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun AlbumDetailsCompact(
    selectedAlbumImage: Uri,
    selectedAlbumId: String,
    firstAlbum: MediaItem,
    songs: List<MediaItem>,
    isStarred: Boolean,
    isNavidrome: Boolean,
    showTrackNumbers: Boolean,
    mediaController: MediaController?,
    coroutineScope: CoroutineScope,
    requester: FocusRequester,
    viewModel: AlbumDetailsViewModel,
    navHostController: NavHostController,
    onStarredChange: (Boolean) -> Unit,
    isInSelectionMode: Boolean = false,
    selectedSongIds: MutableList<String> = mutableListOf(),
    onEnterSelectionMode: (String) -> Unit = {},
    onSelectionChange: (String, Boolean) -> Unit = { _, _ -> },
    onDownloadSelected: () -> Unit = {}
) {
    val context = LocalContext.current

    // Read artwork settings at parent level for performance
    val artworkSettings = remember { ArtworkSettingsManager(context) }
    val generatedArtworkEnabled by artworkSettings.generatedArtworkEnabledFlow.collectAsStateWithLifecycle(true)
    val fallbackMode by artworkSettings.fallbackModeFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT)
    val artworkStyle by artworkSettings.artworkStyleFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ArtworkStyle.GRADIENT)
    val colorPalette by artworkSettings.colorPaletteFlow.collectAsStateWithLifecycle(ArtworkSettingsManager.ColorPalette.MATERIAL_YOU)
    val showInitials by artworkSettings.showInitialsFlow.collectAsStateWithLifecycle(true)

    // Use remember to avoid recomputation on every recomposition
    val groupedAlbums = remember(songs) { songs.groupBy { it.mediaMetadata.discNumber } }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .dialogFocusable(),
        contentPadding = PaddingValues(
            bottom = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        )
    ) {
        item(key = "album_header") {
            AlbumHeader(
                selectedAlbumImage = selectedAlbumImage,
                selectedAlbumId = selectedAlbumId,
                firstAlbum = firstAlbum,
                isStarred = isStarred,
                isNavidrome = isNavidrome,
                onBackClick = { navHostController.popBackStack() },
                onStarClick = {
                    coroutineScope.launch {
                        val newStarredState = !isStarred
                        setNavidromeStar(
                            star = newStarredState,
                            albumId = firstAlbum.mediaMetadata.extras?.getString("navidromeID").toString()
                        )
                        onStarredChange(newStarredState)
                        viewModel.loadAlbumDetails(selectedAlbumId)
                    }
                }
            )
        }

        item(key = "action_buttons") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            coroutineScope.launch {
                                SongHelper.play(songs, 0, mediaController)
                            }
                        }
                    },
                    modifier = Modifier
                        .widthIn(min = 128.dp, max = 320.dp)
                        .focusRequester(requester)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                        Icon(Icons.Rounded.PlayArrow, "Play Album")
                        Text(stringResource(R.string.Action_Play), maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            mediaController?.shuffleModeEnabled = true
                            coroutineScope.launch {
                                val random = songs.indices.random()
                                SongHelper.play(songs, random, mediaController)
                            }
                        }
                    },
                    modifier = Modifier.widthIn(min = 128.dp, max = 320.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                        Icon(ImageVector.vectorResource(R.drawable.round_shuffle_28), "Shuffle Album")
                        Text(stringResource(R.string.Action_Shuffle), maxLines = 1)
                    }
                }
            }
        }

        if (songs.isNotEmpty() && isNavidrome) {
            item(key = "download_button") {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            downloadNavidromeSongs(context, songs.map { it.mediaMetadata })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
                        Icon(ImageVector.vectorResource(R.drawable.rounded_download_24), "Download Album")
                        Text(
                            stringResource(R.string.Action_Download) + " " + stringResource(R.string.Albums),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Selection Action Bar
        item(key = "selection_bar") {
            AnimatedVisibility(
                visible = isInSelectionMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SelectionActionBar(
                    selectedCount = selectedSongIds.size,
                    onCancel = {
                        selectedSongIds.clear()
                    },
                    onSelectAll = {
                        selectedSongIds.clear()
                        selectedSongIds.addAll(songs.map { it.mediaId })
                    },
                    onDownload = onDownloadSelected
                )
            }
        }

        if (groupedAlbums.size > 1) {
            groupedAlbums.forEach { (discNumber, albumsInGroup) ->
                item(key = "compact_disc_header_$discNumber") {
                    Column {
                        Text(
                            text = stringResource(R.string.Album_Disc_Number) + discNumber.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }
                items(albumsInGroup, key = { it.mediaId }) { song ->
                    HorizontalSongCard(
                        song = song,
                        modifier = Modifier.animateItem(),
                        showTrackNumber = showTrackNumbers,
                        isInSelectionMode = isInSelectionMode,
                        isSelected = selectedSongIds.contains(song.mediaId),
                        onEnterSelectionMode = {
                            onEnterSelectionMode(song.mediaId)
                        },
                        onSelectionChange = { selected ->
                            onSelectionChange(song.mediaId, selected)
                        },
                        generatedArtworkEnabled = generatedArtworkEnabled,
                        fallbackMode = fallbackMode,
                        artworkStyle = artworkStyle,
                        colorPalette = colorPalette,
                        showInitials = showInitials,
                        onClick = {
                            coroutineScope.launch {
                                val index = songs.indexOf(song)
                                if (index != -1) {
                                    SongHelper.play(songs, index, mediaController)
                                }
                            }
                        }
                    )
                }
            }
        } else {
            items(songs, key = { it.mediaId }) { song ->
                HorizontalSongCard(
                    song = song,
                    modifier = Modifier.animateItem(),
                    showTrackNumber = showTrackNumbers,
                    isInSelectionMode = isInSelectionMode,
                    isSelected = selectedSongIds.contains(song.mediaId),
                    onEnterSelectionMode = {
                        onEnterSelectionMode(song.mediaId)
                    },
                    onSelectionChange = { selected ->
                        onSelectionChange(song.mediaId, selected)
                    },
                    generatedArtworkEnabled = generatedArtworkEnabled,
                    fallbackMode = fallbackMode,
                    artworkStyle = artworkStyle,
                    colorPalette = colorPalette,
                    showInitials = showInitials,
                    onClick = {
                        coroutineScope.launch {
                            val index = songs.indexOf(song)
                            if (index != -1) {
                                SongHelper.play(songs, index, mediaController)
                            }
                        }
                    }
                )
            }
        }
    }
}