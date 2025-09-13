package me.nanova.summaryexpressive.ui.page

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.vm.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel<HistoryViewModel>(),
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(id = R.string.deleted)

    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            val allSummaries = viewModel.historySummaries
            val summariesToShow = if (searchText.isBlank()) {
                allSummaries
            } else {
                val resultIds = viewModel.searchHistorySummary(searchText)
                val summariesMap = allSummaries.associateBy { summary -> summary.id }
                resultIds.mapNotNull { id -> summariesMap[id] }
            }

            LaunchedEffect(searchText) {
                if (searchText.isBlank() && summariesToShow.isNotEmpty()) {
                    lazyListState.animateScrollToItem(0)
                }
            }

            if (summariesToShow.isEmpty()) {
                val message = if (allSummaries.isEmpty()) stringResource(id = R.string.noHistory)
                else stringResource(id = R.string.nothingFound)
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.height(80.dp)) // space for search bar
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 80.dp, // For SearchBar
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = summariesToShow, key = { it.id }) {
                        SummaryCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp)
                                .animateItem(),
                            summary = SummaryOutput(
                                title = it.title,
                                summary = it.summary,
                                author = it.author,
                                sourceLink = it.sourceLink,
                                isYoutubeLink = it.isYoutubeLink,
                                length = it.length
                            ),
                            cardColors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeHistorySummary(it.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(deletedMessage)
                                }
                            },
                            onShowSnackbar = { message ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        )
                    }
                }
            }
        }

        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = { searchText = it },
                    onSearch = {
                        focusManager.clearFocus()
                    },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text(stringResource(id = R.string.search)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(id = R.string.search)
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotBlank()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            content = {},
        )
    }
}