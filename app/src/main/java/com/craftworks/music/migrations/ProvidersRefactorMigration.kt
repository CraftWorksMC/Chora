package com.craftworks.music.migrations

import android.content.Context
import com.craftworks.music.data.model.MediaProviderData
import com.craftworks.music.data.model.MusicFolder
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.providers.MediaProvider
import com.craftworks.music.providers.local.LocalMediaProvider
import com.craftworks.music.providers.local.LocalProviderData
import com.craftworks.music.providers.subsonic.SubsonicMediaProvider
import com.craftworks.music.providers.subsonic.SubsonicProviderData
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

class ProvidersRefactorMigration : Migration {
    private val PREF_SERVERS = "navidrome_servers"
    private val PREF_CURRENT_SERVER = "current_server_id"

    private val PREF_FOLDERS = "local_folders"

    private val oldJson = Json { ignoreUnknownKeys = true }

    override fun up(context: Context) {
        val navidromeProvider = context.getSharedPreferences("NavidromePrefs", Context.MODE_PRIVATE)
        val localProvider = context.getSharedPreferences("LocalProviderPrefs", Context.MODE_PRIVATE)

        val currentId = navidromeProvider.getString(PREF_CURRENT_SERVER, null)
        val serversJson = navidromeProvider.getString(PREF_SERVERS, null)
        val servers: Map<String, NavidromeProvider> =
            serversJson?.let { oldJson.decodeFromString(it) } ?: emptyMap()

        val foldersJson = localProvider.getString(PREF_FOLDERS, null)
        val folders: List<String> =
            foldersJson?.let { oldJson.decodeFromString(it) } ?: emptyList()

        val providers = mutableMapOf<String, MediaProvider>()
        for ((key, server) in servers) {
            val newProvider = SubsonicMediaProvider()
            newProvider.providerData = SubsonicProviderData(
                url = server.url,
                username = server.username,
                password = server.password,
                allowSelfSignedCert = server.allowSelfSignedCert ?: false
            )
            newProvider.id = key
            newProvider.data = MediaProviderData(
                server.libraryIds.map {
                    Pair(MusicFolder(it.first.id.toString(), it.first.name), it.second)
                }
            )
            providers[key] = newProvider
        }

        for (folder in folders) {
            val key = UUID.randomUUID().toString()
            val newProvider = LocalMediaProvider(LocalProviderData(folder))
            newProvider.init(context)
            newProvider.id = key
            newProvider.data = MediaProviderData(listOf(Pair(MusicFolder(folder, folder), true)))
            providers[key] = newProvider
        }

        val newCurrentId = currentId ?: providers.keys.firstOrNull()

        runBlocking {
            MediaProviderManager.importProviders(context, providers, newCurrentId)
        }
    }

    @Serializable
    private data class NavidromeProvider(
        val id: String = "0",
        var url: String,
        var username: String,
        val password: String,
        val enabled: Boolean? = true,
        var allowSelfSignedCert: Boolean? = false,
        var libraryIds: List<Pair<NavidromeLibrary, Boolean>> =
            listOf(Pair(NavidromeLibrary(0, "Media Library"), true))
    )

    @Serializable
    private data class NavidromeLibrary(
        val id: Int = 0,
        var name: String,
    )
}