package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.database.entity.toMediaDataSong
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.data.repository.SyncRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.ui.util.TextDisplayUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val syncRepository: SyncRepository,
    private val songDao: SongDao
) : ViewModel() {

    // Observe Room database directly for instant UI updates
    // Sort using TextDisplayUtils.getSortKey to handle leading quotes/punctuation properly
    val allSongs: StateFlow<List<MediaItem>> = songDao.getAllSongs()
        .map { entities ->
            entities.map { it.toMediaDataSong().toMediaItem() }
                .sortedBy { TextDisplayUtils.getSortKey(it.mediaMetadata.title?.toString()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    val isLoading: StateFlow<Boolean> = syncRepository.isSyncing

    init {
        // Load cached data instantly, then sync in background (once per day)
        viewModelScope.launch {
            try {
                // Skip auto-sync if cache was just cleared (user wants empty state)
                if (syncRepository.wasJustCleared()) {
                    return@launch
                }

                if (!syncRepository.hasCachedData()) {
                    // First launch - show loading and sync
                    syncRepository.syncAll()
                } else if (syncRepository.shouldSyncToday()) {
                    // Has cached data but hasn't synced today - sync in background
                    launch { syncRepository.syncAll() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Don't auto-refresh on data source change event after clear
        viewModelScope.launch {
            try {
                DataRefreshManager.dataSourceChangedEvent.collect {
                    // Skip if just cleared
                    if (!syncRepository.wasJustCleared()) {
                        // Don't refresh here - let user trigger manually
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshSongs() {
        viewModelScope.launch {
            try {
                syncRepository.syncAll(forceRefresh = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _searchResults.value = songRepository.searchSongs(query)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getRandomSongs(size: Int = 50): List<MediaItem> {
        // Try from cache first, fallback to network
        val cachedSongs = songDao.getAllSongsOnce()
        return if (cachedSongs.isNotEmpty()) {
            cachedSongs.shuffled().take(size).map { it.toMediaDataSong().toMediaItem() }
        } else {
            songRepository.getRandomSongs(size)
        }
    }
}