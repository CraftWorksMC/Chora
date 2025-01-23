package com.craftworks.music.ui.elements

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val navidromeStatus by NavidromeManager.serverStatus.collectAsStateWithLifecycle()
    val showError by remember { derivedStateOf { navidromeStatus } }

    val syncingStatus by NavidromeManager.syncStatus.collectAsStateWithLifecycle()
    val showSync by remember { derivedStateOf { syncingStatus } }

    // Error Container
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = if (showError.isNotBlank() && showError != "ok") 12.dp else 0.dp
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .heightIn(
                max = if (showError.isNotBlank() && showError != "ok") 128.dp
                else 0.dp
            )
    ) {
        Text(
            text = stringResource(R.string.Navidrome_Error) + showError,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            modifier = Modifier.padding(12.dp),
        )
    }

    // Syncing Container
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (showSync) 12.dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .heightIn(
                max = if (showSync) 128.dp
                else 0.dp
            )
    ) {
        Text(
            text = stringResource(R.string.Navidrome_Sync),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            modifier = Modifier.padding(12.dp),
        )
    }
}