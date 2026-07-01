package com.craftworks.music.ui.screens.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import com.craftworks.music.R
import com.craftworks.music.data.model.Screen
import com.craftworks.music.player.SongHelper
import com.craftworks.music.ui.elements.dialogs.tv.AddRadioDialog
import com.craftworks.music.ui.elements.dialogs.tv.ModifyRadioDialog
import com.craftworks.music.ui.elements.tv.TvRadioCard
import com.craftworks.music.ui.viewmodels.RadioScreenViewModel
import kotlinx.coroutines.launch

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TvRadioScreen(
    mediaController: MediaController? = null,
    navHostController: NavHostController = rememberNavController(),
    viewModel: RadioScreenViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    var showRadioModifyDialog by remember { mutableStateOf(false) }
    var showRadioAddDialog by remember { mutableStateOf(false) }

    val radios by viewModel.radioStations.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit, radios) {
        focusRequester.requestFocus()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
            .focusRequester(focusRequester)
            .focusRestorer(focusRequester),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(radios) { radio ->
            TvRadioCard (
                radio = radio,
                onClick = {
                    coroutineScope.launch {
                        SongHelper.play(
                            listOf(radio),
                            0,
                            mediaController
                        )
                        navHostController.navigate(Screen.NowPlayingLandscape.route) {
                            launchSingleTop = true
                        }
                    }
                },
                onLongClick = {
                    viewModel.selectRadioStation(it)
                    showRadioModifyDialog = true
                }
            )
        }
        item {
            StandardCardContainer(
                modifier = Modifier.width(128.dp),
                imageCard = {
                    Card(
                        onClick = {
                            showRadioAddDialog = true
                        },
                        interactionSource = it,
                        content = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.rounded_add_24),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            )
                        }
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.Dialog_Add_Radio),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                },
            )
        }
    }

    if (showRadioAddDialog)
        AddRadioDialog(
            setShowDialog = { showRadioAddDialog = it },
            onAdded = { name, url, homePageUrl ->
                viewModel.addRadioStation(name, url, homePageUrl)
            }
        )

    if (showRadioModifyDialog) {
        val selectedRadio by viewModel.selectedRadioStation.collectAsStateWithLifecycle()
        ModifyRadioDialog(
            setShowDialog = { showRadioModifyDialog = it },
            radio = selectedRadio,
            onModified = { providerId, id, name, url, homepage ->
                viewModel.modifyRadioStation(providerId, id, name, url, homepage)
            },
            onDeleted = { providerId, id ->
                viewModel.deleteRadioStation(providerId, id)
            }
        )
    }
}
