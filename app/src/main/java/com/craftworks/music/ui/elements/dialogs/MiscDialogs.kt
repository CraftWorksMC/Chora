package com.craftworks.music.ui.elements.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.managers.settings.MiscSettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking


@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Preview
@Composable
fun DownloadTemplateDialog(onDismissRequest: () -> Unit = {}, onConfirm: (template: String) -> Unit = {}, template: Flow<String>? = null, title: Int = R.string.misc_download_template, desc: Int = R.string.misc_download_template_description) {
    val context = LocalContext.current
    val downloadTemplate by (template ?: MiscSettingsManager(context).downloadTemplateFlow).collectAsState("{album}/{title} - {artist}.{ext}")
    var templateTextField by remember(downloadTemplate) { mutableStateOf(downloadTemplate) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(desc))

                OutlinedTextField(
                    value = templateTextField,
                    onValueChange = {
                        templateTextField = it
                    },
                    label = { stringResource(title) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                runBlocking {
                    onConfirm(templateTextField)
                    onDismissRequest()
                }
            }) {
                Text(stringResource(R.string.action_done))
            }
        }
    )
}