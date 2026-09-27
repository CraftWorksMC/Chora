package com.craftworks.music.ui.elements.dialogs.appearance

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.playing.NowPlayingBackground
import kotlinx.coroutines.runBlocking


@Preview(showBackground = true)
@Composable
fun PreviewBackgroundDialog(){
    BackgroundDialog(setShowDialog = { })
}
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun BackgroundDialog(setShowDialog: (Boolean) -> Unit) {
    val context = LocalContext.current

    val backgroundType by AppearanceSettingsManager(context).npBackgroundFlow.collectAsState(
        NowPlayingBackground.ANIMATED_BLUR)

    val backgroundTypeLabels = mapOf(
        NowPlayingBackground.PLAIN to R.string.background_style_plain,
        NowPlayingBackground.STATIC_BLUR to R.string.background_style_blur,
        NowPlayingBackground.ANIMATED_BLUR to R.string.background_style_anim
    )

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.appearance_background_style)) },
        text = {
            Column{
                NowPlayingBackground.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = (option == backgroundType),
                                onClick = {
                                    runBlocking {
                                        AppearanceSettingsManager(context).setBackgroundType(option)
                                    }
                                    setShowDialog(false)
                                },
                                role = Role.RadioButton,
                                enabled = !(option == NowPlayingBackground.ANIMATED_BLUR && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == backgroundType,
                            onClick = {
                                runBlocking {
                                    AppearanceSettingsManager(context).setBackgroundType(option)
                                }
                                setShowDialog(false)
                            },
                            modifier = Modifier.bounceClick(),
                            enabled = !(option == NowPlayingBackground.ANIMATED_BLUR && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                        )
                        Text(
                            text = stringResource(id = backgroundTypeLabels[option] ?: androidx.media3.session.R.string.error_message_invalid_state) +
                                    if (option == NowPlayingBackground.ANIMATED_BLUR && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                                        " (Android 13+)"
                                    else "",
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = { }
    )
}