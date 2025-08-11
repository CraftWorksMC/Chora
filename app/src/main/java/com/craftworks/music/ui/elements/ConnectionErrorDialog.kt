package com.craftworks.music.ui.elements

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftworks.music.R
import com.craftworks.music.managers.NavidromeManager

@Composable
@Preview
fun HorizontalLineWithNavidromeCheck() {
    /*
    val context = LocalContext.current

    // Observe Navidrome Server Status
    val navidromeStatus by NavidromeManager.serverStatus.collectAsStateWithLifecycle()
    val lastShownErrorStatus = remember { mutableStateOf<String?>(null) } // Keep track to avoid re-showing for same error

    LaunchedEffect(navidromeStatus) {
        if (navidromeStatus.isNotBlank() && navidromeStatus != "ok") {
            // Only show toast if the status has changed or it's a new error
            if (lastShownErrorStatus.value != navidromeStatus) {
                val errorMessage = context.getString(R.string.Navidrome_Error) + navidromeStatus
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                lastShownErrorStatus.value = navidromeStatus
            }
        } else {
            // Reset when status is "ok" or blank, so a new error can be shown later
            lastShownErrorStatus.value = null
        }
    }

    // Observe Navidrome Sync Status
    val syncingStatus by NavidromeManager.syncStatus.collectAsStateWithLifecycle()
    val syncToastShown = remember { mutableStateOf(false) } // Keep track to show sync toast only once while true

    LaunchedEffect(syncingStatus) {
        if (syncingStatus) {
            if (!syncToastShown.value) {
                val syncMessage = context.getString(R.string.Navidrome_Sync)
                Toast.makeText(context, syncMessage, Toast.LENGTH_SHORT).show()
                syncToastShown.value = true
            }
        } else {
            syncToastShown.value = false // Reset when syncing is false
        }
    }

     */
}