package com.craftworks.music.managers
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.craftworks.music.R
import com.craftworks.music.data.BottomNavItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(
    private val context: Context
) {
    companion object {
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val NP_BACKGROUND_KEY = stringPreferencesKey("np_background_type")
        private val SHOW_NAVIDROME_KEY = booleanPreferencesKey("show_navidrome_logo")
        private val SHOW_MORE_INFO_KEY = booleanPreferencesKey("show_more_info")
        private val BOTTOM_NAV_ITEMS_KEY = stringPreferencesKey("bottom_nav_order")

        private val TRANSCODING_BITRATE_KEY = stringPreferencesKey("transcoding_bitrate")
        private val SCROBBLE_PERCENT_KEY = intPreferencesKey("scrobble_percent")
    }

    //region Appearance Settings
    val usernameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: "Username"
    }

    suspend fun setUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    val npBackgroundFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[NP_BACKGROUND_KEY] ?: "Animated Blur"
    }

    suspend fun setBackgroundType(backgroundType: String) {
        context.dataStore.edit { preferences ->
            preferences[NP_BACKGROUND_KEY] = backgroundType
        }
    }

    val showMoreInfoFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_MORE_INFO_KEY] ?: true
    }

    suspend fun setShowMoreInfo(showMoreInfo: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_MORE_INFO_KEY] = showMoreInfo
        }
    }

    val showNavidromeLogoFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_NAVIDROME_KEY] ?: true
    }

    suspend fun setShowNavidromeLogo(showNavidromeLogo: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_NAVIDROME_KEY] = showNavidromeLogo
        }
    }

    val bottomNavItemsFlow: Flow<List<BottomNavItem>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[BOTTOM_NAV_ITEMS_KEY]
        jsonString?.let { Json.decodeFromString<List<BottomNavItem>>(it) } ?: //region Default Items
        listOf(
            BottomNavItem(
                "Home", R.drawable.rounded_home_24, "home_screen"
            ), BottomNavItem(
                "Albums", R.drawable.rounded_library_music_24, "album_screen"
            ), BottomNavItem(
                "Songs", R.drawable.round_music_note_24, "songs_screen"
            ), BottomNavItem(
                "Artists", R.drawable.rounded_artist_24, "artists_screen"
            ), BottomNavItem(
                "Radios", R.drawable.rounded_radio, "radio_screen"
            ), BottomNavItem(
                "Playlists", R.drawable.placeholder, "playlist_screen"
            )
        )
        //endregion
    }

    suspend fun setBottomNavItems(items: List<BottomNavItem>) {
        context.dataStore.edit { preferences ->
            preferences[BOTTOM_NAV_ITEMS_KEY] = Json.encodeToString(items)
        }
    }
    //endregion

    //region Playback Settings
    val transcodingBitrateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_BITRATE_KEY] ?: "No Transcoding"
    }

    suspend fun setTranscodingBitrate(bitrate: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCODING_BITRATE_KEY] = bitrate
        }
    }

    val scrobblePercentFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SCROBBLE_PERCENT_KEY] ?: 75
    }

    suspend fun setScrobblePercent(scrobblePercent: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCROBBLE_PERCENT_KEY] = scrobblePercent
        }
    }
    //endregion
}