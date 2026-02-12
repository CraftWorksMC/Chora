package com.craftworks.music.managers.settings

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.R
import com.craftworks.music.data.BottomNavItem
import com.craftworks.music.dataStore
import com.craftworks.music.ui.playing.NowPlayingBackground
import com.craftworks.music.ui.playing.NowPlayingTitleAlignment
import com.craftworks.music.ui.screens.HomeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearanceSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val NP_BACKGROUND_KEY = stringPreferencesKey("np_background_type")
        private val NP_TITLE_ALIGNMENT = stringPreferencesKey("np_title_alignment")
        private val SHOW_NAVIDROME_KEY = booleanPreferencesKey("show_navidrome_logo")
        private val SHOW_MORE_INFO_KEY = booleanPreferencesKey("show_more_info")
        private val NOW_PLAYING_LYRIC_BLUR_KEY = booleanPreferencesKey("now_playing_lyrics_blur")
        private val BOTTOM_NAV_ITEMS_KEY = stringPreferencesKey("bottom_nav_order")
        private val HOME_ITEMS_KEY = stringPreferencesKey("home_items_order")
        private val APP_THEME = stringPreferencesKey("theme")

        enum class AppTheme {
            LIGHT, DARK, SYSTEM
        }

        private val SHOW_PROVIDER_DIVIDERS = booleanPreferencesKey("provider_dividers")
        private val LYRICS_ANIMATION_SPEED = intPreferencesKey("lyrics_animation_speed")
        private val USE_REFRESH_ANIMATION = booleanPreferencesKey("use_refresh_animation")
        private val SHOW_TRACK_NUMBERS = booleanPreferencesKey("show_track_numbers")
    }

    val usernameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: "Username"
    }

    suspend fun setUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }

    val npBackgroundFlow: Flow<NowPlayingBackground> = context.dataStore.data.map { preferences ->
        try {
            NowPlayingBackground.valueOf(
                preferences[NP_BACKGROUND_KEY] ?:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    NowPlayingBackground.ANIMATED_BLUR.name
                else
                    NowPlayingBackground.STATIC_BLUR.name
            )
        }
        catch (e: Exception) {
            println(e.message)
            NowPlayingBackground.STATIC_BLUR
        }
    }

    suspend fun setBackgroundType(backgroundType: NowPlayingBackground) {
        context.dataStore.edit { preferences ->
            preferences[NP_BACKGROUND_KEY] = backgroundType.name
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

    val nowPlayingLyricsBlurFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOW_PLAYING_LYRIC_BLUR_KEY]
            ?: true
    }

    suspend fun setNowPlayingLyricsBlur(blur: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOW_PLAYING_LYRIC_BLUR_KEY] = blur
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

    val homeItemsItemsFlow: Flow<List<HomeItem>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[HOME_ITEMS_KEY]
        val defaultValue = listOf(
            HomeItem("recently_played", true),
            HomeItem("recently_added", true),
            HomeItem("most_played", true),
            HomeItem("random_songs", true)
        )
        try {
            jsonString?.let { Json.decodeFromString<List<HomeItem>>(it) } ?: defaultValue
        }
        catch (e: Exception) {
            println(e.message)
            defaultValue
        }
    }

    suspend fun setHomeItems(items: List<HomeItem>) {
        context.dataStore.edit { preferences ->
            preferences[HOME_ITEMS_KEY] = Json.encodeToString(items)
        }
    }

    val bottomNavItemsFlow: Flow<List<BottomNavItem>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[BOTTOM_NAV_ITEMS_KEY]
        val defaultValue = listOf(
            BottomNavItem("Home", R.drawable.rounded_home_24, "home_screen"),
            BottomNavItem("Albums", R.drawable.rounded_library_music_24, "album_screen"),
            BottomNavItem("Songs", R.drawable.round_music_note_24, "songs_screen"),
            BottomNavItem("Artists", R.drawable.rounded_artist_24, "artists_screen"),
            BottomNavItem("Radios", R.drawable.rounded_radio, "radio_screen"),
            BottomNavItem("Playlists", R.drawable.placeholder, "playlist_screen")
        )
        try {
            jsonString?.let { Json.decodeFromString<List<BottomNavItem>>(it) } ?: defaultValue
        }
        catch (e: Exception) {
            println(e.message)
            defaultValue
        }
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

    val showProviderDividersFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_PROVIDER_DIVIDERS] ?: true
    }

    suspend fun setShowProviderDividers(showDividers: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PROVIDER_DIVIDERS] = showDividers
        }
    }

    val lyricsAnimationSpeedFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LYRICS_ANIMATION_SPEED] ?: 1200
    }

    suspend fun setLyricsAnimationSpeed(speed: Int) {
        context.dataStore.edit { preferences ->
            preferences[LYRICS_ANIMATION_SPEED] = speed
        }
    }

    val refreshAnimationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_REFRESH_ANIMATION]
            ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    }

    suspend fun setUseRefreshAnimation(useRefreshAnimation: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_REFRESH_ANIMATION] = useRefreshAnimation
        }
    }

    val showTrackNumbersFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_TRACK_NUMBERS] ?: true
    }

    suspend fun setShowTrackNumbers(showTrackNumbers: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_TRACK_NUMBERS] = showTrackNumbers
        }
    }

    val nowPlayingTitleAlignment: Flow<NowPlayingTitleAlignment> = context.dataStore.data.map { preferences ->
        NowPlayingTitleAlignment.valueOf(
            preferences[NP_TITLE_ALIGNMENT] ?: NowPlayingTitleAlignment.LEFT.name
        )
    }

    suspend fun setNowPlayingTitleAlignment(nowPlayingTitleAlignment: NowPlayingTitleAlignment) {
        context.dataStore.edit { preferences ->
            preferences[NP_TITLE_ALIGNMENT] = nowPlayingTitleAlignment.name
        }
    }
}