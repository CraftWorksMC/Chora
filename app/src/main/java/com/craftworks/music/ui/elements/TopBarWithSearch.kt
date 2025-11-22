package com.craftworks.music.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithSearch(
    headerIcon: ImageVector,
    headerText: String,
    onSearch: (query: String) -> Unit,
    searchResults: @Composable () -> Unit
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(textFieldState.text) {
        onSearch(textFieldState.text.toString())
    }

    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = {
                    onSearch(it)
                },
                leadingIcon = {
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = { PlainTooltip { Text("Back") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        }

    val inputIcon =
        @Composable {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text("Search") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = {
                            scope.launch { searchBarState.animateToExpanded() }
                        }) {
                            Icon(
                                Icons.Rounded.Search, "Search",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = headerIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                )
                .size(42.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = headerText,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.headlineLarge.fontSize,
            modifier = Modifier.padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            )
        )
        Spacer(Modifier.weight(1f))

        AppBarWithSearch(
            state = searchBarState,
            inputField = inputIcon,
            colors = SearchBarDefaults.appBarWithSearchColors(searchBarColors = SearchBarDefaults.colors(containerColor = Color.Transparent)),
            modifier = Modifier.width(50.dp)
        )
    }
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
    ) {
        searchResults()
    }
}