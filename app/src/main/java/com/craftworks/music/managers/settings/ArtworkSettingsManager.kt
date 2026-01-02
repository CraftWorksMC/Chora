package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        // Main toggle
        private val GENERATED_ARTWORK_ENABLED = booleanPreferencesKey("generated_artwork_enabled")

        // When to show generated artwork
        private val ARTWORK_FALLBACK_MODE = stringPreferencesKey("artwork_fallback_mode")

        // Style options
        private val ARTWORK_STYLE = stringPreferencesKey("artwork_style")
        private val ARTWORK_COLOR_PALETTE = stringPreferencesKey("artwork_color_palette")
        private val ARTWORK_SHOW_INITIALS = booleanPreferencesKey("artwork_show_initials")
        private val ARTWORK_ANIMATE = booleanPreferencesKey("artwork_animate")

        // Quality
        private val PREFER_HIGH_QUALITY = booleanPreferencesKey("prefer_high_quality_artwork")
        private val CACHE_ARTWORK = booleanPreferencesKey("cache_artwork")
    }

    enum class FallbackMode {
        ALWAYS,           // Always use generated art (ignore server art)
        NO_ARTWORK,       // Only when no artwork URL exists
        ON_ERROR,         // When artwork fails to load
        PLACEHOLDER_DETECT // Detect and replace placeholder images
    }

    enum class ArtworkStyle {
        GRADIENT,         // Gradient with initials
        SOLID,            // Solid color with initials
        PATTERN,          // Geometric pattern
        WAVEFORM,         // Audio waveform style
        MINIMAL           // Just initials, subtle background
    }

    enum class ColorPalette {
        MATERIAL_YOU,     // Material You inspired colors
        VIBRANT,          // Bright, saturated colors
        PASTEL,           // Soft pastel colors
        MONOCHROME,       // Grayscale
        DYNAMIC           // Based on title hash
    }

    // Generated artwork master toggle
    val generatedArtworkEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GENERATED_ARTWORK_ENABLED] ?: true
    }

    suspend fun setGeneratedArtworkEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GENERATED_ARTWORK_ENABLED] = enabled
        }
    }

    // Fallback mode
    val fallbackModeFlow: Flow<FallbackMode> = context.dataStore.data.map { preferences ->
        try {
            FallbackMode.valueOf(preferences[ARTWORK_FALLBACK_MODE] ?: FallbackMode.PLACEHOLDER_DETECT.name)
        } catch (e: IllegalArgumentException) {
            FallbackMode.PLACEHOLDER_DETECT
        }
    }

    suspend fun setFallbackMode(mode: FallbackMode) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_FALLBACK_MODE] = mode.name
        }
    }

    // Artwork style
    val artworkStyleFlow: Flow<ArtworkStyle> = context.dataStore.data.map { preferences ->
        try {
            ArtworkStyle.valueOf(preferences[ARTWORK_STYLE] ?: ArtworkStyle.GRADIENT.name)
        } catch (e: IllegalArgumentException) {
            ArtworkStyle.GRADIENT
        }
    }

    suspend fun setArtworkStyle(style: ArtworkStyle) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_STYLE] = style.name
        }
    }

    // Color palette
    val colorPaletteFlow: Flow<ColorPalette> = context.dataStore.data.map { preferences ->
        try {
            ColorPalette.valueOf(preferences[ARTWORK_COLOR_PALETTE] ?: ColorPalette.MATERIAL_YOU.name)
        } catch (e: IllegalArgumentException) {
            ColorPalette.MATERIAL_YOU
        }
    }

    suspend fun setColorPalette(palette: ColorPalette) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_COLOR_PALETTE] = palette.name
        }
    }

    // Show initials
    val showInitialsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ARTWORK_SHOW_INITIALS] ?: true
    }

    suspend fun setShowInitials(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_SHOW_INITIALS] = show
        }
    }

    // Animate artwork
    val animateArtworkFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ARTWORK_ANIMATE] ?: false
    }

    suspend fun setAnimateArtwork(animate: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_ANIMATE] = animate
        }
    }

    // High quality preference
    val preferHighQualityFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PREFER_HIGH_QUALITY] ?: true
    }

    suspend fun setPreferHighQuality(prefer: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREFER_HIGH_QUALITY] = prefer
        }
    }

    // Cache artwork
    val cacheArtworkFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CACHE_ARTWORK] ?: true
    }

    suspend fun setCacheArtwork(cache: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CACHE_ARTWORK] = cache
        }
    }
}
