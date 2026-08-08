package com.craftworks.music.ui.elements

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.R
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import com.craftworks.music.data.providers.media.MediaProvider
import com.craftworks.music.data.providers.media.local.LocalMediaProvider
import com.craftworks.music.data.providers.media.local.LocalProviderData
import com.craftworks.music.data.providers.media.subsonic.SubsonicMediaProvider
import com.craftworks.music.ui.elements.dialogs.EditLrcLibUrlDialog
import kotlinx.coroutines.runBlocking

@Preview
@Composable
fun ProviderCard(
    provider: MediaProvider = LocalMediaProvider(
        LocalProviderData("")
    )
) {
    val currentProvider by MediaProviderManager.currentProvider.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appearanceSettingsManager = AppearanceSettingsManager(context)

    Row(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceBright),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider Icon
        Icon(
            imageVector = ImageVector.vectorResource(provider.providerIcon),
            tint = if (provider.providerMonochromeIcon) MaterialTheme.colorScheme.primary else Color.Unspecified,
            contentDescription = "Provider Icon",
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .height(32.dp)
                .size(32.dp)
        )

        // Provider Name
        Column(modifier = Modifier
            .weight(1f)
            .padding(vertical = 10.dp)) {
            Text(
                text = stringResource(provider.providerName),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
            Text(
                text = when (provider) {
                    is LocalMediaProvider -> provider.data.libraries.joinToString(", ") { it.first.name }
                    is SubsonicMediaProvider -> provider.providerData.url
                    else -> ""
                },
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Make current Button
        RadioButton(
            selected = currentProvider == provider,
            onClick = {
                MediaProviderManager.setCurrentProvider(provider)
                runBlocking {
                    if (provider is SubsonicMediaProvider)
                        appearanceSettingsManager.setUsername(provider.providerData.username)
                }
            },
            modifier = Modifier
                .size(32.dp),
        )

        // Delete Button
        IconButton(
            onClick = {
                MediaProviderManager.removeProvider(provider.id)
                runBlocking {
                    if (currentProvider is SubsonicMediaProvider)
                        appearanceSettingsManager.setUsername((currentProvider as SubsonicMediaProvider).providerData.username)
                }
            },
            shape = CircleShape,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete Provider",
                modifier = Modifier
            )
        }

        Spacer(Modifier.width(12.dp))
    }
}

@Preview
@Composable
fun LRCLIBProviderCard(
    context: Context = LocalContext.current
) {
    var showEditDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider Icon
        Image(
            painter = painterResource(R.drawable.lrclib_logo),
            contentDescription = "LRCLIB.net logo",
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .size(32.dp)
        )
        // Provider Name
        Column(modifier = Modifier
            .weight(1f)
            .padding(vertical = 10.dp)) {
            Text(
                text = "Lyrics",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
            Text(
                text = "LRCLIB.net",
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Edit Button
        IconButton(
            onClick = { showEditDialog = true },
            shape = CircleShape,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Edit LRCLIB Endpoint",
            )
        }

        // Enabled Checkbox
        Checkbox(
            checked = LyricsState.useLrcLib,
            onCheckedChange = {
                LyricsState.useLrcLib = it
                runBlocking {
                    MediaProviderSettingsManager(context).setUseLrcLib(it)
                }
            }
        )

        Spacer(Modifier.width(12.dp))
    }

    if (showEditDialog)
        EditLrcLibUrlDialog(setShowDialog = { showEditDialog = it })
}

@Preview
@Composable
fun NetEaseProviderCard(
    context: Context = LocalContext.current
) {
    var showEditDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider Icon
        Image(
            painter = painterResource(R.drawable.netease_cloud_music),
            contentDescription = "NetEase logo",
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .size(32.dp)
        )
        // Provider Name
        Column(modifier = Modifier
            .weight(1f)
            .padding(vertical = 10.dp)) {
            Text(
                text = "Lyrics",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
            Text(
                text = "NetEase",
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Enabled Checkbox
        Checkbox(
            checked = LyricsState.useNetEase,
            onCheckedChange = {
                LyricsState.useNetEase = it
                runBlocking {
                    MediaProviderSettingsManager(context).setUseNetEase(it)
                }
            }
        )

        Spacer(Modifier.width(12.dp))
    }
}