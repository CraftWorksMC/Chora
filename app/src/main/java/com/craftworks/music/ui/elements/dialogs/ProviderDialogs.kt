package com.craftworks.music.ui.elements.dialogs

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.navigation.NavHostController
import com.craftworks.music.R
import com.craftworks.music.data.LocalProvider
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.Screen
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.navidrome.getNavidromeStatus
import com.craftworks.music.providers.navidrome.navidromeStatus
import com.craftworks.music.saveManager
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.viewmodels.GlobalViewModels
import kotlinx.coroutines.launch

//region PREVIEWS
@Preview(showBackground = true)
@Composable
fun PreviewProviderDialog(){
    CreateMediaProviderDialog(setShowDialog = { })
}
@Preview(showBackground = true)
@Composable
fun PreviewNoMediaProvidersDialog(){
    NoMediaProvidersDialog(setShowDialog = { }, NavHostController(LocalContext.current))
}
//endregion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMediaProviderDialog(setShowDialog: (Boolean) -> Unit, context: Context = LocalContext.current) {
    var url: String by remember { mutableStateOf("") }
    var username: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }
    var allowCerts: Boolean by remember { mutableStateOf(false) }

    var dir: String by remember { mutableStateOf("/Music/") }

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Settings_Header_Media),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 24.dp)
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
                            shape = RoundedCornerShape(12.dp, 12.dp),
                            modifier = Modifier.menuAnchor(),
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
                        Column {
                            /* Directory */
                            OutlinedTextField(
                                value = dir,
                                onValueChange = { dir = it },
                                label = { Text(stringResource(R.string.Label_Local_Directory)) },
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    try {
                                        val localProvider = LocalProvider(dir, true)
                                        if (!localProviderList.contains(localProvider)) {
                                            localProviderList.add(localProvider)
                                        }
                                        selectedLocalProvider.intValue =
                                            localProviderList.indexOf(localProvider)
                                        getSongsOnDevice(context)
                                        setShowDialog(false)
                                    } catch (_: Exception) {
                                        // DO NOTHING
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                                    .height(50.dp)
                                    .fillMaxWidth()
                                    .bounceClick(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.Action_Add),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    //endregion

                    //region Navidrome
                    else if (selectedOptionText == stringResource(R.string.Source_Navidrome))
                        Column {
                            /* SERVER URL */
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = { Text(stringResource(R.string.Label_Navidrome_URL)) },
                                placeholder = { Text("http://domain.tld:<port>")},
                                singleLine = true,
                                isError = navidromeStatus.value == "Invalid URL"
                            )
                            /* USERNAME */
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text(stringResource(R.string.Label_Navidrome_Username)) },
                                singleLine = true,
                                isError = navidromeStatus.value == "Wrong username or password"
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
                                isError = navidromeStatus.value == "Wrong username or password"
                            )
                            /* Allow Self Signed Certs */
                            Row (verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)) {
//                                Icon(
//                                    imageVector = ImageVector.vectorResource(R.drawable.s_a_moreinfo),
//                                    contentDescription = "Settings Icon",
//                                    tint = MaterialTheme.colorScheme.onBackground,
//                                    modifier = Modifier
//                                        .padding(horizontal = 12.dp)
//                                        .size(24.dp)
//                                )
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
                            }

                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),horizontalArrangement = Arrangement.SpaceEvenly){
                                Button(
                                    onClick = {
                                        val server = NavidromeProvider(
                                            url, //Set numerical ID.
                                            url,
                                            username,
                                            password,
                                            true,
                                            allowCerts
                                        )
                                        coroutineScope.launch { getNavidromeStatus(server) }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onBackground
                                    ),
                                    modifier = Modifier
                                        //.padding(top = 24.dp)
                                        .padding(6.dp)
                                        .height(50.dp)
                                        //.widthIn(max = 64.dp)
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .bounceClick(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.Action_Login),
                                        modifier = Modifier.height(24.dp))
                                }
                                Button(
                                    onClick = {
                                        val server = NavidromeProvider(
                                            url, //Set numerical ID.
                                            url,
                                            username,
                                            password,
                                            true,
                                            allowCerts
                                        )
                                        NavidromeManager.addServer(server)

                                        saveManager(context).saveSettings()

                                        GlobalViewModels.refreshAll()

                                        navidromeStatus.value = ""
                                        setShowDialog(false)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onBackground
                                    ),
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .height(50.dp)
                                        //.widthIn(max = 320.dp)
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .bounceClick(),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = navidromeStatus.value == "ok"
                                ) {
                                    Text(stringResource(R.string.Action_Add),
                                        modifier = Modifier.height(24.dp))
                                }
                            }

                        }
                    //endregion
                }
            }
        }
    }
}

@Composable
fun NoMediaProvidersDialog(setShowDialog: (Boolean) -> Unit, navController: NavHostController) {
    Dialog(onDismissRequest = { setShowDialog(false) }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.Settings_Header_Media),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = stringResource(R.string.No_Providers_Splash),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = { navController.navigate(Screen.S_Providers.route) {
                            launchSingleTop = true
                        }; setShowDialog(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 24.dp, start = 40.dp, end = 40.dp)
                            .height(50.dp)
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                            .bounceClick(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.Action_Go),
                            modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}