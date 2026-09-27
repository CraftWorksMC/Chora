package com.craftworks.music.ui.elements.dialogs.appearance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.craftworks.music.R
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import kotlinx.coroutines.runBlocking


@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Preview
@Composable
fun NameDialog(setShowDialog: (Boolean) -> Unit = {} ) {
    val context = LocalContext.current
    val username by AppearanceSettingsManager(context).usernameFlow.collectAsState("Username")
    var usernameTextField by remember(username) { mutableStateOf(username) }

    AlertDialog(
        onDismissRequest = { setShowDialog(false) },
        title = { Text(stringResource(R.string.appearance_username)) },
        text = {
            OutlinedTextField(
                value = usernameTextField,
                onValueChange = {
                    usernameTextField = it
                },
                label = { stringResource(R.string.appearance_username) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                runBlocking {
                    AppearanceSettingsManager(context).setUsername(usernameTextField)
                    setShowDialog(false)
                }
            }) {
                Text(stringResource(R.string.action_done))
            }
        }
    )
}
