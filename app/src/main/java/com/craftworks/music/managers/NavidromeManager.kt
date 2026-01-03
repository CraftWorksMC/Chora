package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.craftworks.music.data.NavidromeLibrary
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.datasource.navidrome.NavidromeDataSource
import com.craftworks.music.managers.LocalProviderManager.getAllFolders
import com.craftworks.music.showNoProviderDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object NavidromeManager {
    private val servers = java.util.concurrent.ConcurrentHashMap<String, NavidromeProvider>()
    @Volatile
    private var appContext: Context? = null

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException("NavidromeManager.init() must be called before use")
    }

    // Managed scope for background operations - uses SupervisorJob so child failures don't cancel sibling jobs
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        Log.e("NAVIDROME", "Coroutine exception in NavidromeManager", throwable)
    })

    private var _currentServerId = MutableStateFlow<String?>(null)
    val currentServerId: StateFlow<String?> = _currentServerId.asStateFlow()

    private val _allServers = MutableStateFlow<List<NavidromeProvider>>(emptyList())
    val allServers: StateFlow<List<NavidromeProvider>> = _allServers.asStateFlow()

    private var _libraries = MutableStateFlow<List<Pair<NavidromeLibrary, Boolean>>>(emptyList())
    val libraries: StateFlow<List<Pair<NavidromeLibrary, Boolean>>> =
        _libraries
            .stateIn(
                CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()),
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
        updateServersFlow()
        saveServers()

        // Hide "no provider" dialog since we now have a provider
        showNoProviderDialog.value = false

        // Fetch libraries asynchronously instead of blocking
        val oldServerId = _currentServerId.value
        _currentServerId.value = server.id

        managerScope.launch {
            try {
                val fetchedLibraries = NavidromeDataSource(requireContext()).getNavidromeLibraries().map {
                    Pair(it, true)
                }
                _currentServerId.value = oldServerId
                setServerLibraries(server.id, fetchedLibraries)
                if (server.id == _currentServerId.value) {
                    _libraries.value = fetchedLibraries
                }
            } catch (e: Exception) {
                Log.e("NAVIDROME", "Failed to fetch libraries for server", e)
                _currentServerId.value = oldServerId
            }
        }
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
    @Volatile
    private var sharedPreferences: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_SERVERS = "navidrome_servers"
    private const val PREF_CURRENT_SERVER = "current_server_id"

    private fun requirePrefs(): SharedPreferences {
        return sharedPreferences ?: throw IllegalStateException("NavidromeManager.init() must be called before use")
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        setSyncingStatus(true)

        try {
            // Create MasterKey for encryption
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // Create EncryptedSharedPreferences
            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "NavidromePrefsEncrypted",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // EncryptedSharedPreferences can fail if keys are corrupted (e.g., after backup restore)
            // Clear the corrupted file and create fresh encrypted prefs
            Log.e("NAVIDROME", "Failed to create EncryptedSharedPreferences, resetting", e)
            try {
                // Delete the corrupted encrypted prefs file
                context.getSharedPreferences("NavidromePrefsEncrypted", Context.MODE_PRIVATE)
                    .edit { clear() }
                val encryptedPrefsFile = java.io.File(context.filesDir.parent, "shared_prefs/NavidromePrefsEncrypted.xml")
                if (encryptedPrefsFile.exists()) {
                    encryptedPrefsFile.delete()
                }

                // Retry creating encrypted prefs
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    "NavidromePrefsEncrypted",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                // Last resort: fall back to regular SharedPreferences
                Log.e("NAVIDROME", "Failed to reset EncryptedSharedPreferences, falling back to regular prefs", e2)
                sharedPreferences = context.getSharedPreferences("NavidromePrefs", Context.MODE_PRIVATE)
            }
        }

        // Migrate from old unencrypted prefs if needed
        migrateFromUnencryptedPrefs(context)

        loadServers()

        if (getAllServers().isEmpty() && getAllFolders().isEmpty()) showNoProviderDialog.value = true

        setSyncingStatus(false)
    }

    private fun migrateFromUnencryptedPrefs(context: Context) {
        val oldPrefs = context.getSharedPreferences("NavidromePrefs", Context.MODE_PRIVATE)
        val oldServersJson = oldPrefs.getString(PREF_SERVERS, null)
        val oldCurrentServer = oldPrefs.getString(PREF_CURRENT_SERVER, null)

        // If old prefs have data and new prefs are empty, migrate
        val prefs = requirePrefs()
        if (oldServersJson != null && prefs.getString(PREF_SERVERS, null) == null) {
            Log.d("NAVIDROME", "Migrating server data to encrypted storage")
            prefs.edit {
                putString(PREF_SERVERS, oldServersJson)
                putString(PREF_CURRENT_SERVER, oldCurrentServer)
            }
            // Clear old unencrypted data after successful migration
            oldPrefs.edit { clear() }
        }
    }

    private fun saveServers() {
        DataRefreshManager.notifyDataSourcesChanged()
        val serversJson = json.encodeToString(servers as Map<String, NavidromeProvider>)
        // Use single edit call for atomicity
        requirePrefs().edit {
            putString(PREF_SERVERS, serversJson)
            putString(PREF_CURRENT_SERVER, _currentServerId.value)
        }
    }

    private fun loadServers() {
        val prefs = requirePrefs()
        _currentServerId.value = prefs.getString(PREF_CURRENT_SERVER, null)
        val serversJson = prefs.getString(PREF_SERVERS, null)
        if (serversJson != null) {
            try {
                val loadedServers: Map<String, NavidromeProvider> = json.decodeFromString(serversJson)
                servers.putAll(loadedServers)
            } catch (e: Exception) {
                Log.e("NAVIDROME", "Failed to deserialize servers JSON, clearing corrupted data", e)
                prefs.edit { remove(PREF_SERVERS) }
            }
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