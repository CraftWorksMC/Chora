package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.craftworks.music.R
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.elements.bounceClick
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewTranscodingDialog(){
    TranscodingDialog(setShowDialog = { })
}
//endregion

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun TranscodingDialog(setShowDialog: (Boolean) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val transcodingBitrate by SettingsManager(context).transcodingBitrateFlow.collectAsState("")
    val transcodingBitrateList = listOf(
        "1",
        "96",
        "128",
        "192",
        "256",
        "320",
        "No Transcoding"
    )

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable()
                .selectableGroup()
        ) {
            Text(
                text = stringResource(R.string.Setting_Transcoding),
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            for (bitrate in transcodingBitrateList) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = (bitrate == transcodingBitrate),
                            onClick = {
                                runBlocking {
                                    SettingsManager(context).setTranscodingBitrate(bitrate)
                                }
                                setShowDialog(false)
                            },
                            role = Role.RadioButton
                        ),
                ) {
                    RadioButton(
                        selected = bitrate == transcodingBitrate,
                        onClick = { },
                        modifier = Modifier.bounceClick()
                    )
                    Text(
                        text = if (bitrate != "No Transcoding") "$bitrate Kbps" else stringResource(R.string.Option_No_Transcoding),
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}