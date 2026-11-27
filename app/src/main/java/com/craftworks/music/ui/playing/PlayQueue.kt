
package com.craftworks.music.ui.playing

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.dialogs.AddToPlaylist
import com.craftworks.music.utils.StringUtils
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID
import kotlin.math.roundToInt

data class QueueItem(
    val queueItemId: String = UUID.randomUUID().toString(),
    val mediaItem: MediaItem
)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayQueueContent(
    mediaController: MediaController?,
    modifier: Modifier = Modifier,
    dismissNowPlaying: () -> Unit
) {
    if (mediaController == null)
        return
    val currentList = remember { mutableStateListOf<QueueItem>() }

    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragCurrentIndex by remember { mutableIntStateOf(-1) }

    var currentMediaIndex by remember { mutableIntStateOf(-1) }
    var currentMediaItem: QueueItem? by remember { mutableStateOf(null) }

    var selectedMediaItem: QueueItem? by remember { mutableStateOf(null) }
    var selectedMediaIndex by remember { mutableIntStateOf(-1) }

    var showClearDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var addToPlaylistSongsOverride by remember { mutableStateOf<List<MediaItem>?>(null) }

    DisposableEffect(mediaController) {
        fun syncList() {
            val incomingMediaItems =
                List(mediaController.mediaItemCount) { mediaController.getMediaItemAt(it) }

            val syncedList = incomingMediaItems.map { mediaItem ->
                val matchIndex = currentList.indexOfFirst { it.mediaItem == mediaItem }
                if (matchIndex != -1)
                    currentList.removeAt(matchIndex)
                else
                    QueueItem(mediaItem = mediaItem)
            }

            currentList.clear()
            currentList.addAll(syncedList)
        }

        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (mediaController.mediaItemCount == 0) return
                syncList()
                currentMediaItem = currentList[mediaController.currentMediaItemIndex]
                currentMediaIndex = mediaController.currentMediaItemIndex
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = currentList[mediaController.currentMediaItemIndex]
                currentMediaIndex = mediaController.currentMediaItemIndex
            }
        }

        // Initial load
        if (mediaController.mediaItemCount == 0) return@DisposableEffect onDispose { }
        syncList()
        currentMediaItem = currentList[mediaController.currentMediaItemIndex]
        currentMediaIndex = mediaController.currentMediaItemIndex
        mediaController.addListener(listener)

        onDispose { mediaController.removeListener(listener) }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (currentList.any { it.queueItemId == currentMediaItem?.queueItemId }) {
            lazyListState.scrollToItem(mediaController.currentMediaItemIndex)
        }
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (dragStartIndex == -1) dragStartIndex = from.index
        currentList.add(to.index, currentList.removeAt(from.index))
        dragCurrentIndex = to.index
    }
    Column {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        )  {
            IconButton(onClick = {
                showAddToPlaylistDialog = true
            }, modifier = Modifier.size(40.dp).bounceClick()) {
                Icon(
                    ImageVector.vectorResource(R.drawable.save_24px),
                    contentDescription = stringResource(R.string.action_add_to_playlist),
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append((currentMediaIndex + 1).toString())
                        }
                        append(" / ${mediaController.mediaItemCount}")
                    },
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Row (
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.rounded_timer_24),
                        contentDescription = "DURATION ICON",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = StringUtils.formatSeconds(currentList.sumOf { it.mediaItem.mediaMetadata.durationMs?:0 }.div(1000)),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            IconButton(onClick = {
                showClearDialog = true
            }, modifier = Modifier.padding(4.dp).size(40.dp).bounceClick()) {
                Icon(
                    ImageVector.vectorResource(R.drawable.delete_sweep_24px),
                    contentDescription = stringResource(R.string.action_clear_queue),
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn(
            state = lazyListState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(currentList, key = { _, item -> item.queueItemId }) { index, item ->
                ReorderableItem(
                    state = reorderableState,
                    key = item.queueItemId,
                    animateItemModifier = Modifier.animateItem(
                        placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                    )
                )
                { draggingThis ->
                    QueueItemContent(
                        index = index,
                        item = item,
                        draggingThis = draggingThis,
                        currentMediaItem = currentMediaItem,
                        mediaController = mediaController,
                        onMenuOpen = {
                            selectedMediaIndex = index
                            selectedMediaItem = item
                        },
                        onDragStopped = {
                            // Commit to player only if the item actually moved.
                            if (dragStartIndex != -1 && dragCurrentIndex != -1 &&
                                dragStartIndex != dragCurrentIndex
                            ) {
                                mediaController.moveMediaItem(
                                    dragStartIndex,
                                    dragCurrentIndex
                                )
                            }
                            dragStartIndex = -1
                            dragCurrentIndex = -1
                        }
                    )
                }
            }
        }
    }

    selectedMediaItem?.let {
        QueueItemMenu(
            onDialogDismiss = { selectedMediaItem = null },
            onAddToPlaylist = {
                addToPlaylistSongsOverride = listOf(it.mediaItem)
                showAddToPlaylistDialog = true
            },
            queueItem = it,
            queueIndex = selectedMediaIndex,
            mediaController = mediaController
        )
    }
    if (showClearDialog) {
        AlertDialog(
            title = {
                Text(text = stringResource(R.string.action_clear_queue))
            },
            text = {
                Text(text = stringResource(R.string.clear_queue_confirm))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mediaController.clearMediaItems()
                        currentList.clear()
                        currentMediaItem = null
                        currentMediaIndex =  -1
                        showClearDialog = false
                        dismissNowPlaying()
                    }
                ) {
                    Text(stringResource(R.string.action_clear_queue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            onDismissRequest = { showClearDialog = false }
        )
    }

    if (showAddToPlaylistDialog) {
        AddToPlaylist(
            onDismissRequest = {
                showAddToPlaylistDialog = false
                addToPlaylistSongsOverride = null
                               },
            mediaToAddToPlaylist = addToPlaylistSongsOverride ?: currentList.map { it.mediaItem }
        )
    }
}

enum class DragValue { Settled, Revealed }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderableCollectionItemScope.QueueItemContent(index: Int, item: QueueItem, draggingThis: Boolean, currentMediaItem: QueueItem?, mediaController: MediaController, onMenuOpen: ()->Unit, onDragStopped: ()->Unit) {

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val elevation by animateDpAsState(
            targetValue = if (draggingThis) 6.dp else 0.dp,
            label = "queue_item_elevation"
        )
        val isCurrentItem = item.queueItemId == currentMediaItem?.queueItemId

        val density = LocalDensity.current
        val revealPx = with(density) { 64.dp.toPx() }

        val anchoredDraggableState = remember {
            AnchoredDraggableState(
                initialValue = DragValue.Settled,
                anchors = DraggableAnchors {
                    DragValue.Settled at 0f
                    DragValue.Revealed at -revealPx
                }
            )
        }

        Box {
            Box(Modifier.matchParentSize(), content = {
                // This is what's revealed behind the item while swiping
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .layout { measurable, constraints ->
                            val widthPx = (-anchoredDraggableState.requireOffset())
                                .coerceIn(0f, constraints.maxWidth.toFloat())
                                .roundToInt()
                            val placeable = measurable.measure(
                                constraints.copy(minWidth = widthPx, maxWidth = widthPx)
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    FilledIconButton(
                        onClick = {
                            mediaController.removeMediaItem(index)
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "DELETE", tint = Color.White)
                    }
                }
            })
            Box(
                Modifier
                    .offset {
                        IntOffset(anchoredDraggableState.requireOffset().roundToInt(), 0)
                    }
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Horizontal
                    )
            ) {
                Surface(
                    tonalElevation = elevation,
                    shadowElevation = elevation,
                    color = when {
                        draggingThis -> MaterialTheme.colorScheme.surfaceContainerHighest
                        isCurrentItem -> MaterialTheme.colorScheme.secondaryContainer
                        else -> BottomSheetDefaults.ContainerColor
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                mediaController.seekTo(index, 0)
                            },
                            onLongClick = onMenuOpen
                        )
                )
                {
                    Row(
                        modifier = Modifier.padding(4.dp),
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


                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(
                                    item.mediaItem.mediaMetadata.getProvider()
                                        ?.getImageUrl(
                                            id = item.mediaItem.mediaMetadata.id ?: "",
                                            itemType = LibraryType.SONG,
                                            size = 128
                                        )
                                )
                                .crossfade(true)
                                .diskCacheKey(item.mediaItem.mediaMetadata.id)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .placeholderMemoryCacheKey(item.mediaItem.mediaMetadata.id)
                                .build(),
                            contentDescription = "Album Image",
                            contentScale = ContentScale.FillHeight,
                            modifier = Modifier
                                .size(52.dp)
                                .padding(4.dp, 0.dp, 0.dp, 0.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )

                        // Title + artist
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = item.mediaItem.mediaMetadata.title?.toString()
                                    ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentItem) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.mediaItem.mediaMetadata.artist?.toString()
                                ?.let { artist ->
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
                                    onDragStopped = onDragStopped
                                )
                        )
                    }
                }
        }
    }
}

