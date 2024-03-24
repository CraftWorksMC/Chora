package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.SongHelper
import com.craftworks.music.data.radioList
import com.craftworks.music.providers.getIcecastMetadata
import com.craftworks.music.providers.navidrome.getNavidromeRadios
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.ui.elements.AddRadioDialog
import com.craftworks.music.ui.elements.ModifyRadioDialog
import com.craftworks.music.ui.elements.RadiosGrid

var showRadioAddDialog = mutableStateOf(false)
var showRadioModifyDialog = mutableStateOf(false)
var selectedRadioIndex = mutableIntStateOf(0)
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RadioScreen() {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    if (radioList.isEmpty() && useNavidromeServer.value) getNavidromeRadios()
    val context = LocalContext.current

    /* RADIO ICON + TEXT */
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding,
            top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()
        )) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_radio),
                contentDescription = "Songs Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.radios),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { showRadioAddDialog.value = true },
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.rounded_add_24),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Previous Song",
                    modifier = Modifier
                        .height(32.dp)
                        .size(32.dp)
                )
            }
        }
        Divider(
            modifier = Modifier.padding(12.dp,56.dp,12.dp,0.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(modifier = Modifier
            .padding(12.dp,24.dp,12.dp,12.dp)
            ) {
            RadiosGrid(radioList, onSongSelected = { song ->
                if (song.media.toString().endsWith("m3u8"))
                    return@RadiosGrid

                SongHelper.currentSong = song
                song.media?.let { SongHelper.playStream(context, it, true) }
                // Get Metadata
                val icecastUrl = "${song.media}/status-json.xsl"
                Thread{
                    try {
                        val metadata = getIcecastMetadata(icecastUrl)
                        println(metadata)
                    }catch (_: Exception){
                    }
                }.start()
            })
        }

    }

    if(showRadioAddDialog.value)
        AddRadioDialog(setShowDialog =  { showRadioAddDialog.value = it } )
    if(showRadioModifyDialog.value)
        ModifyRadioDialog(setShowDialog = { showRadioModifyDialog.value = it }, radio = radioList[selectedRadioIndex.intValue])
}