package com.craftworks.music.ui.screens.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.craftworks.music.player.rememberManagedMediaController
import com.craftworks.music.ui.viewmodels.OnboardingViewModel
import kotlinx.coroutines.launch

/**
 * The main onboarding wizard screen.
 * Features a beautiful starfield background, smooth page transitions,
 * and animated page indicators.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingWizard(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val mediaController by rememberManagedMediaController()

    // Stop any playing song when onboarding starts
    LaunchedEffect(Unit) {
        mediaController?.stop()
        mediaController?.clearMediaItems()
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 4 }
    )

    // Sync pager with viewmodel state
    LaunchedEffect(currentStep) {
        if (pagerState.currentPage != currentStep) {
            pagerState.animateScrollToPage(
                page = currentStep,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Sync viewmodel with pager state (for swipe gestures)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentStep) {
            viewModel.goToStep(pagerState.currentPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Beautiful starfield background
        StarfieldBackground(
            modifier = Modifier.fillMaxSize()
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Page indicator at the top
            OnboardingPageIndicator(
                pageCount = 4,
                currentPage = currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp)
            )

            // Paged content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = false, // Disable swipe, use buttons for navigation
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(
                        viewModel = viewModel,
                        onSkip = {
                            coroutineScope.launch {
                                viewModel.skipOnboarding()
                                onComplete()
                            }
                        },
                        onContinue = {
                            viewModel.nextStep()
                        }
                    )
                    1 -> ProviderTypeStep(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.previousStep()
                        },
                        onContinue = {
                            viewModel.nextStep()
                        }
                    )
                    2 -> QuickSetupStep(
                        viewModel = viewModel,
                        onBack = {
                            viewModel.previousStep()
                        },
                        onContinue = {
                            viewModel.nextStep()
                        }
                    )
                    3 -> DoneStep(
                        onGetStarted = {
                            coroutineScope.launch {
                                viewModel.completeOnboarding()
                                onComplete()
                            }
                        }
                    )
                }
            }
        }
    }
}
