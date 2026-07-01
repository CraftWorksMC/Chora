package com.craftworks.music.ui.screens.tv.settings

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import com.craftworks.music.ui.elements.dialogs.tv.CreateLocalProviderDialog
import com.craftworks.music.ui.elements.dialogs.tv.CreateNavidromeProviderDialog
import com.craftworks.music.ui.elements.dialogs.tv.ModifyLrcLibProviderDialog
import com.craftworks.music.ui.elements.tv.LrcLibProviderCard
import com.craftworks.music.ui.elements.tv.NetEaseProviderCard

@Composable
fun TvS_ProviderScreen() {
    val context = LocalContext.current.applicationContext

    // TODO("Update providers settings")

    val providers by MediaProviderManager.allProviders.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Navidrome", "Folders", "Lyrics")

    var showNavidromeServerDialog by remember { mutableStateOf(false) }
    var showLocalFolderDialog by remember { mutableStateOf(false) }
    var showLrcLibEditDialog by remember { mutableStateOf(false) }

    val lrclibUrl by MediaProviderSettingsManager(context).lrcLibEndpointFlow.collectAsStateWithLifecycle("")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TabRow(
            modifier = Modifier.fillMaxWidth().focusGroup().focusRestorer(),
            selectedTabIndex = selectedTab
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onFocus = { selectedTab = index },
                    onClick = { selectedTab = index },
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                /*items(providers, key = { it.first }) { server ->
                    NavidromeProviderCard(server)
                }*/
                item {
                    ListItem(
                        selected = false,
                        onClick = {
                            showNavidromeServerDialog = true
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        },
                        headlineContent = {
                            Text(stringResource(R.string.Action_Add))
                        }
                    )
                }
            }

            /*1 -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(localProviders, key = { it }) { local ->
                    LocalProviderCard(local)
                }
                item {
                    ListItem(
                        selected = false,
                        onClick = {
                            showLocalFolderDialog = true
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        },
                        headlineContent = {
                            Text(stringResource(R.string.Action_Add))
                        }
                    )
                }
            }*/

            2 -> Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LrcLibProviderCard(lrclibUrl) {
                    showLrcLibEditDialog = true
                }
                NetEaseProviderCard()
            }
        }
    }

    if(showNavidromeServerDialog)
        CreateNavidromeProviderDialog(setShowDialog = { showNavidromeServerDialog = it })

    if(showLocalFolderDialog)
        CreateLocalProviderDialog(setShowDialog = { showLocalFolderDialog = it })

    if(showLrcLibEditDialog)
        ModifyLrcLibProviderDialog(
            initialUrl = lrclibUrl,
            setShowDialog = { showLrcLibEditDialog = it }
        )
}