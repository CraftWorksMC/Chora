package com.craftworks.music.ui.elements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.formatMilliseconds
import com.craftworks.music.managers.settings.ArtworkSettingsManager
import com.craftworks.music.managers.settings.PlaybackSettingsManager
import com.craftworks.music.player.SongHelper
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.util.TextDisplayUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalSongCard(
    song: MediaItem,
    modifier: Modifier = Modifier,
    showTrackNumber: Boolean = false,
    isInSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
    onEnterSelectionMode: (() -> Unit)? = null,
    onDownload: ((MediaItem) -> Unit)? = null,
    onAddToPlaylist: ((MediaItem) -> Unit)? = null,
    // Performance: Pass these from parent instead of creating per-item
    isOffline: Boolean = false,
    generatedArtworkEnabled: Boolean = true,
    fallbackMode: ArtworkSettingsManager.FallbackMode = ArtworkSettingsManager.FallbackMode.PLACEHOLDER_DETECT,
    stripTrackNumbers: Boolean = false,
    // Artwork style settings for GeneratedAlbumArt
    artworkStyle: ArtworkSettingsManager.ArtworkStyle = ArtworkSettingsManager.ArtworkStyle.GRADIENT,
    colorPalette: ArtworkSettingsManager.ColorPalette = ArtworkSettingsManager.ColorPalette.MATERIAL_YOU,
    showInitials: Boolean = true,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val mediaController = rememberManagedMediaController().value
    var expanded by remember { mutableStateOf(false) }

    // Check if generated artwork is needed
    val artworkUri = song.mediaMetadata.artworkUri?.toString()
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
    // DISABLED palette extraction for list items - too expensive for large lists
    // Generated art will use default Material You colors instead
    val paletteColors: List<androidx.compose.ui.graphics.Color>? = null

    // Selection animations
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selection_scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            isInSelectionMode -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        label = "selection_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "selection_border"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        modifier = modifier
            .scale(selectionScale)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .combinedClickable(
                onClick = {
                    if (isInSelectionMode) {
                        onSelectionChange?.invoke(!isSelected)
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (!isInSelectionMode && onEnterSelectionMode != null) {
                        onEnterSelectionMode()
                    } else if (!isInSelectionMode) {
                        expanded = true
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier
                .height(72.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox when in selection mode
            if (isInSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else if (showTrackNumber) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(8.dp, 0.dp, 0.dp, 0.dp),
                        //.clip(CircleShape)
                        //.background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = song.mediaMetadata.trackNumber.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else {
                // Use pre-computed needsGeneratedArt from above
                if (needsGeneratedArt) {
                    GeneratedAlbumArtStatic(
                        title = song.mediaMetadata.title?.toString() ?: "?",
                        artist = song.mediaMetadata.artist?.toString(),
                        size = 64.dp,
                        modifier = Modifier
                            .padding(4.dp, 0.dp, 0.dp, 0.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = paletteColors,
                        artworkStyle = artworkStyle,
                        colorPalette = colorPalette,
                        showInitialsOverride = showInitials
                    )
                } else if (hasArtwork) {
                    val cacheKey = (song.mediaMetadata.extras?.getString("source") ?: "default") + "_" +
                        (song.mediaMetadata.extras?.getString("navidromeID") ?: song.mediaId)
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(song.mediaMetadata.artworkUri)
                            .crossfade(true)
                            .size(64)
                            .diskCacheKey(cacheKey)
                            .memoryCacheKey(cacheKey)
                            .build(),
                        contentDescription = "Album Image",
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp, 0.dp, 0.dp, 0.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        error = {
                            if (generatedArtworkEnabled) {
                                GeneratedAlbumArtStatic(
                                    title = song.mediaMetadata.title?.toString() ?: "?",
                                    artist = song.mediaMetadata.artist?.toString(),
                                    size = 64.dp,
                                    modifier = Modifier
                                        .padding(4.dp, 0.dp, 0.dp, 0.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    colors = paletteColors,
                                    artworkStyle = artworkStyle,
                                    colorPalette = colorPalette,
                                    showInitialsOverride = showInitials
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(4.dp, 0.dp, 0.dp, 0.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp, 0.dp, 0.dp, 0.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                Modifier
                    .padding(end = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = TextDisplayUtils.formatSongTitle(song.mediaMetadata.title.toString(), stripTrackNumbers),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )

                Text(
                    text = TextDisplayUtils.formatArtistName(song.mediaMetadata.artist?.toString()) + if (song.mediaMetadata.recordingYear != 0) " • " + song.mediaMetadata.recordingYear else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                    modifier = Modifier,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
            val formattedDuration by remember(song.mediaMetadata.durationMs) {
                derivedStateOf {
                    formatMilliseconds((song.mediaMetadata.durationMs?.div(1000))?.toInt() ?: 0)
                }
            }
            val isNavidromeSong = remember(song) {
                val navidromeID = song.mediaMetadata.extras?.getString("navidromeID")
                navidromeID != null && !navidromeID.startsWith("Local_")
            }

            // isOffline is now passed as a parameter from parent for performance

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                if (isNavidromeSong) {
                    Icon(
                        imageVector = if (isOffline)
                            ImageVector.vectorResource(R.drawable.rounded_download_24)
                        else
                            ImageVector.vectorResource(R.drawable.round_cloud_24),
                        contentDescription = if (isOffline) "Available offline" else "Cloud",
                        modifier = Modifier.size(12.dp),
                        tint = if (isOffline)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }

            Box(
                modifier = Modifier.width(48.dp)
            ) {
                IconButton(
                    modifier = Modifier,
                    onClick = { expanded = true },
//                    colors = IconButtonDefaults.iconButtonColors(
//                        containerColor = MaterialTheme.colorScheme.background,
//                        contentColor = MaterialTheme.colorScheme.primary,
//                        disabledContainerColor = MaterialTheme.colorScheme.background,
//                        disabledContentColor = MaterialTheme.colorScheme.onBackground
//                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "More menu"
                    )
                }

                val coroutineScope = rememberCoroutineScope()
                DropdownMenu(
                    modifier = Modifier,
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.Dialog_Add_To_Playlist).replace(
                                    "/ ",
                                    ""
                                )
                            )
                        },
                        onClick = {
                            println("Add Song To Playlist")
                            onAddToPlaylist?.invoke(song)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add To Playlist Icon"
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.Action_Add_To_Queue)) },
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val addToBottom = PlaybackSettingsManager(context).queueAddToBottomFlow.first()
                                    SongHelper.addToQueue(
                                        song,
                                        mediaController,
                                        addToBottom
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_queue_music_24),
                                contentDescription = "Add To Queue Icon"
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.Action_Play_Next)) },
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    SongHelper.playNext(song, mediaController)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_playlist_play_24),
                                contentDescription = "Play Next Icon"
                            )
                        }
                    )
                    DropdownMenuItem(
                        enabled = song.mediaMetadata.extras?.getString("navidromeID")?.startsWith("Local_") == false && onDownload != null,
                        text = {
                            Text(stringResource(R.string.Action_Download))
                        },
                        onClick = {
                            onDownload?.invoke(song)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_download_24),
                                contentDescription = "Download Icon"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = false, showBackground = true)
@Composable
fun PReviewHorizontalSongCard() {
    HorizontalSongCard(
        song = MediaItem.Builder()
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Lololol")
                    .build()
            ).build()
    ) { }
}

/**
 * A wrapper that adds swipe-to-queue functionality to any song card.
 * Swipe right to add to queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableToQueueSongCard(
    song: MediaItem,
    modifier: Modifier = Modifier,
    onAddedToQueue: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val mediaController = rememberManagedMediaController().value
    val coroutineScope = rememberCoroutineScope()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right -> add to queue
                    coroutineScope.launch {
                        try {
                            val addToBottom = PlaybackSettingsManager(context).queueAddToBottomFlow.first()
                            SongHelper.addToQueue(
                                song,
                                mediaController,
                                addToBottom
                            )
                            onAddedToQueue?.invoke()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    false // Don't dismiss, just add to queue
                }
                else -> false
            }
        },
        positionalThreshold = { it * 0.3f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            // Right swipe background (add to queue)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_queue_music_24),
                        contentDescription = "Add to Queue",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.Action_Add_To_Queue),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        enableDismissFromEndToStart = false, // Only allow right swipe
        content = { content() }
    )
}