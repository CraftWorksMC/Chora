package com.craftworks.music.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.craftworks.music.data.model.MediaProviderData
import com.craftworks.music.data.model.MusicFolder
import com.craftworks.music.providers.MediaProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.util.UUID

object MediaProviderManager {
    private val providers = mutableMapOf<String, MediaProvider>()

    private var _currentProvider = MutableStateFlow<MediaProvider?>(null)
    val currentProvider: StateFlow<MediaProvider?> = _currentProvider.asStateFlow()

    private val _allProviders = MutableStateFlow<List<MediaProvider>>(emptyList())
    val allProviders: StateFlow<List<MediaProvider>> = _allProviders.asStateFlow()
    private var currentProviderId: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = MediaProvider.serializerModule
    }
    private const val PREF_PROVIDERS = "providers"
    private const val PREF_CURRENT_PROVIDER = "current_provider_id"

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("MediaProvidersPrefs", Context.MODE_PRIVATE)
        loadProviders(context)
        Log.d(
            "PM",
            "Providers loaded: " + providers.map {it.key + ": " + it.value.javaClass.simpleName}
        )
    }

    fun getProvider(providerId: String) = providers[providerId]

    suspend fun addProvider(mediaProvider: MediaProvider) {
        val id = UUID.randomUUID().toString()
        providers[id] = mediaProvider
        currentProviderId = id

        mediaProvider.id = id
        mediaProvider.data = MediaProviderData(mediaProvider.getMusicFolderList().map { Pair(it, true) })

        updateProvidersFlow()
        saveProviders()
    }
    fun removeProvider(id: String) {
        providers.remove(id)
        if (currentProviderId == id) {
            currentProviderId = providers.keys.firstOrNull()
        }

        updateProvidersFlow()
        saveProviders()

    }
    fun setProviderLibraries(providerId: String, libraries: List<Pair<MusicFolder, Boolean>>) {
        providers[providerId]?.data?.libraries = libraries
        saveProviders()
    }
    fun checkActiveProvider(): Boolean {
        return providers.isNotEmpty() && currentProviderId != null
    }
    private fun updateProvidersFlow() {
        _allProviders.value = providers.map { it.value }
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
                Log.d(
                    "PM",
                    "Init Provider: " + provider.value.javaClass.simpleName
                )
                provider.value.init(context)
                provider.value.id = provider.key
            }
        }
        if (currentProviderId != null) _currentProvider.value = providers[currentProviderId]
        updateProvidersFlow()
    }
}