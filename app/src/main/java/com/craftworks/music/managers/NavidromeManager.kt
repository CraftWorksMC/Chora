package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.managers.LocalProviderManager.getAllFolders
import com.craftworks.music.showNoProviderDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

object NavidromeManager {
    private val servers = mutableMapOf<String, NavidromeProvider>()

    private var _currentServerId = MutableStateFlow<String?>(null)
    val currentServerId: StateFlow<String?> = _currentServerId.asStateFlow()

    private val _allServers = MutableStateFlow<List<NavidromeProvider>>(emptyList())
    val allServers: StateFlow<List<NavidromeProvider>> = _allServers.asStateFlow()

    private val _syncStatus = MutableStateFlow(false)

    fun addServer(server: NavidromeProvider) {
        Log.d("NAVIDROME", "Added server $server")
        servers[server.id] = server
        // Set newly added server as current
        if (_currentServerId.value == null) {
            _currentServerId.value = server.id
        }
        updateServersFlow()
        saveServers()
    }

    fun removeServer(id: String) {
        servers.remove(id)
        // If we remove the current server, set the active one to be the first or null.
        if (_currentServerId.value == id) {
            _currentServerId.value = servers.keys.firstOrNull()
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
        updateServersFlow()
    }
}