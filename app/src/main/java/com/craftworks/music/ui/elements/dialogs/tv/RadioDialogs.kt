package com.craftworks.music.ui.elements.dialogs.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.ui.screens.tv.settings.SettingsSwitchItem
import kotlinx.coroutines.launch

@Composable
fun AddRadioDialog(
    setShowDialog: (Boolean) -> Unit = { },
    onAdded: (name: String, url: String, homePageUrl: String) -> Unit
) {
    var radioName by remember { mutableStateOf("") }
    var radioUrl by remember { mutableStateOf("") }
    var radioPage by remember { mutableStateOf("") }

    val (nameFocus, urlFocus, pageFocus) = remember { FocusRequester.createRefs() }

    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.surface

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorTextColor = MaterialTheme.colorScheme.error
    )

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(color = backgroundColor) }
                .padding(horizontal = 48.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                /* Radio URL */
                OutlinedTextField(
                    value = radioName,
                    onValueChange = { radioName = it },
                    label = {
                        Text(
                            text = stringResource(R.string.Label_Radio_Name),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            urlFocus.requestFocus()
                        }
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.focusRequester(nameFocus)
                )

                OutlinedTextField(
                    value = radioUrl,
                    onValueChange = { radioUrl = it },
                    label = {
                        Text(
                            text = stringResource(R.string.Label_Radio_URL),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            pageFocus.requestFocus()
                        }
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.focusRequester(urlFocus)
                )

                OutlinedTextField(
                    value = radioPage,
                    onValueChange = { radioPage = it },
                    label = {
                        Text(
                            text = stringResource(R.string.Label_Radio_URL),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            coroutineScope.launch {
                                onAdded(
                                    radioName,
                                    radioUrl,
                                    radioPage
                                )

                                setShowDialog(false)
                            }
                        }
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.focusRequester(pageFocus)
                )

                ListItem(
                    selected = false,
                    headlineContent = { Text(stringResource(R.string.Action_Done)) },
                    onClick = {
                        coroutineScope.launch {
                            onAdded(
                                radioName,
                                radioUrl,
                                radioPage
                            )

                            setShowDialog(false)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ModifyRadioDialog(
    setShowDialog: (Boolean) -> Unit,
    radio: MediaItem?,
    onModified: (providerId: String, id: String, name: String, url: String, homepage: String) -> Unit,
    onDeleted: (providerId: String, id: String) -> Unit = {_,_->}
) {
    var radioName by remember { mutableStateOf(radio?.mediaMetadata?.station) }
    var radioUrl by remember { mutableStateOf(radio?.mediaId) }
    var radioPage by remember { mutableStateOf(radio?.mediaMetadata?.extras?.getString("homepage") ?: "") }

    val (nameFocus, urlFocus, pageFocus) = remember { FocusRequester.createRefs() }

    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.surface

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorTextColor = MaterialTheme.colorScheme.error
    )

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(color = backgroundColor) }
                .padding(horizontal = 48.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                /* Radio URL */
                OutlinedTextField(
                    value = radioName.toString(),
                    onValueChange = { radioName = it },
                    label = {
                        Text(
                            text = stringResource(R.string.Label_Radio_Name),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            urlFocus.requestFocus()
                        }
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.focusRequester(nameFocus)
                )

                OutlinedTextField(
                    value = radioUrl.toString(),
                    onValueChange = { radioUrl = it },
                    label = {
                        Text(
                            text = stringResource(R.string.Label_Radio_URL),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            pageFocus.requestFocus()
                        }
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.focusRequester(urlFocus)
                )

                OutlinedTextField(
                    value = radioPage,
                    onValueChange = { radioPage = it },
                    label = {
                        Text(
                            text = stringResource(R.string.Label_Radio_URL),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            coroutineScope.launch {
                                onModified(
                                    radio?.mediaMetadata?.extras?.getString("providerId") ?: "",
                                    radio?.mediaMetadata?.extras?.getString("id") ?: "",
                                    radioName.toString(),
                                    radioUrl.toString(),
                                    radioPage
                                )

                                setShowDialog(false)
                            }
                        }
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.focusRequester(pageFocus)
                )

                ListItem(
                    selected = false,
                    headlineContent = { Text(stringResource(R.string.Action_Done)) },
                    onClick = {
                        coroutineScope.launch {
                            onModified(
                                radio?.mediaMetadata?.extras?.getString("providerId") ?: "",
                                radio?.mediaMetadata?.extras?.getString("id") ?: "",
                                radioName.toString(),
                                radioUrl.toString(),
                                radioPage
                            )
                            setShowDialog(false)
                        }
                    }
                )
                ListItem(
                    selected = false,
                    headlineContent = { Text(stringResource(R.string.Action_Remove)) },
                    onClick = {
                        coroutineScope.launch {
                            onDeleted(
                                radio?.mediaMetadata?.extras?.getString("providerId") ?: "",
                                radio?.mediaMetadata?.extras?.getString("id") ?: ""
                            )
                            setShowDialog(false)
                        }
                    }
                )
            }
        }
    }
}