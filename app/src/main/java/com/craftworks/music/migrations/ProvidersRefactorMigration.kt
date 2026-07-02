package com.craftworks.music.migrations

import android.content.Context
import androidx.core.content.edit
import com.craftworks.music.utils.StringUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class ProvidersRefactorMigration : Migration {
    private val PREF_SERVERS = "navidrome_servers"
    private val PREF_CURRENT_SERVER = "current_server_id"
    private val PREF_FOLDERS = "local_folders"

    private val PREF_PROVIDERS = "providers"
    private val PREF_CURRENT_PROVIDER = "current_provider_id"
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = MediaProvider.serializerModule
    }

    override fun up(context: Context) {
        val localProvider = context.getSharedPreferences("LocalProviderPrefs", Context.MODE_PRIVATE)
        val navidromeProvider = context.getSharedPreferences("NavidromePrefs", Context.MODE_PRIVATE)
        val provider = context.getSharedPreferences("MediaProvidersPrefs", Context.MODE_PRIVATE)

        // TODO: Migrate local provider

        val currentId = navidromeProvider.getString(PREF_CURRENT_SERVER, null)
        val serversJson = navidromeProvider.getString(PREF_SERVERS, null)
        var servers = mapOf<String, NavidromeProvider>()
        if (serversJson != null) {
            servers = json.decodeFromString(serversJson)
        }
        val providers = mutableMapOf<String, MediaProvider>()

        for (server in servers) {
            val salt = StringUtils.generateSalt(8)
            val token = StringUtils.md5Hash(server.value.password + salt)

            val newProvider = SubsonicMediaProvider(SubsonicProviderData(
                url = server.value.url,
                username = server.value.username,
                credentials = "$salt:$token",
                allowSelfSignedCert = server.value.allowSelfSignedCert?:false
            ))
            newProvider.data = MediaProviderData(
                server.value.libraryIds.map {
                    Pair(MusicFolder(it.first.id.toString(), it.first.name), it.second)
                }
            )

            providers[server.key] = newProvider
        }

        val providersJson = json.encodeToString(providers)
        provider.edit { putString(PREF_PROVIDERS, providersJson) }
        provider.edit { putString(PREF_CURRENT_PROVIDER, currentId) }
    }

    @Serializable
    private data class NavidromeProvider (
        val id: String = "0",
        var url:String,
        var username:String,
        val password:String,
        val enabled:Boolean? = true,
        var allowSelfSignedCert: Boolean? = false,
        var libraryIds: List<Pair<NavidromeLibrary, Boolean>> = listOf(Pair(NavidromeLibrary(0, "Media Library"), true))
    )

    @Serializable
    private data class NavidromeLibrary (
        val id: Int = 0,
        var name:String,
    )

    @Serializable
    private abstract class MediaProvider {
        companion object {
            val serializerModule = SerializersModule {
                polymorphic(MediaProvider::class) {
                    subclass(SubsonicMediaProvider::class)
                }
            }
        }
        lateinit var data: MediaProviderData
    }

    @Serializable
    private data class MusicFolder (
        val id: String,
        val name: String,
    )
    @Serializable
    private data class MediaProviderData (var libraries: List<Pair<MusicFolder, Boolean>>)
    @Serializable
    @SerialName("subsonic")
    private class SubsonicMediaProvider(var providerData: SubsonicProviderData) : MediaProvider() {
    }

    @Serializable
    private data class SubsonicProviderData (
        var url:String,
        var username:String,
        var credentials:String,
        var allowSelfSignedCert: Boolean = false,
    )
}
