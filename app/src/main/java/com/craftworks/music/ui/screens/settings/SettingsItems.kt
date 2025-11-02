package com.craftworks.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import kotlinx.coroutines.runBlocking

@Composable
fun SettingsSwitch(
    selected: Boolean,
    settingsName: String,
    settingsIcon: ImageVector,
    toggleEvent: () -> Unit = {},
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.background)
            .selectable(
                selected = selected,
                onClick = toggleEvent,
                role = Role.RadioButton,
                enabled = enabled
            )
    ) {
        Icon(
            imageVector = settingsIcon,
            contentDescription = "Settings Icon",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .size(32.dp)
        )
        Text(
            text = settingsName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(vertical = 20.dp)
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        Switch(
            checked = selected,
            onCheckedChange = { toggleEvent() },
            enabled = enabled,
            modifier = Modifier.padding(end = 20.dp)
        )
    }
}

@Composable
fun SettingsDialogButton(
    settingsName: String,
    settingsSubtitle: String,
    settingsIcon: ImageVector,
    enabled: Boolean? = true,
    toggleEvent: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (enabled == true)
                    MaterialTheme.colorScheme.background
                else
                    MaterialTheme.colorScheme.surfaceContainerLow
            )
            .fillMaxWidth()
            .clickable(enabled = enabled == true) {
                toggleEvent()
            }
            .focusProperties { left = FocusRequester.Cancel }
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
    ) {
        Icon(
            imageVector = settingsIcon,
            contentDescription = "Settings Icon",
            tint = if (enabled == true)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .size(32.dp)
        )
        Column(
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = settingsName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = if (enabled == true)
                    MaterialTheme.colorScheme.onBackground
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )

            Text(
                text = settingsSubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (enabled == true)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
@Preview(showSystemUi = true, showBackground = true)
fun preview() {
    Column(
        Modifier.background(MaterialTheme.colorScheme.surfaceContainer).fillMaxSize()
    ) {
        SettingsDialogButton(
            settingsName = stringResource(R.string.Setting_Transcoding_Format),
            settingsSubtitle = "transcodingFormat",
            settingsIcon = ImageVector.vectorResource(R.drawable.s_p_transcoding),
            enabled = true,
            toggleEvent = {  }
        )

        SettingsDialogButton(
            settingsName = stringResource(R.string.Setting_Transcoding_Format),
            settingsSubtitle = "transcodingFormat",
            settingsIcon = ImageVector.vectorResource(R.drawable.s_p_transcoding),
            enabled = false,
            toggleEvent = {  }
        )
    }
}

@Composable
fun SettingsSlider(
    settingsName: String,
    steps: Int,
    value: Float,
    minValue: Float, maxValue: Float,
    onValueChange: (newValue: Float) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column (
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = settingsName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 6.dp).padding(top = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        Slider(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
                .onKeyEvent { keyEvent ->
                    when (keyEvent.key) {
                        Key.DirectionRight if keyEvent.type == KeyEventType.KeyDown -> {
                            runBlocking {
                                onValueChange(value + ((maxValue - minValue) / steps).coerceAtMost(maxValue))
                            }
                            true
                        }
                        Key.DirectionLeft if keyEvent.type == KeyEventType.KeyDown -> {
                            runBlocking {
                                onValueChange(value - ((maxValue - minValue) / steps).coerceAtLeast(minValue))
                            }
                            true
                        }
                        else -> false
                    }
                },
            interactionSource = interactionSource,
            value = value,
            steps = steps,
            onValueChange = {
                onValueChange(it)
            },
            valueRange = minValue..maxValue
        )
    }
}