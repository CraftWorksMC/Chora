package com.craftworks.music.managers

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.craftworks.music.data.model.MediaProviderData
import com.craftworks.music.data.model.MusicFolder
import com.craftworks.music.providers.MediaProvider
import com.craftworks.music.utils.CryptoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.io.encoding.Base64

object MediaProviderManager {
    private val providers = mutableMapOf<String, MediaProvider>()

    private var _currentProvider = MutableStateFlow<MediaProvider?>(null)
    val currentProvider: StateFlow<MediaProvider?> = _currentProvider.asStateFlow()

    private val _allProviders = MutableStateFlow<List<MediaProvider>>(emptyList())
    val allProviders: StateFlow<List<MediaProvider>> = _allProviders.asStateFlow()

    private var currentProviderId: String? = null

    private lateinit var appContext: Context

    private val Context.providerDataStore: DataStore<MediaProviderConfig> by dataStore(
        fileName = "providers.pb",
        serializer = EncryptedMediaProviderSerializer
    )

    fun init(context: Context) {
        appContext = context

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

    fun setCurrentProvider(provider: MediaProvider) {
        currentProviderId = provider.id
        _currentProvider.value = provider

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
        providers[currentProviderId]?.let {setCurrentProvider(it) }
    }

    private fun saveProviders() {
        DataRefreshManager.notifyDataSourcesChanged()

        runBlocking {
            appContext.providerDataStore.updateData { currentConfig ->
                currentConfig.copy(
                    currentProviderId = currentProviderId,
                    providers = providers
                )
            }
        }
    }

    private fun loadProviders(context: Context) {
        runBlocking {
            val config = context.providerDataStore.data.first()
            currentProviderId = config.currentProviderId
            providers.putAll(config.providers)
            for (provider in providers) {
                Log.d(
                    "PM",
                    "Init Provider: " + provider.value.javaClass.simpleName
                )
                provider.value.init(context)
                provider.value.id = provider.key
            }
        }

        if (currentProviderId != null)
            _currentProvider.value = providers[currentProviderId]

        updateProvidersFlow()
    }

    suspend fun importProviders(context: Context, providers: Map<String, MediaProvider>, currentProviderId: String?) {
        context.providerDataStore.updateData { current ->
            current.copy(currentProviderId = currentProviderId, providers = providers)
        }
    }
}

//region Serialization

@Serializable
data class MediaProviderConfig(
    val currentProviderId: String? = null,
    val providers: Map<String, MediaProvider> = emptyMap()
)

object EncryptedMediaProviderSerializer : Serializer<MediaProviderConfig> {
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = MediaProvider.serializerModule
    }

    private val cryptoData = CryptoData()

    override val defaultValue: MediaProviderConfig
        get() = MediaProviderConfig()

    override suspend fun readFrom(input: InputStream): MediaProviderConfig {
        return try {
            val bytes = input.readBytes().decodeToString()
            if (bytes.isBlank()) return defaultValue

            val decryptedBytes = cryptoData.decrypt(Base64.decode(bytes))
                ?: return defaultValue

            json.decodeFromString<MediaProviderConfig>(decryptedBytes.decodeToString())
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: MediaProviderConfig, output: OutputStream) {
        val jsonString = json.encodeToString(t)
        val encryptedBytes = cryptoData.encrypt(jsonString.toByteArray())
        val encodedString = Base64.encode(encryptedBytes)

        withContext(Dispatchers.IO) {
            output.write(encodedString.toByteArray())
        }
    }
}

//endregion