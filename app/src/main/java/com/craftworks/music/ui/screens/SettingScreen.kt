package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.ui.elements.bounceClick

var username = mutableStateOf("Username")
var showMoreInfo = mutableStateOf(true)

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
        .padding(start = leftPadding, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {

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
                    .bounceClick()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable{
                        navHostController.navigate(Screen.S_Appearance.route)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.s_a_palette),
                        contentDescription = stringResource(R.string.S_Appearance),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp).padding(start = 12.dp))
                    Text(
                        text = stringResource(R.string.S_Appearance),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
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
                    .bounceClick()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable{
                        navHostController.navigate(Screen.S_Providers.route)
                        println("Navigated To Providers Route")
                    },
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.s_m_media_providers),
                        contentDescription = stringResource(R.string.S_Media),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp).padding(start = 12.dp))
                    Text(
                        text = stringResource(R.string.S_Media),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                    Spacer(Modifier.weight(1f).fillMaxHeight())
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.chevron_down),
                        contentDescription = stringResource(R.string.S_Media),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(48.dp).padding(end = 12.dp).rotate(-90f))
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
                /*

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
                            if (it && (selectedNavidromeServer.value?.username != "" || selectedNavidromeServer.value?.url !="" || selectedNavidromeServer.value?.url != ""))
                                try {
                                    getNavidromeSongs(URL("${selectedNavidromeServer.value?.url}/rest/search3.view?query=''&songCount=10000&u=${selectedNavidromeServer.value?.username}&p=${selectedNavidromeServer.value?.password}&v=1.12.0&c=Chora"))
                                } catch (_: Exception){
                                    // DO NOTHING
                                }
                            else
                                getSongsOnDevice(context)
                        })
                    }

                    // NAVIDROME VARS
                    if (useNavidromeServer.value){



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