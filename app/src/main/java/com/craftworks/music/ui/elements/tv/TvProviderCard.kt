package com.craftworks.music.ui.elements.tv

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Checkbox
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ListItemScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
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

// TODO("Make this working instead of erroring")
/*
@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
fun NavidromeProviderCard(
    server: NavidromeProvider = NavidromeProvider(
        "0",
        "https://demo.navidrome.org",
        "demo",
        "demo",
        enabled = true,
        allowSelfSignedCert = true
    )
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentServerId by NavidromeManager.currentServerId.collectAsStateWithLifecycle()
    val libraries by NavidromeManager.libraries.collectAsStateWithLifecycle()

    val checked by remember { derivedStateOf { server.id == currentServerId } }

    val (mainFocus, librariesFocus) = remember { FocusRequester.createRefs() }

    ListItem(
        modifier = Modifier
            .focusProperties {
                down =
                    if (libraries.size > 1 && server.id == currentServerId) librariesFocus else FocusRequester.Default
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
                text = server.username,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
        },
        supportingContent = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = server.url,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (libraries.size > 1 && server.id == currentServerId) {
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
                                    NavidromeManager.currentServerId.value?.let { serverId ->
                                        NavidromeManager.toggleServerLibraryEnabled(
                                            serverId,
                                            library.id,
                                            !isSelected
                                        )
                                    }
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
            coroutineScope.launch {
                if (!checked && NavidromeManager.getAllServers().size == 1)
                    NavidromeManager.setCurrentServer(null)
                else
                    NavidromeManager.setCurrentServer(server.id)
                AppearanceSettingsManager(context).setUsername(server.username)
            }
            Log.d("NAVIDROME", "Navidrome Current Server: ${server.id}")
        },
        onLongClick = {
            NavidromeManager.removeServer(server.id)
            DataRefreshManager.notifyDataSourcesChanged()
        }
    )
}

@Preview
@Composable
fun LocalProviderCard(
    folder: String = ""
) = ProviderItem(
    icon = R.drawable.s_m_local_filled,
    title = "Local",
    subtitle = folder,
    enabled = LocalProviderManager.getAllFolders().contains(folder),
    onClick = { },
    onLongClick = {
        LocalProviderManager.removeFolder(folder)
        DataRefreshManager.notifyDataSourcesChanged()
    }
)*/

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
                MediaProviderSettingsManager(context).setUseLrcLib(LyricsState.useLrcLib)
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
                MediaProviderSettingsManager(context).setUseNetEase(LyricsState.useNetEase)
            }
        },
    )
}