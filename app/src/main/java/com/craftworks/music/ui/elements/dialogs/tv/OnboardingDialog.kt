package com.craftworks.music.ui.elements.dialogs.tv

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.elements.dialogs.OnboardingStep

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun OnboardingDialog(
    setShowDialog: (Boolean) -> Unit = { }
) {
    val backgroundColor = MaterialTheme.colorScheme.surface

    var step by remember { mutableStateOf(OnboardingStep.OVERVIEW) }

    var showNavidromeServerDialog by remember { mutableStateOf(false) }
    var showLocalFolderDialog by remember { mutableStateOf(false) }
    var showLrcLibEditDialog by remember { mutableStateOf(false) }


    Dialog(
        onDismissRequest = {  },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler {
            val prevStep = step.ordinal - 1
            if (prevStep >= 0)
                step = OnboardingStep.entries[prevStep]
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(color = backgroundColor) }
                .padding(horizontal = 48.dp, vertical = 48.dp),
        ) {
            AnimatedContent(
                step, transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    slideInHorizontally(
                        animationSpec = tween(350, easing = EaseInOutCubic),
                        initialOffsetX = { if (forward) it / 4 else -it / 4 }) + fadeIn(tween(350)) togetherWith slideOutHorizontally(
                        animationSpec = tween(350, easing = EaseInOutCubic),
                        targetOffsetX = { if (forward) -it / 4 else it / 4 }) + fadeOut(tween(350))
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (it) {
                        OnboardingStep.OVERVIEW -> {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_banner_foreground),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                            )

                            Text(
                                text = stringResource(R.string.No_Providers_Splash),
                                modifier = Modifier.width(320.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                            )

                            Button(
                                modifier = Modifier.width(320.dp),
                                onClick = {
                                    step = OnboardingStep.PROVIDER_SELECTION
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.Action_Next),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        OnboardingStep.PROVIDER_SELECTION -> {
                            OnboardingSetupProviders(
                                setDialogStep = { step = it },
                                showNavidromeServerDialog = { showNavidromeServerDialog = true },
                                showLocalFolderDialog = { showLocalFolderDialog = true }
                            )
                        }

                        OnboardingStep.DONE -> {
                            OnboardingDoneScreen {
                                setShowDialog(false)
                            }
                        }
                    }
                }
            }

            CarouselDefaults.IndicatorRow(
                itemCount = OnboardingStep.entries.size,
                activeItemIndex = step.ordinal,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if(showNavidromeServerDialog)
        CreateNavidromeProviderDialog(setShowDialog = { showNavidromeServerDialog = it })

    if(showLocalFolderDialog)
        CreateLocalProviderDialog(setShowDialog = { showLocalFolderDialog = it })
}

@Composable
private fun OnboardingSetupProviders(
    setDialogStep: (OnboardingStep) -> Unit = { },
    showNavidromeServerDialog: () -> Unit = { },
    showLocalFolderDialog: () -> Unit = { }
) {
    //val localProviders by LocalProviderManager.allFolders.collectAsStateWithLifecycle()
    //val navidromeServers by NavidromeManager.allServers.collectAsStateWithLifecycle()

    Text(
        text = stringResource(R.string.Dialog_Media_Source),
        modifier = Modifier.width(320.dp),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )

    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.Source_Local),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        /*items(localProviders, key = { it }) { local ->
            LocalProviderCard(local)
        }*/
        item {
            ListItem(
                selected = false,
                headlineContent = { Text(stringResource(R.string.Action_Add)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.Action_Login),
                    )
                },
                onClick = showLocalFolderDialog
            )
        }

        item {
            HorizontalDivider()
        }

        item {
            Text(
                text = stringResource(R.string.Source_Navidrome),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        /*items(navidromeServers, key = { it.id }) { server ->
            NavidromeProviderCard(server)
        }*/
        item {
            ListItem(
                selected = false,
                headlineContent = { Text(stringResource(R.string.Action_Add)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.Action_Login),
                    )
                },
                onClick = showNavidromeServerDialog
            )
        }
    }

    Button(
        modifier = Modifier.width(320.dp),
        onClick = {
            setDialogStep(OnboardingStep.DONE)
        }
    ) {
        Text(
            text = stringResource(R.string.Action_Next),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingDoneScreen(
    setFinished: () -> Unit = { }
) {
    Icon(
        ImageVector.vectorResource(R.drawable.ic_banner_foreground),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onBackground,
    )

    Text(
        text = stringResource(R.string.No_Providers_Done),
        modifier = Modifier.width(320.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Button(
        modifier = Modifier.width(320.dp),
        onClick = setFinished
    ) {
        Text(
            text = stringResource(R.string.Action_Go),
            textAlign = TextAlign.Center
        )
    }
}