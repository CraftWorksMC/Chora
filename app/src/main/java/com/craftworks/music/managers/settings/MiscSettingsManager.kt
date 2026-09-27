package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiscSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DOWNLOAD_TEMPLATE_KEY = stringPreferencesKey("download_template")
        private val PLAYLIST_DOWNLOAD_TEMPLATE_KEY = stringPreferencesKey("playlist_download_template")
    }

    val downloadTemplateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DOWNLOAD_TEMPLATE_KEY] ?: "{album}/{title} - {artist}.{ext}"
    }

    suspend fun setDownloadTemplate(downloadTemplate: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[DOWNLOAD_TEMPLATE_KEY] = downloadTemplate
            }
        }
    }
    val playlistDownloadTemplateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PLAYLIST_DOWNLOAD_TEMPLATE_KEY] ?: "{playlist}/{playlist_index}. {title} - {artist}.{ext}"
    }

    suspend fun setPlaylistDownloadTemplate(downloadTemplate: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[PLAYLIST_DOWNLOAD_TEMPLATE_KEY] = downloadTemplate
            }
        }
    }
}