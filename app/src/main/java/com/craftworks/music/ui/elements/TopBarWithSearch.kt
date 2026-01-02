package com.craftworks.music.ui.elements

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithSearch(
    headerText: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearch: (query: String) -> Unit,
    searchResults: @Composable () -> Unit,
    extraAction: @Composable () -> Unit = {},
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(textFieldState.text) {
        val query = textFieldState.text.toString()
        if (query.length <= 100) {
            onSearch(query)
        } else {
            onSearch(query.take(100))
        }
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
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (searchBarState.currentValue == SearchBarValue.Expanded)
                                    searchBarState.animateToCollapsed()
                                else
                                    searchBarState.animateToExpanded()
                            }
                        },
                    ) {
                        Crossfade(searchBarState.targetValue == SearchBarValue.Expanded) {
                            if (it)
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                )
                            else
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Search",
                                )
                        }
                    }
                },
            )
        }

    TopAppBar(
        title = { Text(text = headerText) },
        actions = {
            extraAction()
            Spacer(modifier = Modifier.width(8.dp))
            AppBarWithSearch(
                state = searchBarState,
                inputField = inputField,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    searchBarColors = SearchBarDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    appBarContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .width(64.dp)
                    .heightIn(max = if (searchBarState.currentValue == SearchBarValue.Expanded) 96.dp else 64.dp)
            )
        },
        scrollBehavior = scrollBehavior,
    )

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        collapsedShape = CircleShape,
    ) {
        searchResults()
    }
}