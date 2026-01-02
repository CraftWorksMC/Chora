package com.craftworks.music.managers.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.craftworks.music.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingSettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val ONBOARDING_SKIPPED = booleanPreferencesKey("onboarding_skipped")
        private val SELECTED_PROVIDER_TYPE = stringPreferencesKey("onboarding_provider_type")
    }

    enum class ProviderType {
        NAVIDROME,
        LOCAL,
        RADIO,
        NONE
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val onboardingSkippedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_SKIPPED] ?: false
    }

    val selectedProviderTypeFlow: Flow<ProviderType> = context.dataStore.data.map { preferences ->
        try {
            ProviderType.valueOf(preferences[SELECTED_PROVIDER_TYPE] ?: ProviderType.NONE.name)
        } catch (e: IllegalArgumentException) {
            ProviderType.NONE
        }
    }

    /**
     * Returns true if the onboarding wizard should be shown.
     * This is true when onboarding has not been completed AND has not been skipped.
     */
    val shouldShowOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val completed = preferences[ONBOARDING_COMPLETED] ?: false
        val skipped = preferences[ONBOARDING_SKIPPED] ?: false
        !completed && !skipped
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setOnboardingSkipped(skipped: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_SKIPPED] = skipped
        }
    }

    suspend fun setSelectedProviderType(providerType: ProviderType) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PROVIDER_TYPE] = providerType.name
        }
    }

    /**
     * Mark onboarding as complete and optionally store the selected provider type.
     */
    suspend fun completeOnboarding(providerType: ProviderType = ProviderType.NONE) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
            preferences[ONBOARDING_SKIPPED] = false
            if (providerType != ProviderType.NONE) {
                preferences[SELECTED_PROVIDER_TYPE] = providerType.name
            }
        }
    }

    /**
     * Skip onboarding - user can still access settings later to configure providers.
     */
    suspend fun skipOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = false
            preferences[ONBOARDING_SKIPPED] = true
        }
    }

    /**
     * Reset onboarding state - useful for testing or if user wants to redo setup.
     */
    suspend fun resetOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = false
            preferences[ONBOARDING_SKIPPED] = false
            preferences.remove(SELECTED_PROVIDER_TYPE)
        }
    }

}
