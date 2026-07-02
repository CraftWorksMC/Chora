package com.craftworks.music.ui.elements.dialogs.tv

import android.content.res.Configuration
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.managers.settings.MediaProviderSettingsManager
import kotlinx.coroutines.launch
import java.io.File

private enum class DialogStep { URL, CREDENTIALS }

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun CreateNavidromeProviderDialog(
    setShowDialog: (Boolean) -> Unit = { }
) {
    var url: String by remember { mutableStateOf("") }
    var username: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }
    var allowCerts: Boolean by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.surface

    var step by remember { mutableStateOf(DialogStep.URL) }
    val (nextFocus, loginFocus, addFocus) = remember { FocusRequester.createRefs() }

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
//        Text(
//            text = stringResource(R.string.Settings_Header_Media),
//            style = MaterialTheme.typography.titleLarge
//        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(color = backgroundColor) }
                .padding(horizontal = 48.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            // TODO("Remake this")
            /*
            AnimatedContent(
                targetState = step,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (it) {
                        DialogStep.URL -> {
                            /* SERVER URL */
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = {
                                    Text(
                                        text = stringResource(R.string.Label_Navidrome_URL),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "http://domain.tld:<port>",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                isError = navidromeStatus.value == "Invalid URL",
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next,
                                    autoCorrectEnabled = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        coroutineScope.launch {
                                            val server = NavidromeProvider(
                                                url,
                                                url,
                                                username,
                                                password,
                                                true,
                                                allowCerts
                                            )
                                            getNavidromeStatus(server)
                                            if (navidromeStatus.value.contains("Error 10")) {
                                                navidromeStatus.value = ""
                                                step = DialogStep.CREDENTIALS
                                            }
                                        }
                                    }
                                ),
                                colors = textFieldColors
                            )

                            /* Allow Self Signed Certs */
                            SettingsSwitchItem(
                                title = stringResource(R.string.Label_Allow_Self_Signed_Certs),
                                checked = allowCerts,
                                onCheckedChange = {
                                    allowCerts = it
                                }
                            )

                            if (navidromeStatus.value.isNotEmpty() && !navidromeStatus.value.contains(
                                    "Error 10"
                                )
                            ) {
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
                            }

                            ListItem(
                                selected = false,
                                headlineContent = { Text(stringResource(R.string.Action_Done)) },
                                modifier = Modifier.focusRequester(nextFocus),
                                onClick = {
                                    coroutineScope.launch {
                                        val server = NavidromeProvider(
                                            url,
                                            url,
                                            "",
                                            "",
                                            true,
                                            allowCerts
                                        )
                                        getNavidromeStatus(server)
                                        if (navidromeStatus.value.contains("Error 10")) {
                                            navidromeStatus.value = ""
                                            step = DialogStep.CREDENTIALS
                                        }
                                    }
                                }
                            )
                        }

                        DialogStep.CREDENTIALS -> {
                            val (usernameFocus, passwordFocus) = remember { FocusRequester.createRefs() }

                            /* USERNAME */
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = {
                                    Text(
                                        text = stringResource(R.string.Label_Navidrome_Username),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                singleLine = true,
                                isError = navidromeStatus.value.contains("40"),
                                modifier = Modifier.focusRequester(usernameFocus),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    autoCorrectEnabled = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        passwordFocus.requestFocus()
                                    }
                                ),
                                colors = textFieldColors
                            )

                            /* PASSWORD */
                            var passwordVisible by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = {
                                    Text(
                                        text = stringResource(R.string.Label_Navidrome_Password),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                                trailingIcon = {
//                                    val image = if (passwordVisible)
//                                        R.drawable.round_visibility_24
//                                    else
//                                        R.drawable.round_visibility_off_24
//
//                                    val description =
//                                        if (passwordVisible) "Hide password" else "Show password"
//
//                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
//                                        Icon(
//                                            imageVector = ImageVector.vectorResource(id = image),
//                                            description
//                                        )
//                                    }
//                                },
                                isError = navidromeStatus.value.contains("40"),
                                modifier = Modifier.focusRequester(passwordFocus),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Go,
                                    autoCorrectEnabled = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        coroutineScope.launch {
                                            val server = NavidromeProvider(
                                                url,
                                                url,
                                                username,
                                                password,
                                                true,
                                                allowCerts
                                            )
                                            getNavidromeStatus(server)
                                            if (navidromeStatus.value == "ok")
                                                addFocus.requestFocus()
                                        }
                                    }
                                ),
                                colors = textFieldColors
                            )

                            if (navidromeStatus.value.isNotEmpty() && navidromeStatus.value != "ok") {
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
                            }
                        }
                    }

                    CarouselDefaults.IndicatorRow(
                        itemCount = 2,
                        activeItemIndex = step.ordinal,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = step == DialogStep.CREDENTIALS,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListItem(
                        selected = false,
                        headlineContent = { Text(stringResource(R.string.Action_Login)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = stringResource(R.string.Action_Login),
                            )
                        },
                        modifier = Modifier.focusRequester(loginFocus),
                        onClick = {
                            coroutineScope.launch {
                                val server = NavidromeProvider(
                                    url,
                                    url,
                                    username,
                                    password,
                                    true,
                                    allowCerts
                                )
                                getNavidromeStatus(server)
                                if (navidromeStatus.value == "ok")
                                    addFocus.requestFocus()
                            }
                        }
                    )

                    ListItem(
                        selected = false,
                        enabled = navidromeStatus.value == "ok",
                        headlineContent = { Text(stringResource(R.string.Action_Add)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.Action_Login),
                            )
                        },
                        modifier = Modifier.focusRequester(addFocus),
                        onClick = {
                            coroutineScope.launch {
                                val server = NavidromeProvider(
                                    url,
                                    url,
                                    username,
                                    password,
                                    true,
                                    allowCerts
                                )
                                NavidromeManager.addServer(server)
                                AppearanceSettingsManager(context).setUsername(username)
                                navidromeStatus.value = ""
                                setShowDialog(false)
                            }
                        }
                    )


                }
            }
             */
        }
    }
}

