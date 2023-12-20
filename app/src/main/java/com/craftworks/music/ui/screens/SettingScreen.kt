package com.craftworks.music.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.getSongsOnDevice
import com.craftworks.music.mediaFolder
import com.craftworks.music.navidrome.getNavidromeSongs
import com.craftworks.music.navidrome.navidromePassword
import com.craftworks.music.navidrome.navidromeServerIP
import com.craftworks.music.navidrome.navidromeUsername
import com.craftworks.music.songsList
import java.net.URL

var username = mutableStateOf("Username")
var useBlurredBackground = mutableStateOf(true)
var showMoreInfo = mutableStateOf(true)
var useNavidromeServer = mutableStateOf(false)


@Preview(showSystemUi = false, showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SettingScreen() {
    val context = LocalContext.current.applicationContext
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    Box(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(start = leftPadding)) {
        /* SETTINGS ICON + TEXT */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.settings),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize
            )
        }
        Divider(
            modifier = Modifier.padding(12.dp,56.dp,12.dp,0.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
        /* ACTUAL SETTINGS */
        Box(Modifier.padding(0.dp,64.dp,0.dp,0.dp)){
            Column {
                /* -SEPARATOR */
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "- - - PERSONALIZATION - - -",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                /* USERNAME */
                OutlinedTextField(
                    value = username.value,
                    onValueChange = { username.value = it },
                    label = { Text("Username:") },
                    singleLine = true
                )

                /* USE BLURRED BACKGROUND */
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Use Blurred Background",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Switch(checked = useBlurredBackground.value, onCheckedChange = { useBlurredBackground.value = it })
                }
                /* SHOW MORE SONG INFO */
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Show More Song Info",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    Switch(checked = showMoreInfo.value, onCheckedChange = { showMoreInfo.value = it })
                }

                /* -SEPARATOR */
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "- - - MEDIA - - -",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 24.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                /* USE NAVIDROME SERVER */
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // TOGGLE
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Use Navidrome Server",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Switch(checked = useNavidromeServer.value, onCheckedChange = {
                            useNavidromeServer.value = it
                            songsList.clear()
                            if (it && (navidromeUsername.value != "" || navidromePassword.value !="" || navidromeServerIP.value != ""))
                                try {
                                    getNavidromeSongs(URL("${navidromeServerIP.value}/rest/search3.view?query=''&songCount=10000&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=musicApp"))
                                } catch (_: Exception){
                                    // DO NOTHING
                                }

                            else
                                getSongsOnDevice(context)
                        })
                    }

                    // NAVIDROME VARS
                    if (useNavidromeServer.value){
                        /* SERVER URL */
                        OutlinedTextField(
                            value = navidromeServerIP.value,
                            onValueChange = {
                                navidromeServerIP.value = it },
                            label = { Text("Navidrome URL:")},
                            singleLine = true
                        )
                        /* USERNAME */
                        OutlinedTextField(
                            value = navidromeUsername.value,
                            onValueChange = {
                                navidromeUsername.value = it },
                            label = { Text("Navidrome Username:")},
                            singleLine = true
                        )
                        /* PASSWORD */
                        var passwordVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = navidromePassword.value,
                            onValueChange = {
                                navidromePassword.value = it },
                            label = { Text("Navidrome Password:")},
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    R.drawable.round_visibility_24
                                else
                                    R.drawable.round_visibility_off_24

                                // Please provide localized description for accessibility services
                                val description = if (passwordVisible) "Hide password" else "Show password"

                                IconButton(onClick = {passwordVisible = !passwordVisible}){
                                    Icon(imageVector  = ImageVector.vectorResource(id = image), description)
                                }
                            }
                        )

                        Button(
                            onClick = {
                                try {
                                    saveManager(context).saveSettings()
                                    getNavidromeSongs(URL("${navidromeServerIP.value}/rest/search3.view?query=''&songCount=10000&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=musicApp"))
                                } catch (_: Exception){
                                    // DO NOTHING
                                }
                            }, modifier = Modifier.width(128.dp)
                        ) {
                            Text("Login")
                        }
                    }

                    // LOCAL FOLDER
                    else{
                        OutlinedTextField(
                            value = mediaFolder.value,
                            onValueChange = {
                                mediaFolder.value = it
                                songsList.clear()},
                            label = { Text("Music Folder:")}
                        )
                    }
                }


                Spacer(modifier = Modifier.height(72.dp))
            }
        }

    }
}


class saveManager(private val context: Context){
    private val sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    fun saveSettings(){
        /* NAVIDROME */
        sharedPreferences.edit().putBoolean("useNavidrome", useNavidromeServer.value).apply()
        sharedPreferences.edit().putString("navidromeServerIP", navidromeServerIP.value).apply()
        sharedPreferences.edit().putString("navidromeUsername", navidromeUsername.value).apply()
        sharedPreferences.edit().putString("navidromePassword", navidromePassword.value).apply()

        sharedPreferences.edit().putString("username", username.value).apply()
        sharedPreferences.edit().putBoolean("useBlurredBackground", useBlurredBackground.value).apply()
        sharedPreferences.edit().putBoolean("showMoreInfo", showMoreInfo.value).apply()
    }

    fun loadSettings() {
        /* NAVIDROME SETTINGS */
        useNavidromeServer.value = sharedPreferences.getBoolean("useNavidrome", false)
        navidromeServerIP.value = sharedPreferences.getString("navidromeServerIP", "") ?: ""
        navidromeUsername.value = sharedPreferences.getString("navidromeUsername", "") ?: ""
        navidromePassword.value = sharedPreferences.getString("navidromePassword", "") ?: ""

        if (useNavidromeServer.value && (navidromeUsername.value != "" || navidromePassword.value !="" || navidromeServerIP.value != ""))
            try {
                getNavidromeSongs(URL("${navidromeServerIP.value}/rest/search3.view?query=''&songCount=10000&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=musicApp"))
            } catch (_: Exception){
                // DO NOTHING
            }
        else
            getSongsOnDevice(this@saveManager.context)

        /* PREFERENCES */
        username.value = sharedPreferences.getString("username", "Username") ?: "Username"
        useBlurredBackground.value = sharedPreferences.getBoolean("useBlurredBackground", true)
        showMoreInfo.value = sharedPreferences.getBoolean("showMoreInfo", true)

        Log.d("LOAD", "Loaded Settings!")
    }
}