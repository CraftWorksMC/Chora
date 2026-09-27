package com.craftworks.music.ui.elements.dialogs.tv.provider

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.craftworks.music.data.model.ProviderType

@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun TvCreateMediaProviderDialog(
    setShowDialog: (Boolean) -> Unit = { },
) {
    var selectedProvider:ProviderType? by remember { mutableStateOf(null) }

    when (selectedProvider) {
        null -> SelectProviderType(setShowDialog) { selectedProvider = it }

        ProviderType.SUBSONIC -> CreateSubsonicProviderDialog(setShowDialog)
        ProviderType.NAVIDROME -> CreateSubsonicProviderDialog(setShowDialog)

        ProviderType.LOCAL_FOLDER -> CreateLocalProviderDialog(setShowDialog)
    }
}