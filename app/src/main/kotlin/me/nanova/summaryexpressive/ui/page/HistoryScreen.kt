package me.nanova.summaryexpressive.ui.page

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.vm.SummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var searchMode by remember { mutableStateOf(false) } // Active state for SearchBar
    var searchText by remember { mutableStateOf("") } // Query for SearchBar
    val focusManager = LocalFocusManager.current // Hide cursor
    val focusRequester = remember { FocusRequester() } // Show cursor after removing
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current


    if (!searchMode) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text(
                            text = stringResource(id = R.string.history),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigate("home") }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { searchMode = true }
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(id = R.string.search),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
        ) { innerPadding ->
            val reversedList = remember { mutableStateListOf<String>() }

            LaunchedEffect(viewModel.textSummaries) {
                reversedList.clear()
                reversedList.addAll(viewModel.getAllTextSummaries().reversed())
            }

            LazyColumn(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .fillMaxSize(),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewModel.textSummaries.isNotEmpty()) {
                    items(reversedList) { textSummaryId ->
                        val textSummary =
                            viewModel.textSummaries.firstOrNull { it.id == textSummaryId }
                        if (textSummary != null) {
                            SummaryCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 15.dp),
                                title = textSummary.title,
                                author = textSummary.author,
                                summary = textSummary.text,
                                isYouTube = textSummary.youtubeLink,
                                cardColors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.deleted),
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                    viewModel.removeTextSummary(textSummary.id)
                                }
                            )
                        }
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Text(
                            text = stringResource(id = R.string.noHistory),
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    } else {
        val searchResults = remember { mutableStateListOf<String>() }
        val onActiveChange: (Boolean) -> Unit = { searchMode = it }
        Box(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        onQueryChange = {
                            searchText = it
                            searchResults.clear()
                            searchResults.addAll(viewModel.searchTextSummary(searchText).reversed())
                        },
                        onSearch = { focusManager.clearFocus() },
                        expanded = searchMode,
                        onExpandedChange = onActiveChange,
                        placeholder = {
                            Text(text = stringResource(id = R.string.search))
                        },
                        leadingIcon = {
                            IconButton(onClick = { searchMode = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchText.isBlank()) {
                                        searchMode = false
                                    } else {
                                        searchText = ""
                                        focusRequester.requestFocus()
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        },
                    )
                },
                expanded = searchMode,
                onExpandedChange = onActiveChange,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                shape = SearchBarDefaults.inputFieldShape,
                colors = SearchBarDefaults.colors(),
                tonalElevation = SearchBarDefaults.TonalElevation,
                shadowElevation = SearchBarDefaults.ShadowElevation,
                windowInsets = SearchBarDefaults.windowInsets,
                content = {
                    if (searchText.isNotEmpty() && searchResults.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.nothingFound),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .padding(start = 20.dp, end = 20.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { textSummaryId ->
                                val textSummary =
                                    viewModel.textSummaries.firstOrNull { it.id == textSummaryId }
                                if (textSummary != null) {
                                    SummaryCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 15.dp),
                                        title = textSummary.title,
                                        author = textSummary.author,
                                        summary = textSummary.text,
                                        isYouTube = textSummary.youtubeLink,
                                        cardColors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        ),
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            Toast
                                                .makeText(
                                                    context,
                                                    context.getString(R.string.deleted),
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                            viewModel.removeTextSummary(textSummary.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}
