package com.craftworks.music.ui.elements.dialogs.appearance

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.playing.NowPlayingAlignment

@Composable
@Preview
fun NowPlayingTitleAlignmentDialog(
    setShowDialog: (Boolean) -> Unit = { },
    title: String = "",
    selection: NowPlayingAlignment = NowPlayingAlignment.LEFT,
    onSet: (NowPlayingAlignment) -> Unit = { }
) {
    val nowPlayingTitleAlignment by remember { mutableStateOf(selection) }

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(title) },
        text = {
            Column {
                NowPlayingAlignment.entries.forEach { alignment ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = (alignment == nowPlayingTitleAlignment),
                                onClick = {
                                    onSet(alignment)
                                    setShowDialog(false)
                                },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = alignment == nowPlayingTitleAlignment,
                            onClick = {
                                onSet(alignment)
                            },
                            modifier = Modifier.bounceClick()
                        )
                        val alignmentStringRes = when (alignment) {
                            NowPlayingAlignment.LEFT -> R.string.alignment_setting_left
                            NowPlayingAlignment.CENTER -> R.string.alignment_setting_center
                            NowPlayingAlignment.RIGHT -> R.string.alignment_setting_right
                        }

                        Text(
                            text = stringResource(id = alignmentStringRes),
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