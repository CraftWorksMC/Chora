package com.craftworks.music.ui.screens.onboarding

import android.Manifest
import android.os.Build
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.managers.settings.OnboardingSettingsManager.ProviderType
import com.craftworks.music.providers.navidrome.navidromeStatus
import com.craftworks.music.ui.viewmodels.OnboardingViewModel
import kotlinx.coroutines.delay

/**
 * Step 1: Welcome screen with permission requests.
 * Beautiful animated entrance with staggered content reveals.
 */
@Composable
fun WelcomeStep(
    viewModel: OnboardingViewModel,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    val audioPermissionGranted by viewModel.audioPermissionGranted.collectAsStateWithLifecycle()
    val notificationPermissionGranted by viewModel.notificationPermissionGranted.collectAsStateWithLifecycle()
    val preferredName by viewModel.preferredName.collectAsStateWithLifecycle()

    // Staggered animation states
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showNameInput by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showTitle = true
        delay(150)
        showSubtitle = true
        delay(150)
        showNameInput = true
        delay(150)
        showPermissions = true
        delay(150)
        showButtons = true
    }

    // Permission launchers
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateAudioPermissionGranted(granted)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateNotificationPermissionGranted(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // App icon/logo area
        AnimatedVisibility(
            visible = showTitle,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { -40 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_music_note_24),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        AnimatedVisibility(
            visible = showTitle,
            enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        AnimatedVisibility(
            visible = showSubtitle,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Text(
                text = "Let's get you set up",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Name input field
        AnimatedVisibility(
            visible = showNameInput,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            OutlinedTextField(
                value = preferredName,
                onValueChange = { if (it.length <= 64) viewModel.updatePreferredName(it) },
                label = { Text("What should we call you?") },
                placeholder = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Permission cards
        AnimatedVisibility(
            visible = showPermissions,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionCard(
                    title = "Music Access",
                    description = "Required to play local music files",
                    icon = ImageVector.vectorResource(R.drawable.round_music_note_24),
                    isGranted = audioPermissionGranted,
                    onRequestClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        audioPermissionLauncher.launch(permission)
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionCard(
                        title = "Notifications",
                        description = "Show playback controls",
                        icon = ImageVector.vectorResource(R.drawable.rounded_notifications_24),
                        isGranted = notificationPermissionGranted,
                        onRequestClick = {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Buttons
        AnimatedVisibility(
            visible = showButtons,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingPrimaryButton(
                    text = "Continue",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )

                OnboardingSecondaryButton(
                    text = "Skip Setup",
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Step 2: Provider type selection.
 */
@Composable
fun ProviderTypeStep(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val selectedType by viewModel.selectedProviderType.collectAsStateWithLifecycle()

    // Staggered animation states
    var showTitle by remember { mutableStateOf(false) }
    var showCards by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showTitle = true
        delay(200)
        showCards = true
        delay(300)
        showButtons = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Title
        AnimatedVisibility(
            visible = showTitle,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { -30 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "How do you listen?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose your music source",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Provider cards with staggered entrance
        AnimatedVisibility(
            visible = showCards,
            enter = fadeIn(tween(400))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProviderTypeCard(
                    title = "Navidrome / Subsonic",
                    description = "Stream from your personal server",
                    icon = ImageVector.vectorResource(R.drawable.s_m_navidrome),
                    isSelected = selectedType == ProviderType.NAVIDROME,
                    onClick = { viewModel.selectProviderType(ProviderType.NAVIDROME) }
                )

                ProviderTypeCard(
                    title = "Local Music",
                    description = "Play files stored on your device",
                    icon = ImageVector.vectorResource(R.drawable.rounded_folder_24),
                    isSelected = selectedType == ProviderType.LOCAL,
                    onClick = { viewModel.selectProviderType(ProviderType.LOCAL) }
                )

                ProviderTypeCard(
                    title = "Internet Radio",
                    description = "Listen to online radio streams",
                    icon = ImageVector.vectorResource(R.drawable.rounded_radio),
                    isSelected = selectedType == ProviderType.RADIO,
                    onClick = { viewModel.selectProviderType(ProviderType.RADIO) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Buttons
        AnimatedVisibility(
            visible = showButtons,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingSecondaryButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )

                OnboardingPrimaryButton(
                    text = "Continue",
                    onClick = onContinue,
                    enabled = selectedType != null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Step 3: Quick setup based on selected provider type.
 */
@Composable
fun QuickSetupStep(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val selectedType by viewModel.selectedProviderType.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val providerAdded by viewModel.providerAdded.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionTestStatus.collectAsStateWithLifecycle()

    // Auto-advance when provider is added
    LaunchedEffect(providerAdded) {
        if (providerAdded) {
            delay(500)
            onContinue()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = when (selectedType) {
                ProviderType.NAVIDROME -> "Connect to Server"
                ProviderType.LOCAL -> "Add Music Folder"
                ProviderType.RADIO -> "Add Radio Station"
                else -> "Setup"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (selectedType) {
            ProviderType.NAVIDROME -> NavidromeSetupForm(viewModel, connectionStatus, isLoading)
            ProviderType.LOCAL -> LocalFolderSetupForm(viewModel)
            ProviderType.RADIO -> RadioSetupForm(viewModel)
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingSecondaryButton(
                text = "Back",
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )

            OnboardingPrimaryButton(
                text = when (selectedType) {
                    ProviderType.NAVIDROME -> if (connectionStatus is OnboardingViewModel.ConnectionStatus.Success) "Add Server" else "Test"
                    else -> "Add"
                },
                onClick = {
                    when (selectedType) {
                        ProviderType.NAVIDROME -> {
                            if (connectionStatus is OnboardingViewModel.ConnectionStatus.Success) {
                                viewModel.addNavidromeServer()
                            } else {
                                viewModel.testNavidromeConnection()
                            }
                        }
                        ProviderType.LOCAL -> viewModel.addLocalFolder()
                        ProviderType.RADIO -> onContinue() // Radio can be skipped for now
                        else -> {}
                    }
                },
                isLoading = isLoading || connectionStatus is OnboardingViewModel.ConnectionStatus.Testing,
                icon = if (connectionStatus is OnboardingViewModel.ConnectionStatus.Success) Icons.Rounded.Check else null,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun NavidromeSetupForm(
    viewModel: OnboardingViewModel,
    connectionStatus: OnboardingViewModel.ConnectionStatus,
    isLoading: Boolean
) {
    val url by viewModel.navidromeUrl.collectAsStateWithLifecycle()
    val username by viewModel.navidromeUsername.collectAsStateWithLifecycle()
    val password by viewModel.navidromePassword.collectAsStateWithLifecycle()
    val allowSelfSignedCerts by viewModel.allowSelfSignedCerts.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { if (it.length <= 512) viewModel.updateNavidromeUrl(it) },
            label = { Text(stringResource(R.string.Label_Navidrome_URL)) },
            placeholder = { Text("server.com:4533 or https://server.com") },
            supportingText = { Text("Port is optional (defaults to 80/443)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            isError = (connectionStatus is OnboardingViewModel.ConnectionStatus.Error &&
                    (connectionStatus as OnboardingViewModel.ConnectionStatus.Error).message.contains("URL", ignoreCase = true)) ||
                    (url.isNotEmpty() && !Patterns.WEB_URL.matcher(url).matches())
        )

        OutlinedTextField(
            value = username,
            onValueChange = { if (it.length <= 256) viewModel.updateNavidromeUsername(it) },
            label = { Text(stringResource(R.string.Label_Navidrome_Username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { if (it.length <= 256) viewModel.updateNavidromePassword(it) },
            label = { Text(stringResource(R.string.Label_Navidrome_Password)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            if (passwordVisible) R.drawable.round_visibility_24
                            else R.drawable.round_visibility_off_24
                        ),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.Label_Allow_Self_Signed_Certs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "For self-hosted servers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = allowSelfSignedCerts,
                onCheckedChange = { viewModel.updateAllowSelfSignedCerts(it) }
            )
        }

        // Connection status
        when (connectionStatus) {
            is OnboardingViewModel.ConnectionStatus.Testing -> {
                ConnectionStatusIndicator(
                    status = "Testing connection...",
                    isSuccess = false,
                    isLoading = true
                )
            }
            is OnboardingViewModel.ConnectionStatus.Success -> {
                ConnectionStatusIndicator(
                    status = "Connected successfully!",
                    isSuccess = true,
                    isLoading = false
                )
            }
            is OnboardingViewModel.ConnectionStatus.Error -> {
                ConnectionStatusIndicator(
                    status = connectionStatus.message,
                    isSuccess = false,
                    isLoading = false
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun LocalFolderSetupForm(viewModel: OnboardingViewModel) {
    val folderPath by viewModel.localFolderPath.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enter the path to your music folder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = folderPath,
            onValueChange = { if (it.length <= 512) viewModel.updateLocalFolderPath(it) },
            label = { Text(stringResource(R.string.Label_Local_Directory)) },
            placeholder = { Text("/storage/emulated/0/Music/") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.rounded_folder_24),
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun RadioSetupForm(viewModel: OnboardingViewModel) {
    val radioName by viewModel.radioName.collectAsStateWithLifecycle()
    val radioUrl by viewModel.radioUrl.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "You can add radio stations later from the Radio tab",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = radioName,
            onValueChange = { if (it.length <= 128) viewModel.updateRadioName(it) },
            label = { Text("Station Name") },
            placeholder = { Text("My Favorite Station") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = radioUrl,
            onValueChange = { if (it.length <= 512) viewModel.updateRadioUrl(it) },
            label = { Text("Stream URL") },
            placeholder = { Text("https://stream.example.com/radio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
    }
}

/**
 * Step 4: Done screen with celebration animation.
 */
@Composable
fun DoneStep(
    onGetStarted: () -> Unit
) {
    // Staggered animation states
    var showIcon by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    // Scale animation for the check icon
    val iconScale by animateFloatAsState(
        targetValue = if (showIcon) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "icon_scale"
    )

    LaunchedEffect(Unit) {
        delay(200)
        showIcon = true
        delay(400)
        showTitle = true
        delay(300)
        showButton = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // Celebration check icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(iconScale)
                .graphicsLayer {
                    rotationZ = (1f - iconScale) * -30f
                },
            contentAlignment = Alignment.Center
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (showIcon) 1f else 0.5f)
            ) {
                // Simplified celebration effect
            }

            // Main check circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(iconScale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        AnimatedVisibility(
            visible = showTitle,
            enter = fadeIn(tween(500)) + slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You're all set!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Enjoy your music",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Get Started button
        AnimatedVisibility(
            visible = showButton,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            OnboardingPrimaryButton(
                text = "Get Started",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth(),
                icon = null
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
