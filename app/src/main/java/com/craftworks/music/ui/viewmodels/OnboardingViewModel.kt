package com.craftworks.music.ui.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.OnboardingSettingsManager
import com.craftworks.music.managers.settings.OnboardingSettingsManager.ProviderType
import com.craftworks.music.providers.navidrome.getNavidromeStatus
import com.craftworks.music.providers.navidrome.navidromeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val onboardingSettingsManager: OnboardingSettingsManager,
    private val appearanceSettingsManager: AppearanceSettingsManager
) : ViewModel() {

    // Current wizard step (0 = Welcome, 1 = Provider Type, 2 = Quick Setup, 3 = Done)
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // Selected provider type
    private val _selectedProviderType = MutableStateFlow<ProviderType?>(null)
    val selectedProviderType: StateFlow<ProviderType?> = _selectedProviderType.asStateFlow()

    // Navidrome setup fields
    private val _navidromeUrl = MutableStateFlow("")
    val navidromeUrl: StateFlow<String> = _navidromeUrl.asStateFlow()

    private val _navidromeUsername = MutableStateFlow("")
    val navidromeUsername: StateFlow<String> = _navidromeUsername.asStateFlow()

    private val _navidromePassword = MutableStateFlow("")
    val navidromePassword: StateFlow<String> = _navidromePassword.asStateFlow()

    private val _allowSelfSignedCerts = MutableStateFlow(false)
    val allowSelfSignedCerts: StateFlow<Boolean> = _allowSelfSignedCerts.asStateFlow()

    // Local folder setup
    private val _localFolderPath = MutableStateFlow("/Music/")
    val localFolderPath: StateFlow<String> = _localFolderPath.asStateFlow()

    // Radio setup
    private val _radioName = MutableStateFlow("")
    val radioName: StateFlow<String> = _radioName.asStateFlow()

    private val _radioUrl = MutableStateFlow("")
    val radioUrl: StateFlow<String> = _radioUrl.asStateFlow()

    // User's preferred name
    private val _preferredName = MutableStateFlow("")
    val preferredName: StateFlow<String> = _preferredName.asStateFlow()

    // Connection test status
    private val _connectionTestStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionTestStatus: StateFlow<ConnectionStatus> = _connectionTestStatus.asStateFlow()

    // Provider added successfully
    private val _providerAdded = MutableStateFlow(false)
    val providerAdded: StateFlow<Boolean> = _providerAdded.asStateFlow()

    // Permission states
    private val _audioPermissionGranted = MutableStateFlow(false)
    val audioPermissionGranted: StateFlow<Boolean> = _audioPermissionGranted.asStateFlow()

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkPermissions()
    }

    // Step navigation
    fun goToStep(step: Int) {
        _currentStep.value = step.coerceIn(0, 3)
    }

    fun nextStep() {
        _currentStep.value = (_currentStep.value + 1).coerceAtMost(3)
    }

    fun previousStep() {
        _currentStep.value = (_currentStep.value - 1).coerceAtLeast(0)
    }

    // Provider type selection
    fun selectProviderType(type: ProviderType) {
        _selectedProviderType.value = type
    }

    // Navidrome field updates
    fun updateNavidromeUrl(url: String) {
        _navidromeUrl.value = url
    }

    fun updateNavidromeUsername(username: String) {
        _navidromeUsername.value = username
    }

    fun updateNavidromePassword(password: String) {
        _navidromePassword.value = password
    }

    fun updateAllowSelfSignedCerts(allow: Boolean) {
        _allowSelfSignedCerts.value = allow
    }

    // Local folder field updates
    fun updateLocalFolderPath(path: String) {
        _localFolderPath.value = path
    }

    // Radio field updates
    fun updateRadioName(name: String) {
        _radioName.value = name
    }

    fun updateRadioUrl(url: String) {
        _radioUrl.value = url
    }

    // User name update
    fun updatePreferredName(name: String) {
        _preferredName.value = name
    }

    // Test Navidrome connection
    fun testNavidromeConnection() {
        viewModelScope.launch {
            _connectionTestStatus.value = ConnectionStatus.Testing
            try {
                val server = NavidromeProvider(
                    _navidromeUrl.value,
                    _navidromeUrl.value,
                    _navidromeUsername.value,
                    _navidromePassword.value,
                    true,
                    _allowSelfSignedCerts.value
                )
                getNavidromeStatus(server, context)

                // Watch for status change
                when (navidromeStatus.value) {
                    "ok" -> _connectionTestStatus.value = ConnectionStatus.Success
                    "Invalid URL" -> _connectionTestStatus.value = ConnectionStatus.Error("Invalid URL")
                    "Wrong username or password" -> _connectionTestStatus.value = ConnectionStatus.Error("Wrong username or password")
                    else -> _connectionTestStatus.value = ConnectionStatus.Error(navidromeStatus.value.ifEmpty { "Connection failed" })
                }
            } catch (e: Exception) {
                _connectionTestStatus.value = ConnectionStatus.Error(e.message ?: "Connection failed")
            }
        }
    }

    // Add Navidrome server
    fun addNavidromeServer() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val server = NavidromeProvider(
                    _navidromeUrl.value,
                    _navidromeUrl.value,
                    _navidromeUsername.value,
                    _navidromePassword.value,
                    true,
                    _allowSelfSignedCerts.value
                )
                NavidromeManager.addServer(server)
                navidromeStatus.value = ""
                _providerAdded.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add local folder
    fun addLocalFolder() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = LocalProviderManager.addFolder(_localFolderPath.value)
                _providerAdded.value = success
            } catch (e: Exception) {
                e.printStackTrace()
                _providerAdded.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Complete onboarding
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                // Save the user's preferred name if provided
                if (_preferredName.value.isNotBlank()) {
                    appearanceSettingsManager.setUsername(_preferredName.value)
                }
                val providerType = _selectedProviderType.value ?: ProviderType.NONE
                onboardingSettingsManager.completeOnboarding(providerType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Skip onboarding
    fun skipOnboarding() {
        viewModelScope.launch {
            try {
                onboardingSettingsManager.skipOnboarding()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Check permissions
    fun checkPermissions() {
        _audioPermissionGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        _notificationPermissionGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-Android 13 doesn't need notification permission
        }
    }

    fun updateAudioPermissionGranted(granted: Boolean) {
        _audioPermissionGranted.value = granted
    }

    fun updateNotificationPermissionGranted(granted: Boolean) {
        _notificationPermissionGranted.value = granted
    }

    // Reset state for a fresh start
    fun reset() {
        _currentStep.value = 0
        _selectedProviderType.value = null
        _navidromeUrl.value = ""
        _navidromeUsername.value = ""
        _navidromePassword.value = ""
        _allowSelfSignedCerts.value = false
        _localFolderPath.value = "/Music/"
        _radioName.value = ""
        _radioUrl.value = ""
        _preferredName.value = ""
        _connectionTestStatus.value = ConnectionStatus.Idle
        _providerAdded.value = false
        navidromeStatus.value = ""
    }

    sealed class ConnectionStatus {
        data object Idle : ConnectionStatus()
        data object Testing : ConnectionStatus()
        data object Success : ConnectionStatus()
        data class Error(val message: String) : ConnectionStatus()
    }
}
