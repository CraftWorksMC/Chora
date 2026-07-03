package com.craftworks.music.ui.elements.dialogs

import android.content.Context
import android.util.Patterns
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.managers.MediaProviderManager
import com.craftworks.music.managers.settings.AppearanceSettingsManager
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import com.craftworks.music.providers.subsonic.SubsonicMediaProvider
import com.craftworks.music.providers.subsonic.SubsonicProviderData
import com.craftworks.music.ui.elements.bounceClick
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

//region PREVIEWS
@Preview(showBackground = true, device = "id:tv_1080p")
@Preview(showBackground = true)
@Composable
fun PreviewProviderDialog() {
    CreateMediaProviderDialog(setShowDialog = { })
}

@Preview(showBackground = true)
@Composable
fun PreviewLrcLibDialog() {
    EditLrcLibUrlDialog(setShowDialog = { })
}

@Preview(showBackground = true)
@Composable
fun PreviewNoMediaProvidersDialog() {
    NoMediaProvidersDialog(setShowDialog = { }, NavHostController(LocalContext.current))
}
//endregion

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditLrcLibUrlDialog(
    setShowDialog: (Boolean) -> Unit,
    context: Context = LocalContext.current
) {
    val settingsManager = remember { MediaProviderSettingsManager(context) }

    var url by remember { mutableStateOf("https://lrclib.net") }

    // Launch a coroutine to collect the flow once and update url
    LaunchedEffect(settingsManager) {
        settingsManager.lrcLibEndpointFlow.collect { value ->
            url = value
        }
    }

    var isValidUrl: Boolean by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable()
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.Dialog_LRCLIB_Url),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    isValidUrl = Patterns.WEB_URL.matcher(url).matches()
                },
                label = { Text(stringResource(R.string.Dialog_LRCLIB_Url)) },
                singleLine = true,
                isError = !isValidUrl
            )

            Button(
                onClick = {
                    if (isValidUrl)
                        runBlocking {
                            MediaProviderSettingsManager(context).setLrcLibEndpoint(url)
                        }

                    setShowDialog(false)
                },
                modifier = Modifier
                    .bounceClick(),
                enabled = isValidUrl
            ) {
                Text(stringResource(R.string.Action_Done))
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun CreateMediaProviderDialog(
    setShowDialog: (Boolean) -> Unit,
    context: Context = LocalContext.current
) {
    var isError: Boolean by remember { mutableStateOf(false) }
    var url: String by remember { mutableStateOf("") }
    var username: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }
    var credentials: String by remember { mutableStateOf("") }
    var allowCerts: Boolean by remember { mutableStateOf(false) }

    var dir: String by remember { mutableStateOf("/Music/") }

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .widthIn(max = 320.dp)
                .dialogFocusable()
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.Settings_Header_Media),
                style = MaterialTheme.typography.titleLarge
            )

            var expanded by remember { mutableStateOf(false) }

            val options = listOf(
                stringResource(R.string.Source_Local),
                stringResource(R.string.Source_Navidrome)
            )
            var selectedOptionText by remember { mutableStateOf(options[1]) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    value = selectedOptionText,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.Dialog_Media_Source)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedOptionText = selectionOption
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            //region Local Folder
            if (selectedOptionText == stringResource(R.string.Source_Local))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    /* Directory */
                    OutlinedTextField(
                        value = dir,
                        onValueChange = { dir = it },
                        label = { Text(stringResource(R.string.Label_Local_Directory)) },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    TODO("Add folder I presume?")
                                    //LocalProviderManager.addFolder(dir)
                                    setShowDialog(false)
                                } catch (_: Exception) {
                                    // DO NOTHING
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .bounceClick(),
                    ) {
                        Text(
                            stringResource(R.string.Action_Add)
                        )
                    }
                }
            //endregion

            //region Navidrome
            else if (selectedOptionText == stringResource(R.string.Source_Navidrome))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    /* SERVER URL */
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.Label_Navidrome_URL)) },
                        placeholder = { Text("http://domain.tld:<port>") },
                        singleLine = true,
                        isError = isError
                    )
                    /* USERNAME */
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.Label_Navidrome_Username)) },
                        singleLine = true,
                        isError = isError // TODO("Check credentials error")
                    )
                    /* PASSWORD */
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.Label_Navidrome_Password)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                R.drawable.round_visibility_24
                            else
                                R.drawable.round_visibility_off_24

                            // Please provide localized description for accessibility services
                            val description =
                                if (passwordVisible) "Hide password" else "Show password"

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = image),
                                    description
                                )
                            }
                        },
                        isError = isError // TODO("Check URL error")
                    )

                    /* Allow Self Signed Certs */
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                    ) {
                        Text(
                            text = stringResource(R.string.Label_Allow_Self_Signed_Certs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Switch(checked = allowCerts, onCheckedChange = { allowCerts = it })
                    }

                    // TODO("Check error")
                    /*
                    if (navidromeStatus.value != "") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Status: ${navidromeStatus.value}",
                                fontWeight = FontWeight.Medium,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }*/

                    Crossfade(
                        true//navidromeStatus.value == "ok" TODO("Check error")
                    ) {
                        if (it) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val provider = SubsonicMediaProvider(
                                            SubsonicProviderData(
                                                url = url,
                                                username = username,
                                                allowSelfSignedCert = allowCerts,
                                            )
                                        )

                                        provider.init(context)
                                        try {
                                            val auth = provider.authenticate(username, password)
                                            provider.providerData.credentials = auth.credential

                                            MediaProviderManager.addProvider(provider)
                                            AppearanceSettingsManager(context).setUsername(username)
                                            setShowDialog(false)
                                        }
                                        catch (ex: Exception) {
                                            println(ex.message)
                                            println(ex.stackTrace)
                                            isError = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .height(50.dp)
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .bounceClick(),
                                enabled = true//navidromeStatus.value == "ok" TODO("Enable only when it's ok I presume?")
                            ) {
                                Text(
                                    stringResource(R.string.Action_Add)
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    TODO("Figure out this (I'm too lazy rn)")
                                    /*val server = NavidromeProvider(
                                        url,
                                        url,
                                        username,
                                        password,
                                        true,
                                        allowCerts
                                    )
                                    coroutineScope.launch {
                                        getNavidromeStatus(server)
                                    }*/
                                },
                                modifier = Modifier
                                    .height(50.dp)
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .bounceClick()
                            ) {
                                Text(
                                    stringResource(R.string.Action_Login)
                                )
                            }
                        }
                    }
                }
            //endregion
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoMediaProvidersDialog(setShowDialog: (Boolean) -> Unit, navController: NavHostController) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Column(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .dialogFocusable(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.Settings_Header_Media),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(R.string.No_Providers_Splash),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {
                    navController.navigate(Screen.S_Providers.route) {
                        launchSingleTop = true
                    }; setShowDialog(false)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .bounceClick()
            ) {
                Text(
                    stringResource(R.string.Action_Go)
                )
            }
        }
    }
}