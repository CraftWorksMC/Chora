package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.craftworks.music.managers.NavidromeManager.getAllServers
import com.craftworks.music.showNoProviderDialog
import kotlinx.serialization.json.Json

object LocalProviderManager {
    private val folders = mutableListOf<String>()

    fun addFolder(folder: String) {
        Log.d("NAVIDROME", "Added server $folder")
        folders.add(folder)
        saveFolders()
    }

    fun removeFolder(folder: String) {
        folders.remove(folder)
        saveFolders()
    }

    fun checkActiveFolders(): Boolean {
        return folders.isNotEmpty()
    }

    fun getAllFolders(): List<String> = folders.toList()

    // Save and load local folders.
    private lateinit var sharedPreferences: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_FOLDERS = "local_folders"

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("LocalProviderPrefs", Context.MODE_PRIVATE)
        loadFolders()

        if (getAllServers().isEmpty() && folders.isEmpty()) showNoProviderDialog.value = true
    }

    private fun saveFolders() {
        DataRefreshManager.notifyDataSourcesChanged()
        val serversJson = json.encodeToString(folders as List<String>)
        sharedPreferences.edit { putString(PREF_FOLDERS, serversJson) }
    }

    private fun loadFolders() {
        val foldersJson = sharedPreferences.getString(PREF_FOLDERS, null)
        if (foldersJson != null) {
            val loadedServers: List<String> = json.decodeFromString(foldersJson)
            folders.addAll(loadedServers)
        }
    }
}