package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.Screen
import com.craftworks.music.data.albumList
import com.craftworks.music.ui.elements.AlbumGrid

@ExperimentalFoundationApi
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun AlbumScreen(navHostController: NavHostController = rememberNavController()) {
    val leftPadding = if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) 0.dp else 80.dp

    /* RADIO ICON + TEXT */
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(start = leftPadding, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.placeholder),
                contentDescription = "Songs Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Text(
                text = "Albums",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize
            )
        }
        Divider(
            modifier = Modifier.padding(12.dp,56.dp,12.dp,0.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
        val sortedAlbumList = albumList.sortedBy { it.name }

        Column(modifier = Modifier.padding(12.dp,64.dp,12.dp,12.dp)) {
            AlbumGrid(sortedAlbumList, onAlbumSelected = { album ->
                navHostController.navigate(Screen.AlbumDetails.route)
                selectedAlbum = album})
        }
    }
}