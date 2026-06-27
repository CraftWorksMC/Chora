package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.craftworks.music.providers.MediaProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

object MediaProviderManager {
    private val providers = mutableMapOf<String, MediaProvider>()

    private var _currentProvider = MutableStateFlow<MediaProvider?>(null)
    val currentProvider: StateFlow<MediaProvider?> = _currentProvider.asStateFlow()

    private var currentProviderId: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private const val PREF_PROVIDERS = "providers"
    private const val PREF_CURRENT_PROVIDER = "current_provider_id"

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("MediaProvidersPrefs", Context.MODE_PRIVATE)
        loadProviders(context)
    }

    private fun saveProviders() {
        DataRefreshManager.notifyDataSourcesChanged()
        val providersJson = json.encodeToString(providers as Map<String, MediaProvider>)
        sharedPreferences.edit { putString(PREF_PROVIDERS, providersJson) }
        sharedPreferences.edit { putString(PREF_CURRENT_PROVIDER, currentProviderId) }
    }

    private fun loadProviders(context: Context) {
        currentProviderId = sharedPreferences.getString(PREF_CURRENT_PROVIDER, null)
        val providersJson = sharedPreferences.getString(PREF_PROVIDERS, null)
        if (providersJson != null) {
            val loadedProviders: Map<String, MediaProvider> = json.decodeFromString(providersJson)
            providers.putAll(loadedProviders)
            for (provider in providers) {
                provider.value.init(context)
            }
        }
        if (currentProviderId != null) _currentProvider.value = providers[currentProviderId]
    }
}