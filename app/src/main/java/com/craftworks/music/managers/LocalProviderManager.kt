package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

// LEGACY CODE! MUST NOT BE USED
// TODO("Delete legacy file")
object LocalProviderManager {
    private val _allFolders = MutableStateFlow<List<String>>(emptyList())
    val allFolders: StateFlow<List<String>> = _allFolders.asStateFlow()

    fun addFolder(folder: String) {
        Log.d("NAVIDROME", "Added server $folder")
        if (_allFolders.value.contains(folder)) return
        _allFolders.value += folder
        saveFolders()
    }

    fun removeFolder(folder: String) {
        _allFolders.value -= folder
        saveFolders()
    }

    fun checkActiveFolders(): Boolean {
        return _allFolders.value.isNotEmpty()
    }

    fun getAllFolders(): List<String> = _allFolders.value

    // Save and load local folders.
    private lateinit var sharedPreferences: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_FOLDERS = "local_folders"

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("LocalProviderPrefs", Context.MODE_PRIVATE)
        loadFolders()
    }

    private fun saveFolders() {
        DataRefreshManager.notifyDataSourcesChanged()
        val serversJson = json.encodeToString(_allFolders.value)
        sharedPreferences.edit { putString(PREF_FOLDERS, serversJson) }
    }

    private fun loadFolders() {
        val foldersJson = sharedPreferences.getString(PREF_FOLDERS, null)
        if (foldersJson != null) {
            val loadedServers: List<String> = json.decodeFromString(foldersJson)
            _allFolders.value = loadedServers.distinct()
        }
    }
}