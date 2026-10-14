package com.craftworks.music.ui.playing

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.StarRating
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.SongListSort
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.getProvider
import com.craftworks.music.data.model.id
import com.craftworks.music.player.ChoraMediaLibraryService
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.dialogs.AddToPlaylist
import com.craftworks.music.utils.StringUtils
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedPlayQueueContent(
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
    var showSortDialog by remember { mutableStateOf(false) }
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
            ChoraMediaLibraryService.getInstance()?.player?.let { player ->
                PlayPauseButton(player, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(40.dp))
            }

            IconButton(onClick = {
                showSortDialog = true
            }, modifier = Modifier.size(40.dp).bounceClick() ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.rounded_sort_24),
                    contentDescription = stringResource(R.string.button_sort_by),
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
                showAddToPlaylistDialog = true
            }, modifier = Modifier.size(40.dp).bounceClick()) {
                Icon(
                    ImageVector.vectorResource(R.drawable.save_24px),
                    contentDescription = stringResource(R.string.action_add_to_playlist),
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            onDialogDismiss = {selectedMediaItem = null},
            onAddToPlaylist = {
                addToPlaylistSongsOverride = listOf(it.mediaItem)
                showAddToPlaylistDialog = true
            },
            queueItem = it,
            queueIndex = selectedMediaIndex,
            mediaController = mediaController,
            advancedMenu = true
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

    if (showSortDialog) {
        AdvancedQueueSortDialog(
            onDismissRequest = {
                showSortDialog = false
            },
            mediaController = mediaController
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


private fun syncMediaControllerToList(mediaController: MediaController, originList: MutableList<MediaItem>, destinationList: List<MediaItem>) {

    if (originList.size > 100) {
        val currentItem = mediaController.currentMediaItem ?: return
        val currentIndex = mediaController.currentMediaItemIndex

        val newIndex = destinationList.indexOf(currentItem)
            .coerceAtLeast(0)

        mediaController.replaceMediaItems(currentIndex+1, mediaController.mediaItemCount, destinationList.subList(newIndex + 1, destinationList.size))
        mediaController.replaceMediaItems(0, currentIndex, destinationList.subList(0, newIndex))
    } else {
         fun moveItem(item: MediaItem, to: Int) {
            val from = originList.indexOf(item)
            mediaController.moveMediaItem(from, to)
            originList.add(to, originList.removeAt(from))
        }
        destinationList.forEachIndexed { index, item -> moveItem(item, index) }
    }
}

@Suppress("UNCHECKED_CAST")
private fun sortQueue(mediaController: MediaController, sortOrder: SortOrder, sort: SongListSort) {
    val currentMediaItems = MutableList(mediaController.mediaItemCount) { mediaController.getMediaItemAt(it) }

    val sortBindings = mapOf<SongListSort, (MediaItem) -> Comparable<*>?>(
        SongListSort.ALBUM to { it.mediaMetadata.albumTitle.toString() },
        SongListSort.ALBUM_ARTIST to { it.mediaMetadata.albumArtist.toString() },
        SongListSort.ARTIST to { it.mediaMetadata.artist.toString() },
        SongListSort.BPM to { it.mediaMetadata.extras?.getInt("bpm") },
        SongListSort.DURATION to { it.mediaMetadata.durationMs },
        SongListSort.ID to { currentMediaItems.indexOf(it) },
        SongListSort.NAME to { it.mediaMetadata.title.toString() },
        SongListSort.PLAY_COUNT to { it.mediaMetadata.extras?.getInt("playCount") },
        SongListSort.RATING to { (it.mediaMetadata.userRating as StarRating).starRating.toInt() },
        SongListSort.TRACK_NUMBER to { it.mediaMetadata.trackNumber },
        SongListSort.YEAR to { it.mediaMetadata.releaseYear.toString() },
    )

    val sortList =
        if (sort == SongListSort.RANDOM) currentMediaItems.shuffled()
        else if ((sortOrder == SortOrder.DESC) or (sort == SongListSort.ID)) currentMediaItems.sortedByDescending(sortBindings[sort] as (MediaItem) -> Comparable<Any>?)
        else currentMediaItems.sortedBy(sortBindings[sort] as (MediaItem) -> Comparable<Any>?)

    syncMediaControllerToList(mediaController, currentMediaItems, sortList)
}

@Composable
private fun AdvancedQueueSortDialog(onDismissRequest: ()->Unit, mediaController: MediaController) {
    var selectedOrder by remember { mutableStateOf(SortOrder.ASC) }
    var selectedSort by remember { mutableStateOf(SongListSort.ID) }

    val sortTranslationBindings = mapOf(
        SongListSort.ALBUM to R.string.sort_by_album,
        SongListSort.ALBUM_ARTIST to R.string.sort_by_album_artist,
        SongListSort.ARTIST to R.string.sort_by_artist,
        SongListSort.BPM to R.string.sort_by_bpm,
        SongListSort.DURATION to R.string.sort_by_duration,
        SongListSort.ID to R.string.sort_queue_reverse, // Hack to reverse the queue
        SongListSort.NAME to R.string.sort_by_name,
        SongListSort.PLAY_COUNT to R.string.sort_by_play_count,
        SongListSort.RANDOM to R.string.sort_by_random,
        SongListSort.RATING to R.string.sort_by_rating,
        SongListSort.TRACK_NUMBER to R.string.sort_by_track_number,
        SongListSort.YEAR to R.string.sort_by_year,
    )

    AlertDialog(
        title = {
            Text(text = stringResource(R.string.sort_queue_title))
        },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 0,
                            count = 2
                        ),
                        onClick = { selectedOrder = SortOrder.ASC },
                        selected = selectedOrder == SortOrder.ASC,
                        icon = {
                                SegmentedButtonDefaults.Icon(active = selectedOrder == SortOrder.ASC) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.arrow_upward_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                    )
                                }
                               },
                        label = {
                            Text(stringResource(R.string.button_sort_order_ascending))
                        }
                    )
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 1,
                            count = 2
                        ),
                        onClick = { selectedOrder = SortOrder.DESC },
                        selected = selectedOrder == SortOrder.DESC,
                        icon = {
                            SegmentedButtonDefaults.Icon(active = selectedOrder == SortOrder.DESC) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.arrow_downward_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        label = {
                            Text(stringResource(R.string.button_sort_order_descending))
                        }
                    )
                }

                LazyColumn {
                    itemsIndexed(
                        items = listOf(
                            SongListSort.ID,
                            SongListSort.RANDOM,
                            SongListSort.ALBUM,
                            SongListSort.ALBUM_ARTIST,
                            SongListSort.ARTIST,
                            SongListSort.BPM,
                            SongListSort.DURATION,
                            SongListSort.NAME,
                            SongListSort.PLAY_COUNT,
                            SongListSort.TRACK_NUMBER,
                            SongListSort.YEAR,
                        ),
                        key = { _, sort -> sort }
                    ) { _, sort ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (sort == selectedSort),
                                    onClick = { selectedSort = sort },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (sort == selectedSort),
                                onClick = null
                            )
                            Text(
                                text = sortTranslationBindings[sort]?.let { id ->
                                    stringResource(
                                        id
                                    )
                                } ?: sort.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    sortQueue(mediaController, selectedOrder, selectedSort)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.sort_queue_sort))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest
    )
}