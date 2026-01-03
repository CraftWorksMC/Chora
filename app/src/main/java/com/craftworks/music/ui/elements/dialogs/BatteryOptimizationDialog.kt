package com.craftworks.music.ui.elements.dialogs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.craftworks.music.R
import com.craftworks.music.managers.BatteryOptimizationManager
import kotlinx.coroutines.launch

@Composable
fun BatteryOptimizationDialog(
    setShowDialog: (Boolean) -> Unit,
    onRequestDisable: () -> Unit,
    context: Context = LocalContext.current
) {
    val batteryManager = BatteryOptimizationManager(context)
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.round_battery_alert_24),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Battery Optimization",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Chora is being restricted by battery optimization, which causes playback to stop when the screen is off.\n\nDisable battery optimization to allow uninterrupted background playback.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            batteryManager.dismissPrompt()
                        }
                        setShowDialog(false)
                    }
                ) {
                    Text("Not Now")
                }

                Button(
                    onClick = {
                        onRequestDisable()
                        setShowDialog(false)
                    }
                ) {
                    Text("Disable")
                }
            }
        }
    }
}
