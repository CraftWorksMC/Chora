package com.craftworks.music.ui.elements

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.radioList
import com.craftworks.music.ui.screens.selectedRadioIndex
import com.craftworks.music.ui.screens.showRadioModifyDialog

@OptIn(ExperimentalFoundationApi::class)
@Stable
@Composable
fun RadioCard(radio: MediaData.Radio, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onClick(); Log.d("Play", "Clicked Radio: " + radio.name) },
                onLongClick = {
                    showRadioModifyDialog.value = true
                    selectedRadioIndex.intValue =
                        radioList.indexOf(radioList.firstOrNull { it.name == radio.name && it.media == radio.media })
                },
                onLongClickLabel = "Modify Radio"
            )
            .widthIn(min = 128.dp)
            .clip(RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            // Use generic radio image as we cannot get the radio's logo reliably.
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder))
                .crossfade(true).size(256).build(),
            fallback = painterResource(R.drawable.placeholder),
            contentScale = ContentScale.FillWidth,
            contentDescription = "Album Image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
        )

        Text(
            text = radio.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(align = Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}