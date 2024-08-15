package com.craftworks.music.ui.elements

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.data.LocalProvider
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.localProviderList
import com.craftworks.music.data.navidromeServersList
import com.craftworks.music.data.selectedLocalProvider
import com.craftworks.music.data.selectedNavidromeServerIndex
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.providers.local.getSongsOnDevice
import com.craftworks.music.providers.navidrome.reloadNavidrome
import kotlinx.coroutines.launch

@Preview
@Composable
fun LocalProviderCard(local: LocalProvider = LocalProvider("/music", true), context: Context = LocalContext.current){

    val coroutineScope = rememberCoroutineScope()

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
                text = stringResource(R.string.Source_Local),
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
                    if (useNavidromeServer.value &&
                        selectedNavidromeServerIndex.intValue >= 0 &&
                        navidromeServersList.isNotEmpty()){

                        coroutineScope.launch { reloadNavidrome(context) }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun NavidromeProviderCard(server: NavidromeProvider = NavidromeProvider("0","https://demo.navidrome.org", "CraftWorks", "demo", true, true), context: Context = LocalContext.current){

    val coroutineScope = rememberCoroutineScope()

    Row(modifier = Modifier
        .padding(bottom = 12.dp)
        .height(64.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically) {

        // Provider Icon
        Image(
            painter = painterResource(R.drawable.s_m_navidrome),
            contentDescription = "Navidrome Icon",
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(32.dp)
        )
        // Provider Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.username,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Normal,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                modifier = Modifier
            )
            Text(
                text = server.url,
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                fontWeight = FontWeight.Normal,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
            )
        }
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

                if (it){
                    selectedNavidromeServerIndex.intValue = navidromeServersList.indexOf(server)

                    coroutineScope.launch { reloadNavidrome(context) }

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