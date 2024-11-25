package com.craftworks.music.ui.elements

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
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
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.ui.viewmodels.GlobalViewModels

@Preview
@Composable
fun LocalProviderCard(local: String = "", context: Context = LocalContext.current){
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
                text = local,
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                fontWeight = FontWeight.Normal,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
            )
        }

        // Delete Button
        Button(
            onClick = { LocalProviderManager.removeFolder(local) },
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
//        var enabled by remember { mutableStateOf(false) }
//        enabled = true
//
//        Checkbox(
//            checked = enabled,
//            onCheckedChange = {enabled = it
//                LocalProviderManager.addServer(local.directory)
//
//                if (enabled){
//                    if (LocalProviderManager.checkActiveFolders())
//                        getSongsOnDevice(context)
//                }
//                else if (NavidromeManager.checkActiveServers())
//                        GlobalViewModels.refreshAll()
//            }
//        )
    }
}

@Preview
@Composable
fun NavidromeProviderCard(server: NavidromeProvider = NavidromeProvider("0","https://demo.navidrome.org", "CraftWorks", "demo",
    enabled = true,
    allowSelfSignedCert = true
), context: Context = LocalContext.current){

    val coroutineScope = rememberCoroutineScope()

    Row(modifier = Modifier
        .padding(bottom = 12.dp)
        .height(64.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            onClick = { NavidromeManager.removeServer(server.id) },
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
        checked = server.id == NavidromeManager.getCurrentServer()?.id

        // Enabled Checkbox
        Checkbox(
            checked = checked,
            onCheckedChange = { checked = it
                Log.d("NAVIDROME", "Navidrome Current Server: ${server.id}")
                if (it){
                    NavidromeManager.setCurrentServer(server.id)
                    GlobalViewModels.refreshAll()
                }
                else {
                    NavidromeManager.setCurrentServer(null)
                    GlobalViewModels.refreshAll()
                }
                // Update checked
                //checked = (server.id == NavidromeManager.getCurrentServer()?.id)
            }
        )
    }
}