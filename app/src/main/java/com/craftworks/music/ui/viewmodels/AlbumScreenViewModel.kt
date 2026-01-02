package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.database.dao.AlbumDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.database.entity.toMediaDataAlbum
import com.craftworks.music.data.database.entity.toMediaDataSong
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.model.toMediaItem
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.SyncRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import com.craftworks.music.ui.util.TextDisplayUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumScreenViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val localDataSettingsManager: LocalDataSettingsManager,
    private val syncRepository: SyncRepository,
    private val albumDao: AlbumDao,
    private val songDao: SongDao
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.ALPHABETICAL)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Observe Room database directly for instant UI updates, combined with sort order
    val allAlbums: StateFlow<List<MediaItem>> = combine(
        albumDao.getAllAlbums().map { entities -> entities.map { it.toMediaDataAlbum().toMediaItem() } },
        _sortOrder
    ) { albums, order ->
        when (order) {
            SortOrder.ALPHABETICAL -> albums.sortedBy {
                TextDisplayUtils.getSortKey(it.mediaMetadata.title?.toString())
            }
            SortOrder.NEWEST -> albums.sortedByDescending {
                it.mediaMetadata.extras?.getString("created") ?: ""
            }
            SortOrder.RECENT -> albums.sortedByDescending {
                it.mediaMetadata.extras?.getString("lastPlayed") ?: ""
            }
            SortOrder.FREQUENT -> albums.sortedByDescending {
                it.mediaMetadata.extras?.getInt("playCount") ?: 0
            }
            SortOrder.STARRED -> {
                val (starred, unstarred) = albums.partition {
                    !it.mediaMetadata.extras?.getString("starred").isNullOrEmpty()
                }
                starred.sortedBy { TextDisplayUtils.getSortKey(it.mediaMetadata.title?.toString()) } +
                unstarred.sortedBy { TextDisplayUtils.getSortKey(it.mediaMetadata.title?.toString()) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    val isLoading: StateFlow<Boolean> = syncRepository.isSyncing

    init {
        viewModelScope.launch {
            try {
                localDataSettingsManager.sortAlbumOrder.collect { sortOrder ->
                    if (_sortOrder.value != sortOrder) {
                        _sortOrder.value = sortOrder
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                _sortOrder.value = localDataSettingsManager.sortAlbumOrder.first()

                // Skip auto-sync if cache was just cleared
                if (syncRepository.wasJustCleared()) {
                    return@launch
                }

                // Load cached data instantly, sync in background (once per day)
                if (!syncRepository.hasCachedData()) {
                    syncRepository.syncAll()
                } else if (syncRepository.shouldSyncToday()) {
                    launch { syncRepository.syncAll() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                DataRefreshManager.dataSourceChangedEvent.collect {
                    // Skip refresh if just cleared
                    if (!syncRepository.wasJustCleared()) {
                        // Don't auto-refresh - let user trigger manually
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshAlbums() {
        viewModelScope.launch {
            try {
                syncRepository.syncAll(forceRefresh = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

    suspend fun getSongsForAlbums(albums: List<MediaItem>): List<MediaItem> {
        // Use batch query to avoid N+1 problem
        val albumIds = albums.map { it.mediaId }
        val localIds = albumIds.filter { it.startsWith("Local_") }
        val navidromeIds = albumIds.filter { !it.startsWith("Local_") }

        val songs = mutableListOf<MediaItem>()

        // Get cached songs from Room for Navidrome albums
        if (navidromeIds.isNotEmpty()) {
            val cachedSongs = songDao.getSongsByAlbumIds(navidromeIds)
            songs.addAll(cachedSongs.map { it.toMediaDataSong().toMediaItem() })
        }

        // Fallback to repository for local albums
        for (localId in localIds) {
            val albumSongs = albumRepository.getAlbum(localId) ?: emptyList()
            songs.addAll(albumSongs.drop(1)) // Drop album header
        }

        return songs
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _searchResults.value = albumRepository.searchAlbum(query)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSorting(newSortOrder: SortOrder) {
        viewModelScope.launch {
            try {
                localDataSettingsManager.saveSortAlbumOrder(newSortOrder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}