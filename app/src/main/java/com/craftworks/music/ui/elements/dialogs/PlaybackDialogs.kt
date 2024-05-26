package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.craftworks.music.R
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.ui.elements.bounceClick

//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewTranscodingDialog(){
    TranscodingDialog(setShowDialog = { })
}
//endregion

val transcodingBitrateList = listOf(
    "1",
    "96",
    "128",
    "192",
    "256",
    "320",
    "No Transcoding"
)
var transcodingBitrate = mutableStateOf(transcodingBitrateList[6])


@Composable
fun TranscodingDialog(setShowDialog: (Boolean) -> Unit) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Setting_Transcoding),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    for (bitrate in transcodingBitrateList) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                .height(48.dp)
                        ) {
                            RadioButton(
                                selected = bitrate == transcodingBitrate.value,
                                onClick = {
                                    transcodingBitrate.value = bitrate
                                    getNavidromeSongs()
                                    setShowDialog(false)
                                },
                                modifier = Modifier
                                    .semantics { contentDescription = bitrate }
                                    .bounceClick()
                            )
                            Text(
                                text = if (bitrate != "No Transcoding") "$bitrate Kbps" else bitrate,
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}