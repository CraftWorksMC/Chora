package com.craftworks.music.ui.playing

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.craftworks.music.R
import com.craftworks.music.player.ChoraMediaLibraryService
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(UnstableApi::class)
@Composable
fun PlayQueueContent(
    mediaController: MediaController?
) {
    if (mediaController == null) return

    var playlist by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var currentMediaItem by remember { mutableStateOf<MediaItem?>(null) }

    val refreshPlaylistState = {
        playlist = List(mediaController.mediaItemCount) { i ->
            mediaController.getMediaItemAt(i)
        }
    }

    // Observe controller changes
    DisposableEffect(mediaController) {
        val callback = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                super.onTimelineChanged(timeline, reason)
                refreshPlaylistState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaItem = mediaItem
            }
        }

        // Initial state
        playlist = List(mediaController.mediaItemCount) { i ->
            mediaController.getMediaItemAt(i)
        }
        currentMediaItem = mediaController.currentMediaItem

        mediaController.addListener(callback)
        onDispose {
            mediaController.removeListener(callback)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyColumnState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            //TODO: Reorder list items.
        }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = lazyListState
    ) {
        items(playlist, key = { it.mediaId }) { mediaItem ->
            ReorderableItem(reorderableLazyColumnState, mediaItem.mediaId) {
                val isPlaying = mediaItem.mediaId == currentMediaItem?.mediaId
                val index = playlist.indexOf(mediaItem)

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
                    // Track number or playing indicator
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


                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
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
                        modifier = Modifier.draggableHandle()
                    )
                }
            }
        }
    }
}