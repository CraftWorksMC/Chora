package com.craftworks.music.ui.elements.dialogs.tv.provider

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.ProviderType

@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun SelectProviderType(
    setShowDialog: (Boolean) -> Unit = { },
    selectProvider: (ProviderType) -> Unit = { }
) {
    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.settings_media_providers),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                item {
                    ListItem(
                        selected = false,
                        leadingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.s_m_opensubsonic_bw),
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            Text(text = "OpenSubsonic/Navidrome")
                        },
                        onClick = { selectProvider(ProviderType.SUBSONIC) },
                    )
                }

                item {
                    ListItem(
                        selected = false,
                        leadingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.s_m_local_filled),
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            Text(text = stringResource(R.string.source_local_folder))
                        },
                        onClick = { selectProvider(ProviderType.LOCAL_FOLDER) },
                    )
                }
            }
        },
        confirmButton = {}
    )
}