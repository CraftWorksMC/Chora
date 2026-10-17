package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ListItemScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.LyricsProvider
import com.craftworks.music.data.providers.media.MediaProvider
import com.craftworks.music.data.providers.media.local.LocalMediaProvider
import com.craftworks.music.data.providers.media.subsonic.SubsonicMediaProvider
import com.craftworks.music.managers.DataRefreshManager
import com.craftworks.music.managers.MediaProviderManager

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
fun TvLyricsProviderCard(
    provider: LyricsProvider,
    onCheckedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditClick: (() -> Unit) = { }
) {
    val (itemFocus, upFocus, downFocus) = remember { FocusRequester.createRefs() }

    ListItem(
        modifier = Modifier
            .focusRequester(itemFocus)
            .focusProperties {
                right = upFocus
            },
        selected = provider.enabled,
        scale = ListItemScale.None,
        leadingContent = {
            Image(
                painter = painterResource(provider.source.icon),
                contentDescription = null,
                modifier = Modifier.size(ListItemDefaults.IconSize)
            )
        },
        headlineContent = {
            Text(
                text = provider.source.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
        },
        trailingContent = {
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.focusRequester(upFocus)
                        .focusProperties{
                            left = itemFocus
                            right = downFocus
                        },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.focusRequester(downFocus)
                        .focusProperties {
                            left = upFocus
                            right = itemFocus
                        },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null
                    )
                }

                Checkbox(
                    checked = provider.enabled,
                    onCheckedChange = { }
                )
            }
        },
        onClick = { onCheckedChange(!provider.enabled) },
        onLongClick = {
            if (provider.source == LyricSource.LRCLIB)
                onEditClick()
        }
    )
}