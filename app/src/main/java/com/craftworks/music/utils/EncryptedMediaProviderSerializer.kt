package com.craftworks.music.utils

import androidx.datastore.core.Serializer
import com.craftworks.music.providers.MediaProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import kotlin.io.encoding.Base64

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
