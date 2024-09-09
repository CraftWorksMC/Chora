package com.craftworks.music.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.data.localProviderList
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.providers.navidrome.navidromeStatus
import com.craftworks.music.ui.elements.HorizontalLineWithNavidromeCheck
import com.craftworks.music.ui.elements.LocalProviderCard
import com.craftworks.music.ui.elements.NavidromeProviderCard
import com.craftworks.music.ui.elements.bounceClick
import com.craftworks.music.ui.elements.dialogs.CreateMediaProviderDialog
import com.craftworks.music.ui.elements.dialogs.dialogFocusable

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
@Preview(showSystemUi = false, showBackground = true)
fun S_ProviderScreen(navHostController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    var showNavidromeServerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = leftPadding,
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding()
            )
            .dialogFocusable()
            .background(MaterialTheme.colorScheme.background)
    ) {
        /* HEADER */
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.s_m_media_providers),
                contentDescription = "Media Providers Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.Settings_Header_Media),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { navHostController.navigate(Screen.Setting.route) {
                    launchSingleTop = true
                } },
                    modifier = Modifier
                        .size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back To Settings",
                        modifier = Modifier.size(32.dp))
                }
            }
        }

        HorizontalLineWithNavidromeCheck()

        Column(
            Modifier
                .padding(12.dp)
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            // Local Providers First
            for (local in localProviderList){
                LocalProviderCard(local, context)
            }

            // Then Navidrome Providers
            for (server in NavidromeManager.getAllServers()){
                NavidromeProviderCard(server, context)
            }
        }

//        ExtendedFloatingActionButton(
//            icon = @Composable { Icon(Icons.Rounded.Add, "Add Media Provider.") },
//            onClick = {
//                showNavidromeServerDialog = true
//                navidromeStatus.value = ""
//            },
//            containerColor = MaterialTheme.colorScheme.secondaryContainer,
//            contentColor = MaterialTheme.colorScheme.secondary,
//            modifier = Modifier
//                .padding(12.dp)
//                .align(Alignment.End)
//                .bounceClick()
//                .focusable()
//        ) {
//
//        }

        FloatingActionButton(
            onClick = {
                showNavidromeServerDialog = true
                navidromeStatus.value = ""
            },
            modifier = Modifier.padding(12.dp).align(Alignment.End),
            shape = FloatingActionButtonDefaults.extendedFabShape,
            containerColor = FloatingActionButtonDefaults.containerColor,
            contentColor = contentColorFor(FloatingActionButtonDefaults.containerColor),
            elevation = FloatingActionButtonDefaults.elevation(),
        ) {
            Icon(Icons.Rounded.Add, "Add Media Provider.")
        }

//        ExtendedFloatingActionButton(
//            text = @Composable { },
//            icon = @Composable { Icon(Icons.Rounded.Add, "Add Media Provider.") },
//            onClick = {
//                showNavidromeServerDialog = true
//                navidromeStatus.value = ""
//            },
//            modifier = Modifier.padding(12.dp).align(Alignment.End),
//            expanded = false,
//            shape = FloatingActionButtonDefaults.extendedFabShape,
//            containerColor = FloatingActionButtonDefaults.containerColor,
//            contentColor = contentColorFor(FloatingActionButtonDefaults.containerColor),
//            elevation = FloatingActionButtonDefaults.elevation(),
//        )
    }

    if(showNavidromeServerDialog)
        CreateMediaProviderDialog(setShowDialog = { showNavidromeServerDialog = it })
}