package com.craftworks.music.ui.playing

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.craftworks.music.R
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/*
@OptIn(UnstableApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayQueueContent(
    viewModel: NowPlayingViewModel = viewModel(),
    mediaController: MediaController?
) {
    if (mediaController == null) return


    var currentMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    val recommendNextSong by viewModel.autoPlay.collectAsStateWithLifecycle(false)
    var similarSong by remember { mutableStateOf(MediaItem.EMPTY) }

    val currentTracklist = remember { mutableStateListOf<MediaItem>() }

    var isDragging by remember { mutableStateOf(false) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragCurrentIndex by remember { mutableIntStateOf(-1) }

    // Syncs local list from the player. Skipped while a drag is in progress.
    // FIX: Also clears similarSong if the service already added it to the player directly
    // (auto-play bypasses onAddMediaItems so SongHelper never sees it — but we read from
    // the player here, so we can detect the duplicate and prevent the suggestion row from
    // offering a song that's already queued).
    fun syncTracklist() {
        if (isDragging) return
        currentTracklist.clear()
        for (i in 0 until mediaController.mediaItemCount) {
            currentTracklist.add(mediaController.getMediaItemAt(i))
        }
        if (similarSong != MediaItem.EMPTY &&
            currentTracklist.any { it.mediaId == similarSong.mediaId }
        ) {
            similarSong = MediaItem.EMPTY
        }
    }

    LaunchedEffect(recommendNextSong) {
        if (recommendNextSong) {
            val last = mediaController.getMediaItemAt(mediaController.mediaItemCount - 1)
            // Only surface a suggestion if the last item isn't already the suggested song.
            similarSong = if (currentTracklist.any { it.mediaId == last.mediaId } &&
                currentTracklist.size > 1
            ) MediaItem.EMPTY else last
        } else {
            similarSong = MediaItem.EMPTY
        }
    }

    DisposableEffect(mediaController) {
        val callback = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaItem
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                syncTracklist()
            }
        }

        currentMediaItem = mediaController.currentMediaItem
        syncTracklist()
        mediaController.addListener(callback)

        onDispose { mediaController.removeListener(callback) }
    }

    val lazyListState = rememberLazyListState()

    val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
        currentTracklist.apply { add(to.index, removeAt(from.index)) }
        dragCurrentIndex = to.index
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = lazyListState
    ) {
        items(currentTracklist, key = { it.mediaId }) { mediaItem ->
            ReorderableItem(reorderableLazyColumnState, mediaItem.mediaId) { _ ->
                val isPlaying = mediaItem.mediaId == currentMediaItem?.mediaId
                val index = currentTracklist.indexOf(mediaItem)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .background(
                            if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable {
                            mediaController.playWhenReady = true
                            mediaController.seekTo(index, 0L)
                            mediaController.prepare()
                        }
                        .padding(horizontal = 16.dp)
                ) {
                    Box(modifier = Modifier.width(36.dp)) {
                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Now Playing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.baseline_drag_handle_24),
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.draggableHandle(
                            onDragStarted = {
                                isDragging = true
                                dragStartIndex = currentTracklist.indexOf(mediaItem)
                                dragCurrentIndex = dragStartIndex
                            },
                            onDragStopped = {
                                if (dragStartIndex != -1 && dragStartIndex != dragCurrentIndex) {
                                    mediaController.moveMediaItem(dragStartIndex, dragCurrentIndex)
                                    SongHelper.currentTracklist.apply {
                                        add(dragCurrentIndex, removeAt(dragStartIndex))
                                    }
                                }
                                isDragging = false
                                dragStartIndex = -1
                                dragCurrentIndex = -1
                            }
                        )
                    )
                }
            }
        }

        item(key = "toggle") {
            SettingsSwitch(
                selected = recommendNextSong,
                toggleEvent = { viewModel.setAutoPlay(!recommendNextSong) },
                settingsName = "Auto Play",
                settingsIcon = ImageVector.vectorResource(R.drawable.round_music_note_24)
            )
        }

        if (recommendNextSong && similarSong != MediaItem.EMPTY) {
            item(key = "suggestion") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .clickable {
                            if (currentTracklist.none { it.mediaId == similarSong.mediaId }) {
                                mediaController.addMediaItem(similarSong)
                            }
                            val idx = currentTracklist.indexOfFirst { it.mediaId == similarSong.mediaId }
                            if (idx != -1) {
                                mediaController.seekTo(idx, 0L)
                                mediaController.prepare()
                                mediaController.play()
                            }
                        }
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.width(12.dp + 36.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = similarSong.mediaMetadata.title?.toString() ?: "Unknown",
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = similarSong.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

}

 */

@Composable
fun PlayQueueContent(
    mediaController: MediaController?,
    modifier: Modifier = Modifier
) {
    if (mediaController == null)
        return
    val currentList = remember { mutableStateListOf<MediaItem>() }

    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragCurrentIndex by remember { mutableIntStateOf(-1) }

    var currentMediaItem by remember { mutableStateOf(mediaController.currentMediaItem) }

    val haptic = LocalHapticFeedback.current

    DisposableEffect(mediaController) {
        fun syncList() {
            currentList.clear()
            currentList.addAll(
                List(mediaController.mediaItemCount) { i -> mediaController.getMediaItemAt(i) }
            )
        }

        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                syncList()
                currentMediaItem = mediaController.currentMediaItem
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaController.currentMediaItem
            }
        }

        // Initial load
        syncList()
        currentMediaItem = mediaController.currentMediaItem
        mediaController.addListener(listener)

        onDispose { mediaController.removeListener(listener) }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (currentMediaItem in currentList) {
            lazyListState.scrollToItem(currentList.indexOf(currentMediaItem))
        }
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragStartIndex == -1) dragStartIndex = from.index
        currentList.add(to.index, currentList.removeAt(from.index))
        dragCurrentIndex = to.index
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(currentList, key = { _, item -> item.mediaId }) { index, item ->
            ReorderableItem(
                state = reorderableState,
                key = item.mediaId,
                modifier = Modifier.clickable {
                    mediaController.seekTo(index, 0)
                },
                animateItemModifier = Modifier.animateItem(
                    placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                )
            ) { draggingThis ->
                val elevation by animateDpAsState(
                    targetValue = if (draggingThis) 6.dp else 0.dp,
                    label = "queue_item_elevation"
                )
                val isCurrentItem = item == currentMediaItem

                Surface(
                    tonalElevation = elevation,
                    shadowElevation = elevation,
                    color = when {
                        draggingThis -> MaterialTheme.colorScheme.surfaceContainerHighest
                        isCurrentItem -> MaterialTheme.colorScheme.secondaryContainer
                        else -> Color.Transparent
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentItem && !draggingThis) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Now playing",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Title + artist
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = item.mediaMetadata.title?.toString() ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentItem) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.mediaMetadata.artist?.toString()?.let { artist ->
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (
                                            if (isCurrentItem) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                            ).copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Drag handle
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.baseline_drag_handle_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        // Commit to player only if the item actually moved.
                                        if (dragStartIndex != -1 && dragCurrentIndex != -1 &&
                                            dragStartIndex != dragCurrentIndex
                                        ) {
                                            mediaController.moveMediaItem(dragStartIndex, dragCurrentIndex)
                                        }
                                        dragStartIndex = -1
                                        dragCurrentIndex = -1
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}