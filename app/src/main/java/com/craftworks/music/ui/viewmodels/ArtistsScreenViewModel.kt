package com.craftworks.music.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.craftworks.music.data.model.AlbumArtistListSort
import com.craftworks.music.data.model.LibraryType
import com.craftworks.music.data.model.MediaModel
import com.craftworks.music.data.model.MediaQuery
import com.craftworks.music.data.model.SortOrder
import com.craftworks.music.data.repository.AlbumRepository
import com.craftworks.music.data.repository.ArtistRepository
import com.craftworks.music.data.repository.SongRepository
import com.craftworks.music.data.repository.StarredRepository
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.LocalDataSettingsManager
import com.craftworks.music.managers.settings.MiscSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistsScreenViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val songRepository: SongRepository,
    private val starredRepository: StarredRepository,
    private val localDataSettingsManager: LocalDataSettingsManager,
    private val miscSettingsManager: MiscSettingsManager,
    appearanceSettingsManager: AppearanceSettingsManager
) : ViewModel() {
    private val _allArtists = MutableStateFlow<List<MediaModel.Artist>>(emptyList())
    val allArtists: StateFlow<List<MediaModel.Artist>> = _allArtists.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaModel.Artist>>(emptyList())
    val searchResults: StateFlow<List<MediaModel.Artist>> = _searchResults.asStateFlow()

    private val _selectedArtist = MutableStateFlow<MediaModel.Artist?>(null)
    val selectedArtist: StateFlow<MediaModel.Artist?> = _selectedArtist

    private val _artistAlbums = MutableStateFlow<List<MediaItem>>(emptyList())
    val artistAlbums: StateFlow<List<MediaItem>> = _artistAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _sort = MutableStateFlow(AlbumArtistListSort.NAME)
    val sort: StateFlow<AlbumArtistListSort> = _sort.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    val actionButtons = appearanceSettingsManager.artistDetailsButtons

    init {
        viewModelScope.launch {
            combine(
                localDataSettingsManager.sortArtist,
                localDataSettingsManager.sortArtistOrder,
                localDataSettingsManager.showFavoriteArtist
            ) { sort, sortOrder, showFavorites -> Triple(sort, sortOrder, showFavorites) }
                .distinctUntilChanged()
                .collect { (sort, sortOrder, showFavorites) ->
                    _sort.value = sort
                    _sortOrder.value = sortOrder
                    _showFavoritesOnly.value = showFavorites
                    getArtists()
                }
        }

        viewModelScope.launch {
            DataRefreshManager.dataSourceChangedEvent.collect {
                getArtists()
            }
        }
    }

    private var getArtistsJob: Job? = null
    fun getArtists() {
        getArtistsJob?.cancel()

        getArtistsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _allArtists.value = artistRepository.getArtists(
                    MediaQuery.AlbumArtistListQuery(
                        sortBy = _sort.value,
                        sortOrder = _sortOrder.value,
                        startIndex = 0,
                        favorite = if (_showFavoritesOnly.value) true else null
                    )
                )
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun getMoreArtists() {
        if (_isLoading.value || getArtistsJob?.isActive == true) return

        getArtistsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _allArtists.value += artistRepository.getArtists(
                    MediaQuery.AlbumArtistListQuery(
                        sortBy = _sort.value,
                        sortOrder = _sortOrder.value,
                        startIndex = _allArtists.value.size,
                        limit = 50,
                        favorite = if (_showFavoritesOnly.value) true else null
                    )
                )
            }
            finally {
                _isLoading.value = false
            }
        }
    }


    suspend fun getAlbum(id: String): List<MediaItem> {
        return albumRepository.getAlbum(id) ?: emptyList()
    }

    private var searchJob: Job? = null
    fun search(query: String) {
        if (query.isBlank())
            return

        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _searchResults.value = artistRepository.getArtists(
                    MediaQuery.AlbumArtistListQuery(
                        sortBy = _sort.value,
                        sortOrder = _sortOrder.value,
                        favorite = if (_showFavoritesOnly.value) true else null,
                        startIndex = 0,
                        searchTerm = query
                    )
                )
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun loadArtistDetails(artistId: String) {
        _selectedArtist.value = _allArtists.value.firstOrNull { it.id == artistId }
        _artistAlbums.value = emptyList()

        viewModelScope.launch {
            val loadingJob = launch {
                delay(1000)
                if (_artistAlbums.value.isEmpty()) {
                    _isLoading.value = true
                }
            }
            loadingJob.start()
            try {
                val infoDeferred = async { artistRepository.getArtistInfo(artistId) }

                val artistDetail = artistRepository.getArtistDetail(artistId)

                _selectedArtist.value = artistDetail?.artist
                if (artistDetail?.albums.isNullOrEmpty()) {
                    val artistAlbumsAsync = async { artistRepository.getArtistAlbums(artistId) }
                    _artistAlbums.value = artistAlbumsAsync.await()
                } else {
                    _artistAlbums.value = artistDetail.albums.map { it.toMediaItem() }
                }

                val artistInfo = infoDeferred.await()
                _selectedArtist.value = _selectedArtist.value?.copy(
                    biography = artistInfo?.biography,
                    similarArtists = artistInfo?.similarArtists ?: emptyList()
                )
            }
            finally {
                loadingJob.cancel()
                _isLoading.value = false
            }
        }
    }
    fun setSorting(newSort: AlbumArtistListSort) {
        viewModelScope.launch {
            localDataSettingsManager.saveSortArtist(newSort)
        }
    }
    fun setOrder(newSortOrder: SortOrder) {
        viewModelScope.launch {
            localDataSettingsManager.saveSortArtistOrder(newSortOrder)
        }
    }
    fun setShowFavoritesOnly(showFavorites: Boolean) {
        viewModelScope.launch {
            localDataSettingsManager.saveShowFavoriteArtist(showFavorites)
        }
    }
    fun starArtist(id: String) {
        viewModelScope.launch {
            starredRepository.starItem(listOf(id), LibraryType.ARTIST)
        }
    }
    fun unstarArtist(id: String) {
        viewModelScope.launch {
            starredRepository.unStarItem(listOf(id), LibraryType.ARTIST)
        }
    }
    fun downloadArtist(songs: List<MediaItem>) {
        viewModelScope.launch {
            val template = miscSettingsManager.downloadTemplateFlow.first()
            songs.forEach { song ->
                songRepository.downloadSong(song.mediaMetadata, template)
            }
        }
    }
}