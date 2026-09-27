package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ListItemScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.providers.media.MediaProvider
import com.craftworks.music.data.providers.media.local.LocalMediaProvider
import com.craftworks.music.data.providers.media.subsonic.SubsonicMediaProvider
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.MediaProviderManager
import kotlinx.coroutines.launch

@Composable
private fun ProviderItem(
    icon: Int,
    title: String,
    subtitle: String,
    trailingContent: @Composable () -> Unit = { },
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = { }
) {
    ListItem(
        selected = enabled,
        scale = ListItemScale.None,
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ListItemDefaults.IconSize)
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingContent = {
            Row {
                trailingContent()

                Checkbox(
                    checked = enabled,
                    onCheckedChange = { }
                )
            }
        },
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvMaterial3Api::class)
@Composable
fun TvProviderCard(provider: MediaProvider) {
    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()

    val libraries = if (provider == currentProvider) {
        currentProvider?.data?.libraries ?: emptyList()
    } else {
        provider.data?.libraries ?: emptyList()
    }

    val checked by remember { derivedStateOf { provider == currentProvider } }

    val (mainFocus, librariesFocus) = remember { FocusRequester.createRefs() }

    ListItem(
        modifier = Modifier
            .focusProperties {
                down =
                    if (libraries.size > 1 && provider == currentProvider) librariesFocus else FocusRequester.Default
            }
            .focusRequester(mainFocus),
        selected = checked,
        scale = ListItemScale.None,
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.s_m_navidrome),
                contentDescription = null,
                modifier = Modifier.size(ListItemDefaults.IconSize)
            )
        },
        headlineContent = {
            Text(
                text = stringResource(provider.providerName),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
        },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when (provider) {
                        is LocalMediaProvider -> provider.data.libraries.joinToString(", ") { it.first.name }
                        is SubsonicMediaProvider -> provider.providerData.url
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (libraries.size > 1 && provider == currentProvider) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .focusRestorer()
                            .focusGroup()
                            .focusRequester(librariesFocus)
                            .focusProperties {
                                up = mainFocus
                            }
                    ) {
                        libraries.forEach { (library, isSelected) ->
                            FilterChip(
                                onClick = {
                                    MediaProviderManager.setProviderLibraries(
                                        provider.id,
                                        libraries = libraries.map { (currentLibrary, currentEnabled) ->
                                            if (currentLibrary.id == library.id) {
                                                Pair(library, !isSelected)
                                            } else {
                                                Pair(currentLibrary, currentEnabled)
                                            }
                                        }
                                    )
                                },
                                content = {
                                    Text(library.name)
                                },
                                leadingIcon =
                                    if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Done,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                selected = isSelected,
                            )
                        }
                    }
                }
            }
        },
        trailingContent = {
            Row {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { }
                )
            }
        },
        onClick = {
            MediaProviderManager.setCurrentProvider(provider)
        },
        onLongClick = {
            MediaProviderManager.removeProvider(provider.id)
            DataRefreshManager.notifyDataSourcesChanged()
        }
    )
}

@Composable
fun LrcLibProviderCard(
    url: String,
    onLongClick: () -> Unit = { }
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    ProviderItem(
        icon = R.drawable.lrclib_logo,
        title = "LRCLIB",
        subtitle = url,
        enabled = LyricsState.useLrcLib,
        onClick = {
            LyricsState.useLrcLib = !LyricsState.useLrcLib
            coroutineScope.launch {
                //TODO: FIX TV LYRICS PROVIDERS
                //MediaProviderSettingsManager(context).setUseLrcLib(LyricsState.useLrcLib)
            }
        },
        onLongClick = onLongClick
    )
}

@Preview
@Composable
fun NetEaseProviderCard() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    ProviderItem(
        icon = R.drawable.netease_cloud_music,
        title = "NetEase",
        subtitle = "Lyrics",
        enabled = LyricsState.useNetEase,
        onClick = {
            LyricsState.useNetEase = !LyricsState.useNetEase
            coroutineScope.launch {
                //TODO: FIX TV LYRICS PROVIDERS
                //MediaProviderSettingsManager(context).setUseNetEase(LyricsState.useNetEase)
            }
        },
    )
}