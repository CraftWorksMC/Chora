package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.craftworks.music.managers.NavidromeManager.getAllServers
import com.craftworks.music.showNoProviderDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.io.File

object LocalProviderManager {
    private val _allFolders = MutableStateFlow<List<String>>(emptyList())
    val allFolders: StateFlow<List<String>> = _allFolders.asStateFlow()

    // Thread-safe lock for folder modifications
    private val folderLock = Any()

    /**
     * Validates a folder path to prevent path traversal attacks.
     * Returns sanitized path or null if invalid.
     */
    private fun validateAndSanitizePath(folder: String): String? {
        if (folder.isBlank()) return null

        // Reject obvious path traversal attempts
        if (folder.contains("..") || folder.contains("./")) {
            Log.w("LOCAL_PROVIDER", "Rejected path with traversal patterns: $folder")
            return null
        }

        // Normalize the path and check it's absolute
        val file = File(folder)
        val canonicalPath = try {
            file.canonicalPath
        } catch (e: Exception) {
            Log.w("LOCAL_PROVIDER", "Failed to canonicalize path: $folder", e)
            return null
        }

        // Ensure the canonical path doesn't escape to sensitive directories
        val blockedPaths = listOf("/system", "/data/data", "/data/user")
        if (blockedPaths.any { canonicalPath.startsWith(it) }) {
            Log.w("LOCAL_PROVIDER", "Rejected blocked path: $canonicalPath")
            return null
        }

        return canonicalPath
    }

    fun addFolder(folder: String): Boolean {
        val sanitizedPath = validateAndSanitizePath(folder)
        if (sanitizedPath == null) {
            Log.e("LOCAL_PROVIDER", "Invalid folder path rejected: $folder")
            return false
        }

        synchronized(folderLock) {
            // Avoid duplicates
            if (_allFolders.value.contains(sanitizedPath)) {
                Log.d("LOCAL_PROVIDER", "Folder already exists: $sanitizedPath")
                return false
            }

            Log.d("LOCAL_PROVIDER", "Added folder: $sanitizedPath")
            _allFolders.update { currentList -> currentList + sanitizedPath }
            saveFolders()
        }
        return true
    }

    fun removeFolder(folder: String) {
        synchronized(folderLock) {
            _allFolders.update { currentList -> currentList - folder }
            saveFolders()
        }
    }

    fun checkActiveFolders(): Boolean {
        return _allFolders.value.isNotEmpty()
    }

    fun getAllFolders(): List<String> = _allFolders.value

    // Save and load local folders.
    private var sharedPreferences: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_FOLDERS = "local_folders"

    private val isInitialized: Boolean
        get() = sharedPreferences != null

    fun init(context: Context) {
        synchronized(folderLock) {
            if (sharedPreferences != null) return // Already initialized
            sharedPreferences = context.getSharedPreferences("LocalProviderPrefs", Context.MODE_PRIVATE)
            loadFolders()
        }

        if (getAllServers().isEmpty() && _allFolders.value.isEmpty()) showNoProviderDialog.value = true
    }

    private fun saveFolders() {
        val prefs = sharedPreferences
        if (prefs == null) {
            Log.e("LOCAL_PROVIDER", "saveFolders called before init()")
            return
        }

        DataRefreshManager.notifyDataSourcesChanged()
        val serversJson = json.encodeToString(_allFolders.value)
        prefs.edit { putString(PREF_FOLDERS, serversJson) }
    }

    private fun loadFolders() {
        val prefs = sharedPreferences ?: return

        val foldersJson = prefs.getString(PREF_FOLDERS, null)
        if (foldersJson != null) {
            try {
                val loadedFolders: List<String> = json.decodeFromString(foldersJson)
                // Re-validate loaded folders in case storage was tampered with
                val validFolders = loadedFolders.mapNotNull { validateAndSanitizePath(it) }
                _allFolders.value = validFolders
            } catch (e: Exception) {
                Log.e("LOCAL_PROVIDER", "Failed to load folders from preferences", e)
                _allFolders.value = emptyList()
            }
        }
    }
}