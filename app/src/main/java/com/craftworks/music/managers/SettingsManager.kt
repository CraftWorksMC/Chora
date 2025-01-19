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
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
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
        private val NOW_PLAYING_LYRIC_BLUR_KEY = booleanPreferencesKey("now_playing_lyrics_blur")
        private val BOTTOM_NAV_ITEMS_KEY = stringPreferencesKey("bottom_nav_order")
        private val APP_THEME = stringPreferencesKey("theme")
        enum class AppTheme {
            LIGHT, DARK, SYSTEM
        }

        private val TRANSCODING_BITRATE_WIFI_KEY = stringPreferencesKey("transcoding_bitrate_wifi")
        private val TRANSCODING_BITRATE_DATA_KEY = stringPreferencesKey("transcoding_bitrate_data")
        private val SCROBBLE_PERCENT_KEY = intPreferencesKey("scrobble_percent")

        private val LOCAL_RADIOS = stringPreferencesKey("radios_list")
        private val LOCAL_PLAYLISTS = stringPreferencesKey("playlists_list")
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
        preferences[SHOW_MORE_INFO_KEY] != false
    }

    suspend fun setShowMoreInfo(showMoreInfo: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_MORE_INFO_KEY] = showMoreInfo
        }
    }

    val nowPlayingLyricsBlurFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOW_PLAYING_LYRIC_BLUR_KEY] != false
    }

    suspend fun setNowPlayingLyricsBlur(blur: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOW_PLAYING_LYRIC_BLUR_KEY] = blur
        }
    }

    val showNavidromeLogoFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_NAVIDROME_KEY] != false
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

    val appTheme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME] ?: "SYSTEM"
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme.name
        }
    }
    //endregion

    //region Playback Settings
    val wifiTranscodingBitrateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_BITRATE_WIFI_KEY] ?: "No Transcoding"
    }

    suspend fun setWifiTranscodingBitrate(bitrate: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCODING_BITRATE_WIFI_KEY] = bitrate
        }
    }

    val mobileDataTranscodingBitrateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRANSCODING_BITRATE_DATA_KEY] ?: "No Transcoding"
    }

    suspend fun setMobileDataTranscodingBitrate(bitrate: String) {
        context.dataStore.edit { preferences ->
            preferences[TRANSCODING_BITRATE_DATA_KEY] = bitrate
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

    val localRadios:  Flow<MutableList<MediaData.Radio>> = context.dataStore.data.map { preferences ->
        Json.decodeFromString<List<MediaData.Radio>>(preferences[LOCAL_RADIOS] ?: "[]").toMutableList()
    }

    suspend fun saveLocalRadios() {
        val radiosListJson = Json.encodeToString(radioList)
        context.dataStore.edit { preferences ->
            preferences[LOCAL_RADIOS] = radiosListJson
        }
    }

    val localPlaylists:  Flow<MutableList<MediaData.Playlist>> = context.dataStore.data.map { preferences ->
        Json.decodeFromString<List<MediaData.Playlist>>(preferences[LOCAL_PLAYLISTS] ?: "[]").toMutableList()
    }

    suspend fun saveLocalPlaylists(){
        val playlistJson = Json.encodeToString(playlistList)
        context.dataStore.edit { preferences ->
            preferences[LOCAL_PLAYLISTS] = playlistJson
        }
    }
}