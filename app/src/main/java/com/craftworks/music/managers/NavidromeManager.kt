package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.localProviderList
import com.craftworks.music.providers.navidrome.navidromeStatus
import com.craftworks.music.showNoProviderDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object NavidromeManager {
    private val servers = mutableMapOf<String, NavidromeProvider>()
    private var currentServerId: String? = null

    private val _serverStatus = MutableStateFlow("")
    val serverStatus: StateFlow<String> = _serverStatus.asStateFlow()

    private val _syncStatus = MutableStateFlow(false)
    val syncStatus: StateFlow<Boolean> = _syncStatus.asStateFlow()

    fun addServer(server: NavidromeProvider) {
        Log.d("NAVIDROME", "Added server $server")
        servers[server.id] = server
        // Set newly added server as current
        if (currentServerId == null) {
            currentServerId = server.id
        }
        saveServers()
    }

    fun removeServer(id: String) {
        servers.remove(id)
        // If we remove the current server, set the active one to be the first or null.
        if (currentServerId == id) {
            currentServerId = servers.keys.firstOrNull()
        }
        saveServers()
    }

    fun setCurrentServer(id: String) {
        if (id in servers) {
            currentServerId = id
        } else {
            throw IllegalArgumentException("Server with id $id not found")
        }
        saveServers()
    }

    fun checkActiveServers(): Boolean {
        return servers.keys.isNotEmpty() || currentServerId != null
    }

    fun getAllServers(): List<NavidromeProvider> = servers.values.toList()
    fun getCurrentServer(): NavidromeProvider? = currentServerId?.let { servers[it] }

    fun getServerStatus(): String = navidromeStatus.value
    fun setServerStatus(status: String) { _serverStatus.value = status }

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

        if (getAllServers().isEmpty() && localProviderList.isEmpty()) showNoProviderDialog.value = true

        setSyncingStatus(false)
    }

    private fun saveServers() {
        val serversJson = json.encodeToString(servers as Map<String, NavidromeProvider>)
        sharedPreferences.edit().putString(PREF_SERVERS, serversJson).apply()
        sharedPreferences.edit().putString(PREF_CURRENT_SERVER, currentServerId).apply()
    }

    private fun loadServers() {
        currentServerId = sharedPreferences.getString(PREF_CURRENT_SERVER, null)
        val serversJson = sharedPreferences.getString(PREF_SERVERS, null)
        if (serversJson != null) {
            val loadedServers: Map<String, NavidromeProvider> = json.decodeFromString(serversJson)
            servers.putAll(loadedServers)
        }
    }
}