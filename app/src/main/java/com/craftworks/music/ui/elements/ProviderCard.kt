package com.craftworks.music.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.craftworks.music.data.model.LyricSource
import com.craftworks.music.data.model.LyricsProvider
import com.craftworks.music.data.providers.media.MediaProvider
import com.craftworks.music.data.providers.media.local.LocalMediaProvider
import com.craftworks.music.data.providers.media.local.LocalProviderData
import com.craftworks.music.data.providers.media.subsonic.SubsonicMediaProvider
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import kotlinx.coroutines.runBlocking
import sh.calvin.reorderable.ReorderableCollectionItemScope

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

@Composable
fun ReorderableCollectionItemScope.LyricsProviderCard(
    provider: LyricsProvider,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit) = { }
) {
    val type = provider.source

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(type.icon),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(32.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = type.displayName,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (provider.source == LyricSource.LRCLIB) {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${type.displayName} URL"
                )
            }
        }

        Checkbox(
            checked = provider.enabled,
            onCheckedChange = onCheckedChange
        )

        IconButton(
            modifier = Modifier.draggableHandle(
                onDragStarted = {
                },
                onDragStopped = {
                }
            ),
            onClick = {},
        ) {
            Icon(
                ImageVector.vectorResource(R.drawable.baseline_drag_handle_24),
                contentDescription = "Reorder"
            )
        }
    }
}