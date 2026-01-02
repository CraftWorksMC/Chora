package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.database.dao.AlbumDao
import com.craftworks.music.data.database.dao.ArtistDao
import com.craftworks.music.data.database.dao.SongDao
import com.craftworks.music.data.repository.SyncRepository
import com.craftworks.music.data.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataSettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) : ViewModel() {

    val isSyncing: StateFlow<Boolean> = syncRepository.isSyncing

    val syncProgress: StateFlow<String> = syncRepository.syncProgress

    val syncState: StateFlow<SyncState> = syncRepository.syncState

    val isPaused: StateFlow<Boolean> = syncRepository.isPaused

    // Use getCountFlow() instead of getAllX().map { it.size } to avoid loading
    // all entities into memory just to count them - major memory optimization
    val songCount: StateFlow<Int> = songDao.getCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val albumCount: StateFlow<Int> = albumDao.getCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val artistCount: StateFlow<Int> = artistDao.getCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun cancelSync() {
        syncRepository.cancelSync()
    }

    fun pauseSync() {
        syncRepository.pauseSync()
    }

    fun resumeSync() {
        viewModelScope.launch {
            try {
                syncRepository.resumeSync()
                syncRepository.syncAll(resumeFromPause = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            try {
                syncRepository.syncAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun forceResync() {
        viewModelScope.launch {
            try {
                syncRepository.syncAll(forceRefresh = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            try {
                syncRepository.clearAllCache()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
