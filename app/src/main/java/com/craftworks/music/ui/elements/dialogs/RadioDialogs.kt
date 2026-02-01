package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import com.craftworks.music.R
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.ui.elements.bounceClick

@Composable
fun AddRadioDialog(
    setShowDialog: (Boolean) -> Unit,
    onAdded: (name: String, url: String, homePageUrl: String, addToNavidrome: Boolean) -> Unit
) {
    var radioName by remember { mutableStateOf("") }
    var radioUrl by remember { mutableStateOf("") }
    var radioPage by remember { mutableStateOf("") }

    var addToNavidrome by remember { mutableStateOf(
        NavidromeManager.checkActiveServers()
    ) }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.Dialog_Add_Radio),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .clickable { setShowDialog(false) }
                        )
                    }

                    OutlinedTextField(
                        value = radioName,
                        onValueChange = { radioName = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioUrl,
                        onValueChange = { radioUrl = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_URL)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioPage,
                        onValueChange = { radioPage = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Homepage)) },
                        singleLine = true
                    )

                    if (NavidromeManager.checkActiveServers()) {
                        Row (
                            modifier = Modifier.selectable(
                                selected = addToNavidrome,
                                onClick = {
                                    addToNavidrome = !addToNavidrome
                                },
                                role = Role.Checkbox,
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = addToNavidrome,
                                onCheckedChange = { addToNavidrome = it }
                            )

                            Text(
                                text = stringResource(R.string.Label_Radio_Add_To_Navidrome),
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
                        Button(
                            onClick = {
                                if (radioName.isBlank() && radioUrl.isBlank()) return@Button

                                onAdded(
                                    radioName,
                                    radioUrl,
                                    radioPage,
                                    addToNavidrome
                                )

                                setShowDialog(false)
                            },
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Action_Done))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModifyRadioDialog(
    setShowDialog: (Boolean) -> Unit,
    radio: MediaItem?,
    onModified: (id: String, name: String, url: String, homepage: String) -> Unit,
    onDeleted: (id: String) -> Unit = {}
) {
    var radioName by remember { mutableStateOf(radio?.mediaMetadata?.station) }
    var radioUrl by remember { mutableStateOf(radio?.mediaId) }
    var radioPage by remember { mutableStateOf(radio?.mediaMetadata?.extras?.getString("homepage") ?: "") }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.Dialog_Modify_Radio) + radioName,
                            style = MaterialTheme.typography.titleLarge
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

                    OutlinedTextField(
                        value = radioName.toString(),
                        onValueChange = { radioName = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioUrl.toString(),
                        onValueChange = { radioUrl = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_URL)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = radioPage,
                        onValueChange = { radioPage = it },
                        label = { Text(stringResource(id = R.string.Label_Radio_Homepage)) },
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                setShowDialog(false)
                                onDeleted(radio?.mediaMetadata?.extras?.getString("navidromeID") ?: "null")
                            },
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .weight(1f)
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Action_Remove))
                        }
                        Button(
                            onClick = {
                                setShowDialog(false)

                                onModified(
                                    radio?.mediaMetadata?.extras?.getString("navidromeID") ?: "Local",
                                    radioName.toString(),
                                    radioUrl.toString(),
                                    radioPage
                                )
                            },
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .weight(1f)
                                .height(50.dp)
                                .bounceClick()
                        ) {
                            Text(stringResource(R.string.Action_Done))
                        }
                    }
                }
            }
        }
    }
}