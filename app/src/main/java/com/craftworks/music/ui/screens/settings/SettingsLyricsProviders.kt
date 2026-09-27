package com.craftworks.music.ui.screens.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import com.craftworks.music.ui.elements.LyricsProviderCard
import com.craftworks.music.ui.elements.dialogs.EditLrcLibUrlDialog
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_LyricsProviderScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext

    val settingsManager = MediaProviderSettingsManager(context)
    val providers by settingsManager.lyricProvidersFlow.collectAsStateWithLifecycle(emptyList())

    var showEditLrcLibUrl by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_lyrics_providers)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navHostController.popBackStack()
                        },
                        modifier = Modifier.size(56.dp, 70.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Previous Song",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
    ) { innerPadding ->
        Box (
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding()
                )
                .fillMaxSize()
        ) {
            val lazyListState = rememberLazyListState()
            val reorderableLazyColumnState =
                rememberReorderableLazyListState(lazyListState) { from, to ->
                    settingsManager.setLyricProviders(providers.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    })
                }
            LazyColumn (
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                state = lazyListState,
                contentPadding = PaddingValues(12.dp)
            ) {
                itemsIndexed(
                    items = providers,
                    key = { index, provider -> provider.source}
                ) { index, provider ->
                    val topCornerRadius = animateDpAsState(
                        if (index == 0) 16.dp else 4.dp
                    ).value
                    val bottomCornerRadius = animateDpAsState(
                        if (index == providers.size - 1) 16.dp else 4.dp
                    ).value

                    ReorderableItem(
                        reorderableLazyColumnState,
                        key = provider.source,
                        animateItemModifier = Modifier.animateItem(
                            placementSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                        )
                    ) {
                        LyricsProviderCard(
                            provider,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    settingsManager.setLyricProviders(
                                        providers.map { p ->
                                            if (p == provider) p.copy(enabled = checked) else p
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.clip(
                                RoundedCornerShape(topCornerRadius, topCornerRadius, bottomCornerRadius, bottomCornerRadius)
                            ),
                            onEditClick = {
                                showEditLrcLibUrl = true
                            }
                        )
                    }
                }
            }
        }
    }

    if(showEditLrcLibUrl)
        EditLrcLibUrlDialog(setShowDialog = { showEditLrcLibUrl = it })
}