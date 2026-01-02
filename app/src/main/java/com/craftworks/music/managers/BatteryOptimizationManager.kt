package com.craftworks.music.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.craftworks.music.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BatteryOptimizationManager(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    companion object {
        private val BATTERY_PROMPT_DISMISSED = booleanPreferencesKey("battery_prompt_dismissed")
    }

    /**
     * Check if the app is exempted from battery optimization
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Flow to check if user has dismissed the battery prompt before
     */
    val hasUserDismissedPromptFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BATTERY_PROMPT_DISMISSED] ?: false
    }

    /**
     * Mark that user has dismissed the prompt (don't show again)
     */
    suspend fun dismissPrompt() {
        context.dataStore.edit { prefs ->
            prefs[BATTERY_PROMPT_DISMISSED] = true
        }
    }

    /**
     * Reset the dismissed state (for testing or settings)
     */
    suspend fun resetPrompt() {
        context.dataStore.edit { prefs ->
            prefs[BATTERY_PROMPT_DISMISSED] = false
        }
    }

    /**
     * Request to disable battery optimization - shows system dialog
     */
    fun requestDisableBatteryOptimization(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Open battery optimization settings page (fallback if direct request fails)
     */
    fun openBatteryOptimizationSettings(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}
