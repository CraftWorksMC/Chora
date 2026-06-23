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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
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
                        else -> BottomSheetDefaults.ContainerColor
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().clickable {
                        mediaController.seekTo(index, 0)
                    }
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