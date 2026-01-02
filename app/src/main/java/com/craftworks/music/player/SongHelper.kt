@file:OptIn(UnstableApi::class) package com.craftworks.music.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongHelper {
    companion object {
        // Use a single lock object to avoid synchronizing on a mutable field
        private val tracklistLock = Any()

        // Thread-safe backing field for the tracklist - no need for synchronizedList since we use explicit locking
        private var _currentTracklist: MutableList<MediaItem> = mutableListOf()

        // Thread-safe getter/setter - returns a defensive copy (not synchronized, since it's a copy)
        var currentTracklist: MutableList<MediaItem>
            get() = synchronized(tracklistLock) {
                _currentTracklist.toMutableList()
            }
            set(value) = synchronized(tracklistLock) {
                _currentTracklist.clear()
                _currentTracklist.addAll(value)
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

            currentTracklist = mediaItems.toMutableList()
            withContext(Dispatchers.Main) {
                mediaController?.setMediaItems(mediaItems, index, 0)
                mediaController?.prepare()
                mediaController?.play()
            }
        }

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

            val (shouldShuffle, newQueue) = synchronized(tracklistLock) {
                if (_currentTracklist.size <= 1) {
                    Pair(false, emptyList())
                } else {
                    val currentSong = _currentTracklist.getOrNull(currentIdx)
                    val songsToShuffle = _currentTracklist.filterIndexed { idx, _ -> idx != currentIdx }
                    val shuffled = songsToShuffle.shuffled()

                    _currentTracklist.clear()
                    currentSong?.let { _currentTracklist.add(it) }
                    _currentTracklist.addAll(shuffled)
                    Pair(true, _currentTracklist.toList())
                }
            }

            if (shouldShuffle) {
                // Rebuild the player queue atomically
                withContext(Dispatchers.Main) {
                    try {
                        val currentPosition = mediaController.currentPosition
                        mediaController.setMediaItems(newQueue, 0, currentPosition)
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