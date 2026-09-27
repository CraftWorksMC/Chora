package com.craftworks.music.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.settings.MiscSettingsManager
import com.craftworks.music.ui.elements.dialogs.DownloadTemplateDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable
import kotlinx.coroutines.launch

@Preview(showSystemUi = false, showBackground = true)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun S_MiscScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current

    var showDownloadTemplateDialog by remember { mutableStateOf(false) }
    var showPlaylistDownloadTemplateDialog by remember { mutableStateOf(false) }

    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_misc)) },
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
                .verticalScroll(rememberScrollState())
                .dialogFocusable()
        ) {
            Column(
                Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val downloadTemplate by MiscSettingsManager(context).downloadTemplateFlow.collectAsState("{album}/{title} - {artist}.{ext}")

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.misc_download_template),
                        settingsSubtitle = downloadTemplate,
                        settingsIcon = ImageVector.vectorResource(R.drawable.rounded_download_24),
                        toggleEvent = { showDownloadTemplateDialog = true },
                        enabled = currentProvider != null
                    )

                    val playlistDownloadTemplate by MiscSettingsManager(context).playlistDownloadTemplateFlow.collectAsState("{playlist}/{playlist_index}. {title} - {artist}.{ext}")

                    SettingsDialogButton(
                        settingsName = stringResource(R.string.misc_playlist_download_template),
                        settingsSubtitle = playlistDownloadTemplate,
                        settingsIcon = ImageVector.vectorResource(R.drawable.rounded_download_24),
                        toggleEvent = { showPlaylistDownloadTemplateDialog = true },
                        enabled = currentProvider != null
                    )
                }
            }
        }

        if (showDownloadTemplateDialog)
            DownloadTemplateDialog(
                onDismissRequest = {
                    showDownloadTemplateDialog = false
                },
                onConfirm = {
                    coroutineScope.launch {
                        MiscSettingsManager(context).setDownloadTemplate(it)
                    }
                },
            )
        if (showPlaylistDownloadTemplateDialog)
            DownloadTemplateDialog(
                onDismissRequest = {
                    showPlaylistDownloadTemplateDialog = false
                },
                onConfirm = {
                    coroutineScope.launch {
                        MiscSettingsManager(context).setPlaylistDownloadTemplate(it)
                    }
                },
                template = MiscSettingsManager(context).playlistDownloadTemplateFlow,
                R.string.misc_playlist_download_template,
                R.string.misc_playlist_download_template_description
            )
    }
}