@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun CreateLocalProviderDialog(
    setShowDialog: (Boolean) -> Unit = {}
) {
    val rootOptions = remember {
        buildList {
            Environment.getExternalStorageDirectory()?.let { add(it) }
            System.getenv("SECONDARY_STORAGE")
                ?.split(":")
                ?.map { File(it) }
                ?.filter { it.exists() }
                ?.let { addAll(it) }
        }
    }

    var currentDir by remember { mutableStateOf(rootOptions.firstOrNull() ?: File("/")) }

    val entries = remember(currentDir) {
        currentDir.listFiles()
            ?.filter { it.isDirectory && !it.isHidden && it.canRead() }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    val backgroundColor = MaterialTheme.colorScheme.surface
    val (content, action) = remember { FocusRequester.createRefs() }

    Dialog(
        onDismissRequest = { setShowDialog(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        BackHandler {
            val parentDir = currentDir.parentFile?.takeIf { it.canRead() }
            if (parentDir != null) {
                currentDir = parentDir
                println("parentDir: $parentDir")
            }
            else {
                setShowDialog(false)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(color = backgroundColor) }
                .padding(horizontal = 48.dp, vertical = 24.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val parentDir = currentDir.parentFile?.takeIf { it.canRead() }
            if (parentDir != null) {
                ListItem(
                    selected = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(content),
                    headlineContent = {
                        Text(
                            text = "..",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go up"
                        )
                    },
                    onClick = {
                        currentDir = parentDir
                        content.requestFocus()
                    }
                )
            }

            LazyColumn (
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(content)
                    .focusRestorer(content)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.absolutePath }) { dir ->
                    ListItem(
                        selected = false,
                        modifier = Modifier.fillMaxWidth(),
                        headlineContent = {
                            Text(
                                text = dir.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.s_m_local_filled),
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            currentDir = dir
                            content.requestFocus()
                        }
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListItem(
                    selected = false,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .focusRequester(action),
                    headlineContent = {
                        Text(stringResource(R.string.Action_Add))
                    },
                    supportingContent = {
                        Text(
                            text = currentDir.canonicalPath,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        TODO("Add local folder")
                        //LocalProviderManager.addFolder(currentDir.canonicalPath)
                        setShowDialog(false)
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        content.requestFocus()
    }
    LaunchedEffect(entries) {
        if (entries.isEmpty())
            action.requestFocus()
        else
            content.requestFocus()
    }
}


@Preview(
    showBackground = false, showSystemUi = true, device = "id:tv_1080p",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_TELEVISION,
    wallpaper = Wallpapers.BLUE_DOMINATED_EXAMPLE
)
@Composable
fun ModifyLrcLibProviderDialog(
    initialUrl: String = "",
    setShowDialog: (Boolean) -> Unit = { }
) {
    val context = LocalContext.current
    val settingsManager = remember { MediaProviderSettingsManager(context) }
    var url: String by remember { mutableStateOf(initialUrl) }
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
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = {
                        Text(
                            text = "LRCLIB Url",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    placeholder = {
                        Text(
                            text = "https://lrclib.net",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    isError = url.isEmpty(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            coroutineScope.launch {
                                settingsManager.setLrcLibEndpoint(url)
                                setShowDialog(false)
                            }
                        }
                    ),
                    colors = textFieldColors
                )

                ListItem(
                    selected = false,
                    headlineContent = { Text(stringResource(R.string.Action_Done)) },
                    onClick = {
                        coroutineScope.launch {
                            settingsManager.setLrcLibEndpoint(url)
                            setShowDialog(false)
                        }
                    }
                )
            }
        }
    }
}
