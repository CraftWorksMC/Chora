package com.craftworks.music.ui.elements

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.craftworks.music.R
import com.craftworks.music.data.useNavidromeServer
import com.craftworks.music.providers.navidrome.navidromeStatus
import com.craftworks.music.providers.navidrome.navidromeSyncInProgress

@Composable
@Preview
fun HorizontalLineWithNavidromeCheck(){
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth().padding(12.dp, 0.dp, 12.dp, 0.dp),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.onBackground
    )

    Column(modifier = Modifier
        .animateContentSize()
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
        //.background(Color(0xFFed8796)) // Catppuccin Macchiato Red.
        .background(MaterialTheme.colorScheme.errorContainer)
        .heightIn(max =
        if (useNavidromeServer.value && (navidromeStatus.value != "ok" && navidromeStatus.value != ""))
            128.dp
        else
            0.dp)
    ) {
        Text(
            text = stringResource(R.string.Navidrome_Error) + "Status: " + navidromeStatus.value,
            //color = Color(0xFF181926),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            modifier = Modifier.padding(12.dp),
        )
    }

    Column(modifier = Modifier
        .animateContentSize()
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
        .background(MaterialTheme.colorScheme.primaryContainer) // Catppuccin Macchiato Yellow.
        .heightIn(max =
        if (navidromeSyncInProgress.value)
            128.dp
        else
            0.dp)
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