@Composable
private fun QueueMenuButton(icon: Int, text: Int, action: () -> Unit) {
    ListItem(
        onClick = action,
        modifier = Modifier
            .fillMaxWidth(),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        leadingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = stringResource(text)
            )
        },
        content = { Text(stringResource(text)) }
    )
}

@Composable
fun QueueItemMenu(
    onDialogDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    queueItem: QueueItem,
    queueIndex: Int,
    mediaController: MediaController,
    advancedMenu: Boolean = false
) {
    Dialog(onDismissRequest = { onDialogDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = queueItem.mediaItem.mediaMetadata.title.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                    QueueMenuButton(R.drawable.remove_circle_24px, R.string.action_remove_from_queue) {
                        mediaController.removeMediaItem(queueIndex)
                        onDialogDismiss()
                    }
                    if (advancedMenu) {
                        HorizontalDivider()
                        QueueMenuButton(R.drawable.vertical_align_top_24px, R.string.action_move_to_start) {
                            mediaController.moveMediaItem(queueIndex, 0)
                            onDialogDismiss()
                        }
                    }
                    QueueMenuButton(R.drawable.play_next_24px, R.string.action_move_next) {
                        val to = mediaController.currentMediaItemIndex
                        if (queueIndex > to) {
                            mediaController.moveMediaItem(queueIndex, to+1)
                        } else {
                            mediaController.moveMediaItem(queueIndex, to)
                        }
                        onDialogDismiss()
                    }
                    if (advancedMenu) {
                        QueueMenuButton(R.drawable.vertical_align_bottom_24px, R.string.action_move_to_end) {
                            mediaController.moveMediaItem(queueIndex, mediaController.mediaItemCount-1)
                            onDialogDismiss()
                        }
                        HorizontalDivider()
                    }
                    QueueMenuButton(R.drawable.rounded_add_24, R.string.action_add_to_playlist) {
                        onAddToPlaylist()
                        onDialogDismiss()
                    }
                }
            }

        }
    }
}