package com.craftworks.music.ui.elements

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.R
import com.craftworks.music.data.NavidromeProvider
import com.craftworks.music.data.repository.LyricsState
import com.craftworks.music.managers.LocalProviderManager
import com.craftworks.music.managers.NavidromeManager
import com.craftworks.music.managers.SettingsManager
import com.craftworks.music.ui.elements.dialogs.EditLrcLibUrlDialog
import kotlinx.coroutines.runBlocking

@Preview
@Composable
fun LocalProviderCard(local: String = "", context: Context = LocalContext.current){
    Row(modifier = Modifier
        .padding(bottom = 12.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically) {
        // Provider Icon
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.s_m_local_filled),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = "Folder Icon",
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .height(32.dp)
                .size(32.dp)
        )
        // Provider Name
        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = stringResource(R.string.Source_Local),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
            Text(
                text = local,
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                style = MaterialTheme.typography.bodyMedium,
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

        Spacer(Modifier.width(20.dp))
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
fun NavidromeProviderCard(
    server: NavidromeProvider = NavidromeProvider(
        "0",
        "https://demo.navidrome.org",
        "CraftWorks",
        "demo",
        enabled = true,
        allowSelfSignedCert = true
    )
) {
    rememberCoroutineScope()

    Row(modifier = Modifier
        .padding(bottom = 12.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.background)
        .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider Icon
        Image(
            painter = painterResource(R.drawable.s_m_navidrome),
            contentDescription = "Navidrome Icon",
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .size(32.dp)
        )
        // Provider Name
        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = server.username,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
            Text(
                text = server.url,
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        var checked by remember { mutableStateOf(false) }
        checked = server.id == NavidromeManager.currentServerId.collectAsStateWithLifecycle().value

        // Enabled Checkbox
        Checkbox(
            checked = checked,
            onCheckedChange = {
                if (!it && NavidromeManager.getAllServers().size == 1)
                    NavidromeManager.setCurrentServer(null)
                else
                    NavidromeManager.setCurrentServer(server.id)
                Log.d("NAVIDROME", "Navidrome Current Server: ${server.id}")
            }
        )

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

        Spacer(Modifier.width(20.dp))
    }
}

@Preview
@Composable
fun LRCLIBProviderCard(
    context: Context = LocalContext.current
){
    var showEditDialog by remember { mutableStateOf(false) }
    Row(modifier = Modifier
        .padding(bottom = 12.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.background)
        .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider Icon
        Image(
            painter = painterResource(R.drawable.lrclib_logo),
            contentDescription = "LRCLIB.net logo",
            modifier = Modifier
                .padding(start = 20.dp, end = 16.dp)
                .size(32.dp)
        )
        // Provider Name
        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = "Lyrics",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
            )
            Text(
                text = "LRCLIB.net",
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Enabled Checkbox
        Checkbox(
            checked = LyricsState.useLrcLib,
            onCheckedChange = {
                LyricsState.useLrcLib = it
                runBlocking {
                    SettingsManager(context).setUseLrcLib(it)
                }
            }
        )

        if (showEditDialog)
            EditLrcLibUrlDialog(setShowDialog = { showEditDialog = it })

        // Edit Button
        Button(
            onClick = { showEditDialog = true },
            shape = CircleShape,
            modifier = Modifier
                .size(32.dp),
            contentPadding = PaddingValues(2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Remove Navidrome Server",
                modifier = Modifier
                    .height(32.dp)
                    .size(32.dp)
            )
        }

        Spacer(Modifier.width(20.dp))
    }
}