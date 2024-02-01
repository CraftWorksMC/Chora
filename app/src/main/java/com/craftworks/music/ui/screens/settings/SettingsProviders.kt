package com.craftworks.music.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.data.albumList
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.playlistList
import com.craftworks.music.data.radioList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.data.songsList
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.navidrome.getNavidromePlaylists
import com.craftworks.music.providers.navidrome.getNavidromeRadios
import com.craftworks.music.providers.navidrome.getNavidromeSongs
import com.craftworks.music.providers.navidrome.selectedNavidromeServerIndex
import com.craftworks.music.providers.navidrome.useNavidromeServer
import com.craftworks.music.ui.elements.CreateMediaProviderDialog
import com.craftworks.music.ui.elements.bounceClick
import java.net.URL

@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_ProviderScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext
    val leftPadding =
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp
    val bottomPadding =
        if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 80.dp + 72.dp + 12.dp else 72.dp

    var showNavidromeServerDialog by remember { mutableStateOf(false) }

    Box(){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = leftPadding, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {

            /* HEADER */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.s_a_palette),
                    contentDescription = "Media Providers Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(R.string.S_Media),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    Button(
                        onClick = { navHostController.navigate(Screen.Setting.route) },
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Return To Settings",
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

            Column(Modifier.padding(12.dp,12.dp,12.dp,bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally){
                // Local Providers First
                for (local in localProviderList){
                    Row(modifier = Modifier
                        .padding(bottom = 12.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically) {
                        // Provider Icon
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.s_m_local_filled),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = "Folder Icon",
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .height(32.dp)
                                .size(32.dp)
                        )
                        // Provider Name
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.S_M_Local),
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                modifier = Modifier
                            )
                            Text(
                                text = local.directory,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            )
                        }

                        // Delete Button
                        Button(
                            onClick = {
                                localProviderList.remove(local) },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(32.dp),
                            contentPadding = PaddingValues(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = "Delete Local Provider",
                                modifier = Modifier
                                    .height(32.dp)
                                    .size(32.dp)
                            )
                        }
                        // Enabled Checkbox
                        var enabled by remember { mutableStateOf(false) }
                        enabled = localProviderList[localProviderList.indexOf(local)].enabled
                        println(enabled)

                        Checkbox(
                            checked = enabled,
                            onCheckedChange = {enabled = it
                                val index = localProviderList.indexOf(local)

                                localProviderList[localProviderList.indexOf(local)] = localProviderList[localProviderList.indexOf(local)].copy(enabled = it)

                                selectedLocalProvider.intValue = index

                                if (enabled){
                                    if (selectedLocalProvider.intValue >= 0 && selectedLocalProvider.intValue < localProviderList.size && localProviderList.size > 0)
                                        getSongsOnDevice(context)
                                }
                                else {
                                    songsList.clear()
                                    albumList.clear()
                                    radioList.clear()
                                    playlistList.clear()

                                    if (useNavidromeServer.value &&
                                        selectedNavidromeServerIndex.intValue >= 0 &&
                                        navidromeServersList.isNotEmpty()){

                                        getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
                                        getNavidromePlaylists()
                                        getNavidromeRadios()
                                    }
                                }
                            }
                        )
                    }
                }

                // Then Navidrome Providers
                for (server in navidromeServersList){
                    Row(modifier = Modifier
                        .padding(bottom = 12.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically) {

                        // Provider Icon
                        Image(
                            painter = painterResource(R.drawable.s_m_navidrome),
                            //tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Navidrome Icon",
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(32.dp)
                        )

                        // Provider Name
                        Text(
                            text = URL(server.url).host,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Normal,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            modifier = Modifier.weight(1f)
                        )
                        // Delete Button
                        Button(
                            onClick = { navidromeServersList.remove(server) },
                            shape = CircleShape,
                            modifier = Modifier
                                .size(32.dp),
                            contentPadding = PaddingValues(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                tint = MaterialTheme.colorScheme.onBackground,
                                contentDescription = "Remove Navidrome Server",
                                modifier = Modifier
                                    .height(32.dp)
                                    .size(32.dp)
                            )
                        }

                        var checked by remember { mutableStateOf(false) }
                        if (useNavidromeServer.value)
                            checked = selectedNavidromeServerIndex.intValue == navidromeServersList.indexOf(server)

                        // Enabled Checkbox
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { checked = it
                                useNavidromeServer.value = it
                                
                                songsList.clear()
                                albumList.clear()
                                radioList.clear()
                                playlistList.clear()

                                if (it){
                                    selectedNavidromeServerIndex.intValue = navidromeServersList.indexOf(server)
                                    // Reload Navidrome
                                    getNavidromeSongs(URL("${navidromeServersList[selectedNavidromeServerIndex.intValue].url}/rest/search3.view?query=''&songCount=10000&u=${navidromeServersList[selectedNavidromeServerIndex.intValue].username}&p=${navidromeServersList[selectedNavidromeServerIndex.intValue].password}&v=1.12.0&c=Chora"))
                                    getNavidromePlaylists()
                                    getNavidromeRadios()

                                    // Make very sure that the selectedLocalProvider actually exists
                                    if (selectedLocalProvider.intValue >= 0 && selectedLocalProvider.intValue < localProviderList.size && localProviderList.size > 0)
                                        if (localProviderList[selectedLocalProvider.intValue].enabled)
                                            getSongsOnDevice(context)
                                }
                                else{
                                    if (selectedLocalProvider.intValue >= 0 && selectedLocalProvider.intValue < localProviderList.size && localProviderList.size > 0)
                                        if (localProviderList[selectedLocalProvider.intValue].enabled)
                                            getSongsOnDevice(context)
                                }
                            }
                        )
                    }
                }
            }


        }

        Box(modifier = Modifier
            .padding(bottom = bottomPadding + 12.dp)
            .padding(12.dp)
            .align(Alignment.BottomEnd)
        ){
            FloatingActionButton(
                onClick = {
                          showNavidromeServerDialog = true
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.bounceClick()
            ) {
                Icon(Icons.Rounded.Add, "Add Media Provider.")
            }


        }
    }
    if(showNavidromeServerDialog)
        CreateMediaProviderDialog(setShowDialog = { showNavidromeServerDialog = it })
}