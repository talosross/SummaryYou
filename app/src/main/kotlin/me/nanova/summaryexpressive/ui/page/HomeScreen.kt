package me.nanova.summaryexpressive.ui.page

import android.content.ClipData
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.YouTube.isYouTubeLink
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.model.SummaryResult
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.util.extractTextFromDocx
import me.nanova.summaryexpressive.util.extractTextFromImage
import me.nanova.summaryexpressive.util.extractTextFromPdf
import me.nanova.summaryexpressive.util.getFileName
import me.nanova.summaryexpressive.vm.SummaryViewModel


private object MimeTypes {
    const val PDF = "application/pdf"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val PNG = "image/png"
    const val JPEG = "image/jpeg"
    const val JPG = "image/jpg"
    const val WEBP = "image/webp"

    val allSupported = arrayOf(PDF, DOCX, PNG, JPEG, JPG, WEBP)
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    initialUrl: String? = null,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    var isExtracting by remember { mutableStateOf(false) } // For Loading-Animation
    var url by remember { mutableStateOf(initialUrl ?: "") }
    val scope = rememberCoroutineScope() // Coroutine scope for async calls
    val context = LocalContext.current
    val clipboard = LocalClipboard.current // Clipboard
    val focusManager = LocalFocusManager.current // Hide cursor
    val focusRequester = remember { FocusRequester() } // Show cursor after removing
    val options = listOf(
        stringResource(id = R.string.short_length),
        stringResource(id = R.string.middle_length),
        stringResource(id = R.string.long_length)
    ) // Lengths
    val isLoading by viewModel.isLoading.collectAsState()
    val summaryResult by viewModel.currentSummaryResult.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val apiKey by viewModel.apiKey.collectAsState()
    var isDocument by remember { mutableStateOf(false) }
    var textDocument by remember { mutableStateOf<String?>(null) }
    var singleLine by remember { mutableStateOf(false) }
    val showLength by viewModel.showLength.collectAsState()
    val showLengthNumber by viewModel.lengthNumber.collectAsState()
    var selectedIndex by remember { mutableIntStateOf(showLengthNumber) } // Summary length index
    val multiLine by viewModel.multiLine.collectAsState()

    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            result.value = uri
            if (uri != null) {
                val mimeType = context.contentResolver.getType(uri)

                scope.launch {
                    isExtracting = true
                    isDocument = true
                    url = getFileName(context, uri)
                    textDocument = when (mimeType) {
                        MimeTypes.PDF -> extractTextFromPdf(context, uri)
                        MimeTypes.DOCX -> extractTextFromDocx(context, uri)
                        else -> extractTextFromImage(context, uri)
                    }
                    isExtracting = false
                }
            }
        }

    fun summarize() {
        focusManager.clearFocus()
        viewModel.summarize(url, selectedIndex, isDocument, textDocument)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopAppBar(navController, scrollBehavior)
        },
        floatingActionButton = {
            HomeFloatingActionButtons(
                onPaste = {
                    scope.launch {
                        clipboard.getClipEntry()?.let {
                            url = it.clipData.getItemAt(0).text.toString()
                        }
                    }
                },
                onSummarize = { summarize() },
                isExtracting = isExtracting
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    UrlInputSection(
                        url = url,
                        onUrlChange = { url = it },
                        onSummarize = { summarize() },
                        summaryResult = summaryResult,
                        apiKey = apiKey,
                        onClear = {
                            url = ""
                            viewModel.clearCurrentSummary()
                            focusRequester.requestFocus()
                            isDocument = false
                            singleLine = false
                        },
                        focusRequester = focusRequester,
                        onLaunchFilePicker = {
                            launcher.launch(MimeTypes.allSupported)
                        },
                        isDocument = isDocument,
                        isExtracting = isExtracting,
                        multiLine = multiLine,
                        singleLine = singleLine,
                        onSingleLineChange = { singleLine = it }
                    )

                    val currentResult = summaryResult
                    if (showLength) {
                        SummaryLengthSelector(
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = {
                                selectedIndex = it
                                viewModel.setShowLengthNumberValue(it)
                            },
                            options = options,
                        )
                    }

                    // Loading-Animation
                    if (isLoading) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(height = 8.dp))
                    }

                    if (currentResult != null && !currentResult.isError && !currentResult.summary.isNullOrEmpty()) {
                        SummaryResultSection(
                            summaryResult = currentResult,
                            url = url,
                            onRegenerate = { summarize() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTopAppBar(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    MediumFlexibleTopAppBar(
        modifier = Modifier.height(100.dp),
        colors = TopAppBarDefaults.topAppBarColors(),
        title = { },
        navigationIcon = {
            IconButton(
                onClick = { navController.navigate("settings") }
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
        actions = {
            IconButton(
                onClick = { navController.navigate("history") }
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.LibraryBooks,
                    contentDescription = "History",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UrlInputSection(
    url: String,
    onUrlChange: (String) -> Unit,
    onSummarize: () -> Unit,
    summaryResult: SummaryResult?,
    apiKey: String,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    onLaunchFilePicker: () -> Unit,
    isDocument: Boolean,
    isExtracting: Boolean,
    multiLine: Boolean,
    singleLine: Boolean,
    onSingleLineChange: (Boolean) -> Unit
) {
    val showCancelIcon = remember(url) { url.isNotBlank() }

    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("URL/Text") },
            isError = summaryResult?.isError == true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSummarize() }),
            supportingText = {
                if (summaryResult?.isError == true) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = getErrorMessage(summaryResult.errorMessage, apiKey),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingIcon = {
                if (showCancelIcon) {
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Outlined.Cancel,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            maxLines = 15,
            singleLine = if (multiLine) singleLine else true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .padding(top = 20.dp)
                .focusRequester(focusRequester)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            OutlinedButton(
                onClick = onLaunchFilePicker,
                modifier = Modifier
                    .padding(top = 27.dp)
                    .height(58.dp)
            ) {
                Box {
                    if (isExtracting) {
                        LoadingIndicator(
                            modifier = Modifier
                                .size(55.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Icon(
                        if (isDocument && !isExtracting) Icons.Filled.CheckCircle else Icons.Filled.AddCircle,
                        contentDescription = "Floating action button",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            if (multiLine) {
                val textLength = url.length
                val lineBreaks = url.count { it == '\n' }
                if (textLength >= 100 || lineBreaks >= 1) {
                    Button(
                        onClick = { onSingleLineChange(!singleLine) },
                        modifier = Modifier
                            .height(72.dp)
                            .padding(top = 15.dp)
                    ) {
                        Icon(
                            imageVector = if (singleLine) Icons.Outlined.KeyboardArrowDown
                            else Icons.Outlined.KeyboardArrowUp,
                            contentDescription = if (singleLine) "Minimize" else "Expand",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryLengthSelector(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    options: List<String>,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { onSelectedIndexChange(index) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun SummaryResultSection(
    summaryResult: SummaryResult,
    url: String,
    onRegenerate: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current

    SummaryCard(
        modifier = Modifier.padding(top = 15.dp, bottom = 15.dp),
        title = summaryResult.title,
        author = summaryResult.author,
        summary = summaryResult.summary,
        isYouTube = isYouTubeLink(url),
        onLongClick = {
            scope.launch {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboard.setClipEntry(
                    ClipData.newPlainText(
                        "User Input",
                        summaryResult.summary
                    ).toClipEntry()
                )
            }
        }
    )

    Column(
        modifier = Modifier
            .padding(top = 15.dp, bottom = 90.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Button(
                onClick = onRegenerate,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(id = R.string.regenerate))
            }
        }
    }
}


@Composable
private fun HomeFloatingActionButtons(
    onPaste: () -> Unit,
    onSummarize: () -> Unit,
    isExtracting: Boolean
) {
    val context = LocalContext.current
    val stillLoading = stringResource(id = R.string.stillLoading)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        FloatingActionButton(
            onClick = onPaste
        ) {
            Icon(
                Icons.Rounded.ContentPaste,
                contentDescription = "Paste",
                modifier = Modifier.size(24.dp)
            )
        }
        FloatingActionButton(
            onClick = {
                if (isExtracting) {
                    Toast.makeText(context, stillLoading, Toast.LENGTH_SHORT).show()
                } else {
                    onSummarize()
                }
            }
        ) {
            Icon(Icons.Filled.Check, "Check")
        }
    }
}

@Composable
private fun getErrorMessage(errorMessage: String?, apiKey: String): String {
    return when (errorMessage) {
        SummaryException.NoInternetException.message -> stringResource(id = R.string.noInternet)
        SummaryException.InvalidLinkException.message -> stringResource(id = R.string.invalidURL)
        SummaryException.NoTranscriptException.message -> stringResource(id = R.string.noTranscript)
        SummaryException.NoContentException.message -> stringResource(id = R.string.noContent)
        SummaryException.TooShortException.message -> stringResource(id = R.string.tooShort)
        SummaryException.PaywallException.message -> stringResource(id = R.string.paywallDetected)
        SummaryException.TooLongException.message -> stringResource(id = R.string.tooLong)
        SummaryException.IncorrectKeyException.message -> {
            if (apiKey.isBlank()) {
                stringResource(id = R.string.incorrectKeyOpenSource)
            } else {
                stringResource(id = R.string.incorrectKey)
            }
        }

        SummaryException.RateLimitException.message -> stringResource(id = R.string.rateLimit)
        SummaryException.NoKeyException.message -> stringResource(id = R.string.noKey)
        else -> errorMessage ?: "unknown error"
    }
}
