package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.craftworks.music.data.NavidromeLibrary
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.LocalProviderManager.getAllFolders
import com.craftworks.music.showNoProviderDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

object NavidromeManager {
    private val servers = mutableMapOf<String, NavidromeProvider>()

    private var _currentServerId = MutableStateFlow<String?>(null)
    val currentServerId: StateFlow<String?> = _currentServerId.asStateFlow()

    private val _allServers = MutableStateFlow<List<NavidromeProvider>>(emptyList())
    val allServers: StateFlow<List<NavidromeProvider>> = _allServers.asStateFlow()

    private var _libraries = MutableStateFlow<List<Pair<NavidromeLibrary, Boolean>>>(emptyList())
    val libraries: StateFlow<List<Pair<NavidromeLibrary, Boolean>>> =
        _libraries
            .stateIn(
                CoroutineScope(Dispatchers.Main.immediate),
                SharingStarted.Eagerly,
                emptyList()
            )

    private val _syncStatus = MutableStateFlow(false)

    fun addServer(server: NavidromeProvider) {
        Log.d("NAVIDROME", "Added server $server")
        servers[server.id] = server
        // Set newly added server as current
        if (_currentServerId.value == null) {
            _currentServerId.value = server.id
        }
        // Not really ideal.
        val oldServerId = _currentServerId.value
        _currentServerId.value = server.id
        val fetchedLibraries = runBlocking {
            NavidromeDataSource().getNavidromeLibraries().map {
                Pair(it, true)
            }
        }
        _currentServerId.value = oldServerId
        setServerLibraries(server.id, fetchedLibraries)
        if (server.id == _currentServerId.value) {
            _libraries.value = fetchedLibraries
        }
        updateServersFlow()
        saveServers()
    }

    fun setServerLibraries(serverId: String, libraries: List<Pair<NavidromeLibrary, Boolean>>) {
        servers[serverId]?.libraryIds = libraries
        if (serverId == _currentServerId.value) {
            _libraries.value = libraries
        }
        saveServers()
    }

    fun toggleServerLibraryEnabled(serverId: String, libraryId: Int, isEnabled: Boolean) {
        servers[serverId]?.let { server ->
            val updatedLibraries = server.libraryIds.map { (library, currentEnabled) ->
                if (library.id == libraryId) {
                    Pair(library, isEnabled)
                } else {
                    Pair(library, currentEnabled)
                }
            }
            server.libraryIds = updatedLibraries
            if (serverId == _currentServerId.value) {
                if (_libraries.value != updatedLibraries) {
                    _libraries.value = updatedLibraries
                }
            }
            saveServers()
        }
    }

    fun removeServer(id: String) {
        servers.remove(id)
        if (_currentServerId.value == id) {
            _currentServerId.value = servers.keys.firstOrNull()
            _libraries.value = _currentServerId.value?.let { servers[it]?.libraryIds } ?: emptyList()
        }
        updateServersFlow()
        saveServers()
    }

    fun checkActiveServers(): Boolean {
        return servers.keys.isNotEmpty() && _currentServerId.value != null
    }

    fun getAllServers(): List<NavidromeProvider> = servers.values.toList()
    fun getCurrentServer(): NavidromeProvider? = _currentServerId.value?.let { servers[it] }

    fun setCurrentServer(serverId: String?) {
        _currentServerId.value = serverId
        _libraries.value = serverId?.let { servers[it]?.libraryIds } ?: emptyList()
        saveServers()
    }

    private fun updateServersFlow() {
        _allServers.value = servers.values.toList()
    }

    fun setSyncingStatus(status: Boolean) { _syncStatus.value = status }

    // Save and load navidrome servers.
    private lateinit var sharedPreferences: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_SERVERS = "navidrome_servers"
    private const val PREF_CURRENT_SERVER = "current_server_id"

    fun init(context: Context) {
        setSyncingStatus(true)
        sharedPreferences = context.getSharedPreferences("NavidromePrefs", Context.MODE_PRIVATE)
        loadServers()

        if (getAllServers().isEmpty() && getAllFolders().isEmpty()) showNoProviderDialog.value = true

        setSyncingStatus(false)
    }

    private fun saveServers() {
        DataRefreshManager.notifyDataSourcesChanged()
        val serversJson = json.encodeToString(servers as Map<String, NavidromeProvider>)
        sharedPreferences.edit { putString(PREF_SERVERS, serversJson) }
        sharedPreferences.edit { putString(PREF_CURRENT_SERVER, _currentServerId.value) }
    }

    private fun loadServers() {
        _currentServerId.value = sharedPreferences.getString(PREF_CURRENT_SERVER, null)
        val serversJson = sharedPreferences.getString(PREF_SERVERS, null)
        if (serversJson != null) {
            val loadedServers: Map<String, NavidromeProvider> = json.decodeFromString(serversJson)
            servers.putAll(loadedServers)
        }
        _libraries.value = _currentServerId.value?.let { servers[it]?.libraryIds } ?: emptyList()
        updateServersFlow()
    }

    fun getEnabledLibraryIdsForCurrentServer(): List<Int> {
        return _currentServerId.value?.let { serverId ->
            servers[serverId]?.libraryIds
                ?.filter { it.second }
                ?.map { it.first.id }
        } ?: emptyList()
    }
}