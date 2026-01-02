@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongHelper {
    companion object {
        private const val TAG = "SongHelper"

        // Maximum items to send to MediaController to avoid Binder transaction limit
        // Android's Binder buffer is ~1MB, and each MediaItem can be several KB
        private const val MAX_QUEUE_SIZE = 200

        // Use a single lock object to avoid synchronizing on a mutable field
        private val tracklistLock = Any()

        // Thread-safe backing field for the tracklist - no need for synchronizedList since we use explicit locking
        private var _currentTracklist: MutableList<MediaItem> = mutableListOf()

        // Track the offset of the current window in the full tracklist
        private var windowStartOffset: Int = 0

        // Thread-safe getter/setter - returns a defensive copy (not synchronized, since it's a copy)
        var currentTracklist: MutableList<MediaItem>
            get() = synchronized(tracklistLock) {
                _currentTracklist.toMutableList()
            }
            set(value) = synchronized(tracklistLock) {
                _currentTracklist.clear()
                _currentTracklist.addAll(value)
                windowStartOffset = 0
            }

        /**
         * Thread-safe index lookup by mediaId.
         * Returns -1 if not found.
         */
        fun indexOfMediaId(mediaId: String): Int = synchronized(tracklistLock) {
            _currentTracklist.indexOfFirst { it.mediaId == mediaId }
        }

        /**
         * Thread-safe move operation for reordering.
         */
        fun moveItem(fromIndex: Int, toIndex: Int) = synchronized(tracklistLock) {
            if (fromIndex in _currentTracklist.indices && toIndex in _currentTracklist.indices) {
                val item = _currentTracklist.removeAt(fromIndex)
                _currentTracklist.add(toIndex, item)
            }
        }

        /**
         * Get the current size of the tracklist in a thread-safe manner.
         */
        fun tracklistSize(): Int = synchronized(tracklistLock) {
            _currentTracklist.size
        }

        suspend fun play(mediaItems: List<MediaItem>, index: Int, mediaController: MediaController?) {
            if (mediaItems.isEmpty())
                return

            // Store the full tracklist in memory
            currentTracklist = mediaItems.toMutableList()

            // Calculate the window to send to MediaController
            val (windowItems, windowIndex, windowOffset) = synchronized(tracklistLock) {
                if (mediaItems.size <= MAX_QUEUE_SIZE) {
                    // Small enough to send all items
                    windowStartOffset = 0
                    Triple(mediaItems, index, 0)
                } else {
                    // Calculate window centered around the selected index
                    val halfWindow = MAX_QUEUE_SIZE / 2
                    val start = (index - halfWindow).coerceAtLeast(0)
                    val end = (start + MAX_QUEUE_SIZE).coerceAtMost(mediaItems.size)
                    val adjustedStart = (end - MAX_QUEUE_SIZE).coerceAtLeast(0)

                    windowStartOffset = adjustedStart
                    val windowList = mediaItems.subList(adjustedStart, end)
                    val windowIdx = index - adjustedStart

                    Log.d(TAG, "Queue windowing: total=${mediaItems.size}, window=$adjustedStart-$end, windowIndex=$windowIdx")
                    Triple(windowList, windowIdx, adjustedStart)
                }
            }

            withContext(Dispatchers.Main) {
                mediaController?.setMediaItems(windowItems, windowIndex, 0)
                mediaController?.prepare()
                mediaController?.play()
            }
        }

        /**
         * Extend the queue window when approaching the edge.
         * Call this when the current track is near the start or end of the window.
         */
        suspend fun extendQueueWindow(mediaController: MediaController?, direction: WindowDirection) {
            if (mediaController == null) return

            val (newItems, insertIndex) = synchronized(tracklistLock) {
                val totalSize = _currentTracklist.size
                if (totalSize <= MAX_QUEUE_SIZE) {
                    return@synchronized null // Already showing all items
                }

                when (direction) {
                    WindowDirection.FORWARD -> {
                        val currentEnd = windowStartOffset + MAX_QUEUE_SIZE
                        if (currentEnd >= totalSize) {
                            null // Already at the end
                        } else {
                            val newEnd = (currentEnd + 50).coerceAtMost(totalSize)
                            val newItems = _currentTracklist.subList(currentEnd, newEnd)
                            Pair(newItems.toList(), -1) // -1 means append at end
                        }
                    }
                    WindowDirection.BACKWARD -> {
                        if (windowStartOffset <= 0) {
                            null // Already at the start
                        } else {
                            val newStart = (windowStartOffset - 50).coerceAtLeast(0)
                            val newItems = _currentTracklist.subList(newStart, windowStartOffset)
                            windowStartOffset = newStart
                            Pair(newItems.toList(), 0) // Insert at beginning
                        }
                    }
                }
            } ?: return

            withContext(Dispatchers.Main) {
                try {
                    if (insertIndex == -1) {
                        // Append at end
                        newItems.forEach { mediaController.addMediaItem(it) }
                    } else {
                        // Insert at beginning
                        newItems.forEachIndexed { idx, item ->
                            mediaController.addMediaItem(idx, item)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extending queue window", e)
                }
            }
        }

        enum class WindowDirection { FORWARD, BACKWARD }

        /**
         * Add a song to the queue
         * @param addToBottom true = add to end of queue, false = add after current song
         */
        suspend fun addToQueue(song: MediaItem, mediaController: MediaController?, addToBottom: Boolean = true) {
            if (addToBottom) {
                // Add to end of queue
                synchronized(tracklistLock) {
                    _currentTracklist.add(song)
                }
                withContext(Dispatchers.Main) {
                    mediaController?.addMediaItem(song)
                }
            } else {
                // Add after current song (same as playNext)
                playNext(song, mediaController)
            }
        }

        /**
         * Insert a song immediately after the currently playing track
         */
        suspend fun playNext(song: MediaItem, mediaController: MediaController?) {
            val currentIdx = withContext(Dispatchers.Main) {
                mediaController?.currentMediaItemIndex ?: 0
            }

            val insertIdx = synchronized(tracklistLock) {
                val idx = (currentIdx + 1).coerceAtMost(_currentTracklist.size)
                _currentTracklist.add(idx, song)
                idx
            }

            withContext(Dispatchers.Main) {
                mediaController?.addMediaItem(insertIdx, song)
            }
        }

        /**
         * Remove a song from the queue at the given index.
         * Returns true if removal was successful, false if index was invalid.
         */
        suspend fun removeFromQueue(index: Int, mediaController: MediaController?): Boolean {
            val removed = synchronized(tracklistLock) {
                if (index in _currentTracklist.indices) {
                    _currentTracklist.removeAt(index)
                    true
                } else {
                    false
                }
            }
            if (removed) {
                withContext(Dispatchers.Main) {
                    try {
                        if (index < (mediaController?.mediaItemCount ?: 0)) {
                            mediaController?.removeMediaItem(index)
                        }
                    } catch (e: IllegalStateException) {
                        // Player may have been released or index became invalid
                    }
                }
            }
            return removed
        }

        /**
         * Clear all songs from queue except the currently playing one
         */
        suspend fun clearQueue(mediaController: MediaController?) {
            val currentIdx = withContext(Dispatchers.Main) {
                mediaController?.currentMediaItemIndex ?: 0
            }

            val (newQueue, currentItem) = synchronized(tracklistLock) {
                val currentSong = _currentTracklist.getOrNull(currentIdx)
                _currentTracklist.clear()
                currentSong?.let { _currentTracklist.add(it) }
                Pair(_currentTracklist.toList(), currentSong)
            }

            withContext(Dispatchers.Main) {
                try {
                    if (currentItem != null) {
                        // atomic update: set queue to just the current item, preserving position
                        val pos = mediaController?.currentPosition ?: 0L
                        mediaController?.setMediaItems(newQueue, 0, pos)
                        mediaController?.prepare()
                        mediaController?.play()
                    } else {
                        mediaController?.clearMediaItems()
                    }
                } catch (e: IllegalStateException) {
                    // Player may have been released
                }
            }
        }

        /**
         * Shuffle the queue (keeping current song in place)
         */
        suspend fun shuffleQueue(mediaController: MediaController?) {
            if (mediaController == null) return

            val currentIdx = withContext(Dispatchers.Main) {
                mediaController.currentMediaItemIndex
            }

            val (shouldShuffle, windowQueue) = synchronized(tracklistLock) {
                if (_currentTracklist.size <= 1) {
                    Pair(false, emptyList())
                } else {
                    // Get actual index in full tracklist
                    val actualIdx = windowStartOffset + currentIdx
                    val currentSong = _currentTracklist.getOrNull(actualIdx)
                    val songsToShuffle = _currentTracklist.filterIndexed { idx, _ -> idx != actualIdx }
                    val shuffled = songsToShuffle.shuffled()

                    _currentTracklist.clear()
                    currentSong?.let { _currentTracklist.add(it) }
                    _currentTracklist.addAll(shuffled)

                    // Reset window to start and apply windowing
                    windowStartOffset = 0
                    val windowSize = _currentTracklist.size.coerceAtMost(MAX_QUEUE_SIZE)
                    Pair(true, _currentTracklist.take(windowSize))
                }
            }

            if (shouldShuffle) {
                // Rebuild the player queue atomically with windowed items
                withContext(Dispatchers.Main) {
                    try {
                        val currentPosition = mediaController.currentPosition
                        mediaController.setMediaItems(windowQueue, 0, currentPosition)
                        mediaController.prepare()
                        mediaController.play()
                    } catch (e: IllegalStateException) {
                        // Player may have been released
                    }
                }
            }
        }
    }
}