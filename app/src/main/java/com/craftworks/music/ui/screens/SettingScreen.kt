package com.craftworks.music.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.getSongsOnDevice
import com.craftworks.music.navidrome.getNavidromePlaylists
import com.craftworks.music.navidrome.getNavidromeRadios
import com.craftworks.music.navidrome.getNavidromeSongs
import com.craftworks.music.navidrome.navidromePassword
import com.craftworks.music.navidrome.navidromeServerIP
import com.craftworks.music.navidrome.navidromeUsername
import java.net.URL

var username = mutableStateOf("Username")
var showMoreInfo = mutableStateOf(true)
var useNavidromeServer = mutableStateOf(false)

// BACKGROUND TYPES
val backgroundTypes = listOf(
    "Plain",
    "Static Blur",
    "Animated Blur"
)
var backgroundType = mutableStateOf(backgroundTypes[2])


// TRANSCODING
val transcodingBitrateList = listOf(
    "1",
    "96",
    "128",
    "192",
    "256",
    "320",
    "No Transcoding"
)
var transcodingBitrate = mutableStateOf(transcodingBitrateList[6])

@Preview(showSystemUi = false, showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SettingScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val bottomPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 80.dp + 72.dp + 12.dp else 72.dp
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(start = leftPadding)) {

        /* HEADER */
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_settings_24),
                contentDescription = "Settings Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Text(
                text = stringResource(R.string.settings),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                modifier = Modifier.weight(1f)
            )
            Box {
                Button(
                    onClick = { navHostController.navigate(Screen.Home.route) },
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp),
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .height(32.dp)
                            .size(32.dp)
                    )
                }
            }
        }

        Divider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )

        /* Settings */
        Box(Modifier.padding(12.dp,12.dp,12.dp,bottomPadding)){
            Column {
                /* NEW SETTINGS */

                //Appearance
                Row (modifier = Modifier
                    .height(76.dp)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable{
                        navHostController.navigate(Screen.S_Appearance.route)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_palette_24),
                        contentDescription = stringResource(R.string.S_Appearance),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp).padding(start = 12.dp))
                    Text(
                        text = stringResource(R.string.S_Appearance),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(Modifier.weight(1f).fillMaxHeight())
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.chevron_down),
                        contentDescription = stringResource(R.string.S_Appearance),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp).padding(end = 12.dp).rotate(-90f))
                }

                //Media Providers
                Row (modifier = Modifier
                    .height(76.dp)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable{

                    },
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.rounded_palette_24),
                        contentDescription = stringResource(R.string.S_Media),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp).padding(start = 12.dp))
                    Text(
                        text = stringResource(R.string.S_Media),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(Modifier.weight(1f).fillMaxHeight())
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.chevron_down),
                        contentDescription = stringResource(R.string.S_Media),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp).padding(end = 12.dp).rotate(-90f))
                }

                /*

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
                            // Clear everything!
                            songsList.clear()
                            albumList.clear()
                            radioList.clear()
                            playlistList.clear()
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
                                    getNavidromePlaylists()
                                    getNavidromeRadios()
                                } catch (_: Exception){
                                    // DO NOTHING
                                }
                            }, modifier = Modifier.width(128.dp)
                        ) {
                            Text("Login")
                        }

                        Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = "Transcoding",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start
                            )
                            TranscodingDropdown()
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

                */
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscodingDropdown() {
    var expanded by remember { mutableStateOf(false) }
    // menu box
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = Modifier.width(192.dp)
    ) {
        TextField(
            modifier = Modifier
                .menuAnchor(), // menuAnchor modifier must be passed to the text field for correctness.
            readOnly = true,
            value = transcodingBitrate.value,
            onValueChange = {},
            label = { Text("Bitrate") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        // menu
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
        ) {
            // menu items
            transcodingBitrateList.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        transcodingBitrate.value = selectionOption
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
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
        sharedPreferences.edit().putString("transcodingBitRate", transcodingBitrate.value).apply()

        sharedPreferences.edit().putString("username", username.value).apply()
        sharedPreferences.edit().putString("backgroundType", backgroundType.value).apply()
        sharedPreferences.edit().putBoolean("showMoreInfo", showMoreInfo.value).apply()
    }

    fun loadSettings() {
        /* NAVIDROME SETTINGS */
        useNavidromeServer.value = sharedPreferences.getBoolean("useNavidrome", false)
        navidromeServerIP.value = sharedPreferences.getString("navidromeServerIP", "") ?: ""
        navidromeUsername.value = sharedPreferences.getString("navidromeUsername", "") ?: ""
        navidromePassword.value = sharedPreferences.getString("navidromePassword", "") ?: ""
        transcodingBitrate.value = sharedPreferences.getString("transcodingBitRate", "No Transcoding") ?: "No Transcoding"

        if (useNavidromeServer.value && (navidromeUsername.value != "" || navidromePassword.value !="" || navidromeServerIP.value != ""))
            try {
                getNavidromeSongs(URL("${navidromeServerIP.value}/rest/search3.view?query=''&songCount=10000&u=${navidromeUsername.value}&p=${navidromePassword.value}&v=1.12.0&c=Chora"))
                getNavidromePlaylists()
                getNavidromeRadios()
            } catch (_: Exception){
                // DO NOTHING
            }
        else
            getSongsOnDevice(this@saveManager.context)

        /* PREFERENCES */
        username.value = sharedPreferences.getString("username", "Username") ?: "Username"
        backgroundType.value = sharedPreferences.getString("backgroundType", "Animated Blur") ?: "Animated Blur"
        showMoreInfo.value = sharedPreferences.getBoolean("showMoreInfo", true)

        Log.d("LOAD", "Loaded Settings!")
    }
}