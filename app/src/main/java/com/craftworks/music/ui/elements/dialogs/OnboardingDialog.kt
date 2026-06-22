package com.craftworks.music.ui.elements.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.R
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.ui.elements.LocalProviderCard
import com.craftworks.music.ui.elements.NavidromeProviderCard

enum class OnboardingStep { OVERVIEW, PROVIDER_SELECTION, DONE }

@Preview(showSystemUi = true)
@Composable
fun OnboardingDialog(
    setShowDialog: (Boolean) -> Unit = {}
) {
    var step by remember { mutableStateOf(OnboardingStep.OVERVIEW) }
    var showAddProviderDialog by remember { mutableStateOf(false) }

    val localProviders by LocalProviderManager.allFolders.collectAsStateWithLifecycle()
    val navidromeServers by NavidromeManager.allServers.collectAsStateWithLifecycle()

    val hasProviders = localProviders.isNotEmpty() || navidromeServers.isNotEmpty()

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                StepIndicator(
                    currentStep = step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 4.dp)
                )

                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialOffsetX = { if (forward) it / 3 else -it / 3 }
                        ) + fadeIn(tween(200)) togetherWith
                                slideOutHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    targetOffsetX = { if (forward) -it / 3 else it / 3 }
                                ) + fadeOut(tween(200))
                    },
                    label = "OnboardingStep"
                ) { currentStep ->
                    when (currentStep) {
                        OnboardingStep.OVERVIEW -> OverviewStep(
                            localProviders = localProviders,
                            navidromeServers = navidromeServers,
                            hasProviders = hasProviders,
                            onAddProvider = { showAddProviderDialog = true },
                            onNext = { step = OnboardingStep.DONE },
                            onSkip = { setShowDialog(false) }
                        )

                        OnboardingStep.DONE -> DoneStep(
                            onFinish = { setShowDialog(false) }
                        )

                        else -> {}
                    }
                }
            }
        }
    }

    if (showAddProviderDialog) {
        CreateMediaProviderDialog(
            setShowDialog = { showAddProviderDialog = it },
            context = LocalContext.current
        )
    }
}

@Composable
private fun StepIndicator(
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OnboardingStep.entries.forEachIndexed { index, step ->
            if (step == OnboardingStep.PROVIDER_SELECTION)
                return@forEachIndexed

            val isActive = step.ordinal <= currentStep.ordinal
            val width by animateFloatAsState(
                targetValue = if (step == currentStep) 24f else 8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(width = width.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun OverviewStep(
    localProviders: List<String>,
    navidromeServers: List<NavidromeProvider>,
    hasProviders: Boolean,
    onAddProvider: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_banner_foreground),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
        )

        Text(
            text = stringResource(R.string.No_Providers_Splash),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(localProviders) { local ->
                LocalProviderCard(local)
            }
            items(navidromeServers, key = { it.id }) { server ->
                NavidromeProviderCard(server)
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = onAddProvider,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
            Text(
                text = stringResource(R.string.Action_Add),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.weight(1f))

        Crossfade(
            targetState = hasProviders
        ) {
            if (it) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = onNext,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.Action_Next),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "Skip for now",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DoneStep(onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_banner_foreground),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        Text(
            text = stringResource(R.string.No_Providers_Done),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            onClick = onFinish,
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = stringResource(R.string.Action_Go),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}