package me.nanova.summaryexpressive.ui.page

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(id = R.string.deleted)
//    val undoMessage = stringResource(id = R.string.undo)
    val haptics = LocalHapticFeedback.current

    val searchText by viewModel.searchText.collectAsState()
    var currentlyPlayingSummaryText by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            val summariesToShow by viewModel.summariesToShow.collectAsState()
            val allSummaries by viewModel.historySummaries.collectAsState()

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
                    items(items = summariesToShow, key = { it.id }) { summaryItem ->
                        val dismissState = rememberSwipeToDismissBoxState()

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier
                                .animateItem()
                                .clip(CardDefaults.shape),
                            enableDismissFromEndToStart = true,
                            enableDismissFromStartToEnd = false,
                            onDismiss = { direction ->
                                if (direction == SwipeToDismissBoxValue.EndToStart) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.removeHistorySummary(summaryItem.id)

                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedMessage,
//                                            actionLabel = undoMessage,
//                                            duration = SnackbarDuration.Long
                                        )
                                        // todo
//                                        when (result) {
//                                            SnackbarResult.ActionPerformed -> {
//                                                viewModel.addHistorySummary(summaryItem)
//                                            }
//
//                                            SnackbarResult.Dismissed -> {
//                                            }
//                                        }
                                    }
                                }
                            },
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        Color.Transparent
                                    }, label = "background color"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = CardDefaults.shape)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }) {
                            SummaryCard(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                summary = SummaryOutput(
                                    title = summaryItem.title,
                                    summary = summaryItem.summary,
                                    author = summaryItem.author,
                                    sourceLink = summaryItem.sourceLink,
                                    isYoutubeLink = summaryItem.isYoutubeLink,
                                    isBiliBiliLink = summaryItem.isBiliBiliLink,
                                    length = summaryItem.length,
                                ),
                                cardColors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                onShowSnackbar = { message ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                isPlaying = currentlyPlayingSummaryText == summaryItem.id,
                                onPlayRequest = {
                                    currentlyPlayingSummaryText =
                                        if (currentlyPlayingSummaryText == summaryItem.id) null
                                        else summaryItem.id
                                }
                            )
                        }
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
                    onQueryChange = { viewModel.onSearchTextChanged(it) },
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
                            IconButton(onClick = { viewModel.onSearchTextChanged("") }) {
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