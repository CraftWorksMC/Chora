package com.craftworks.music.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.craftworks.music.R
import com.craftworks.music.data.Radio
import com.craftworks.music.navidrome.createNavidromeRadioStation
import com.craftworks.music.navidrome.deleteNavidromeRadioStation
import com.craftworks.music.navidrome.modifyNavidromeRadoStation
import com.craftworks.music.playingSong
import com.craftworks.music.songState
import com.craftworks.music.ui.elements.RadiosGrid

var radioList:MutableList<Radio> = mutableStateListOf()
var showRadioAddDialog = mutableStateOf(false)
var showRadioModifyDialog = mutableStateOf(false)
var selectedRadioIndex = mutableIntStateOf(0)
@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RadioScreen() {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    /* RADIO ICON + TEXT */
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding)) {
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

        Column(modifier = Modifier.padding(12.dp,24.dp,12.dp,12.dp)) {
            RadiosGrid(radioList, onSongSelected = { song ->
                playingSong.selectedSong = song
                songState = true })
        }
    }

    if(showRadioAddDialog.value)
        AddRadioDialog(setShowDialog =  { showRadioAddDialog.value = it } )
    if(showRadioModifyDialog.value)
        ModifyRadioDialog(setShowDialog = { showRadioModifyDialog.value = it }, radio = radioList[selectedRadioIndex.intValue])
}

// DIALOGS
@Composable
fun AddRadioDialog(setShowDialog: (Boolean) -> Unit) {
    var radioName by remember { mutableStateOf("") }
    var radioUrl by remember { mutableStateOf("") }
    var radioPage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Internet Radio",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable { setShowDialog(false) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = radioName,
                        onValueChange = { radioName = it },
                        label = { Text("Radio Name:") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioUrl,
                        onValueChange = { radioUrl = it },
                        label = { Text("Stream URL:") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioPage,
                        onValueChange = { radioPage = it },
                        label = { Text("Homepage URL") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
                        Button(
                            onClick = {
                                if (radioName.isBlank() && radioUrl.isBlank()) return@Button
                                setShowDialog(false)
                                radioList.add(
                                    Radio(
                                        name = radioName,
                                        imageUrl = Uri.parse("$radioUrl/favicon.ico"),
                                        media = Uri.parse(radioUrl),
                                        homepageUrl = radioPage
                                    )
                                )
                                if (useNavidromeServer.value) createNavidromeRadioStation(radioName,radioUrl,radioPage)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(text = "Done")
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ModifyRadioDialog(setShowDialog: (Boolean) -> Unit, radio: Radio) {
    var radioName by remember { mutableStateOf(radio.name) }
    var radioUrl by remember { mutableStateOf(radio.media.toString()) }
    var radioPage by remember { mutableStateOf(radio.homepageUrl) }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modify $radioName",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable { setShowDialog(false) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = radioName,
                        onValueChange = { radioName = it },
                        label = { Text("Radio Name:") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioUrl,
                        onValueChange = { radioUrl = it },
                        label = { Text("Stream URL:") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioPage,
                        onValueChange = { radioPage = it },
                        label = { Text("Homepage URL") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                setShowDialog(false)
                                radioList.remove(radio)
                                if (useNavidromeServer.value) radio.navidromeID?.let { deleteNavidromeRadioStation(it) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(128.dp)
                                .height(50.dp)
                        ) {
                            Text(text = "Remove")
                        }
                        Button(
                            onClick = {
                                if (radioName.isBlank() && radioUrl.isBlank()) return@Button
                                if (useNavidromeServer.value) radio.navidromeID?.let { modifyNavidromeRadoStation(it, radioName, radioUrl, radioPage) }
                                setShowDialog(false)
                                if (useNavidromeServer.value) {
                                    radioList.clear()
                                    //getNavidromeRadios()
                                }
                                else {
                                    radioList.remove(radio)
                                    radioList.add(
                                        Radio(
                                            name = radioName,
                                            imageUrl = Uri.parse("$radioUrl/favicon.ico"),
                                            media = Uri.parse(radioUrl)
                                        ))
                                } },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(128.dp)
                                .height(50.dp)
                        ) {
                            Text(text = "Done")
                        }
                    }
                }
            }
        }
    }
}