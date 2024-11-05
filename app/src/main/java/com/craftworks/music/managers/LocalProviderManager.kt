package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.craftworks.music.managers.NavidromeManager.getAllServers
import com.craftworks.music.providers.local.LocalProvider
import com.craftworks.music.showNoProviderDialog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object LocalProviderManager {
    private val folders = mutableListOf<String>("/Music/")

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

    fun getAllFolders(): List<String> = folders

    // Save and load local folders.
    private lateinit var sharedPreferences: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_FOLDERS = "local_folders"

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("LocalProviderPrefs", Context.MODE_PRIVATE)
        loadFolders()

        LocalProvider.getInstance().init(context)

        if (getAllServers().isEmpty() && getAllFolders().isEmpty()) showNoProviderDialog.value = true
    }

    private fun saveFolders() {
        val serversJson = json.encodeToString(folders as List<String>)
        sharedPreferences.edit().putString(PREF_FOLDERS, serversJson).apply()
    }

    private fun loadFolders() {
        val foldersJson = sharedPreferences.getString(PREF_FOLDERS, null)
        if (foldersJson != null) {
            val loadedServers: List<String> = json.decodeFromString(foldersJson)
            folders.addAll(loadedServers)
        }
    }
}