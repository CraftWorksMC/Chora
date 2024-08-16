package com.craftworks.music.ui.elements

import android.content.res.Configuration
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.craftworks.music.R
import com.craftworks.music.data.MediaData
import com.craftworks.music.data.radioList
import com.craftworks.music.ui.screens.selectedRadioIndex
import com.craftworks.music.ui.screens.showRadioModifyDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadioCard(radio: MediaData.Radio, onClick: () -> Unit){
    Card(
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
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 128.dp)
                //.height(172.dp)
            , horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                // Use generic radio image as we cannot get the radio's logo reliably.
                model = Uri.parse("android.resource://com.craftworks.music/" + R.drawable.radioplaceholder),
                placeholder = painterResource(R.drawable.placeholder),
                fallback = painterResource(R.drawable.placeholder),
                contentScale = ContentScale.FillWidth,
                contentDescription = "Album Image",
                modifier = Modifier
                    .fillMaxSize()
                    //.height(128.dp)
                    //.width(128.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            )


            Text(
                text = radio.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize().wrapContentHeight(align = Alignment.CenterVertically),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
@Preview(
    name = "Dark Mode",
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
fun RadioCardPreview(){
    RadioCard(MediaData.Radio("", "", "")){}
}