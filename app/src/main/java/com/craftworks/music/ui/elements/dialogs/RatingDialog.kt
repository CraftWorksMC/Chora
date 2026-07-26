package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun RatingDialog(
    currentRating: Int = 0,
    onDismiss: () -> Unit = { },
    onSetRating: (Int) -> Unit = { }
) {
    var selectedRating by remember { mutableIntStateOf(currentRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (star in 1..5) {
                    IconButton(onClick = { selectedRating = star }) {
                        Icon(
                            imageVector = if (star <= selectedRating) Icons.Rounded.Star
                            else ImageVector.vectorResource(R.drawable.rounded_star_outline_24),
                            contentDescription = "Star $star",
                            tint = if (star <= selectedRating) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSetRating(selectedRating)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}