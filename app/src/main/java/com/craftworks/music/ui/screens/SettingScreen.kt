package com.craftworks.music.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen

@Preview(
    showSystemUi = false, showBackground = true, wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
fun SettingScreen(navHostController: NavHostController = rememberNavController()) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(
            top = WindowInsets.statusBars
                .asPaddingValues()
                .calculateTopPadding()
        )
    ) {
        /* HEADER */
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.rounded_settings_24),
                contentDescription = "Settings Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.settings),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { navHostController.navigate(Screen.Home.route) {
                launchSingleTop = true
            } },
                modifier = Modifier
                    .size(56.dp, 70.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back To Home",
                    modifier = Modifier.size(32.dp))
            }
        }

        /* Settings */
        Column (
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SettingsButton(
                Screen.S_Appearance.route,
                R.drawable.s_a_palette,
                R.string.Settings_Header_Appearance,
                navHostController)

            SettingsButton(
                Screen.S_Providers.route,
                R.drawable.s_m_media_providers,
                R.string.Settings_Header_Media,
                navHostController)

            SettingsButton(
                Screen.S_Playback.route,
                R.drawable.s_m_playback,
                R.string.Settings_Header_Playback,
                navHostController)
        }
    }
}

@Composable
private fun SettingsButton(route: String, icon: Int, text: Int, navHostController: NavHostController){
    Button(
        onClick = { navHostController.navigate(route) {
            launchSingleTop = true
        } },
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 20.dp, end = 16.dp)
                    .size(32.dp)
            )
            Text(
                text = stringResource(text),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 20.dp).weight(1f)
            )
        }
    }
}