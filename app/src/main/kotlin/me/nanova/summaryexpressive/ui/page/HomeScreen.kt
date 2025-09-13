package me.nanova.summaryexpressive.ui.page

import android.content.ClipData
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.tools.getFileName
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.vm.AppViewModel
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
    onNav: (dest: Nav) -> Unit = {},
    appViewModel: AppViewModel,
    summaryViewModel: SummaryViewModel = hiltViewModel<SummaryViewModel>(),
) {
    var urlOrText by rememberSaveable { mutableStateOf("") }
    var documentFilename by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current // Hide cursor
    val focusRequester = remember { FocusRequester() } // Show cursor after removing

    val settings by appViewModel.settingsUiState.collectAsState()

    fun summarize() {
        focusManager.clearFocus()
        if (documentFilename != null) summaryViewModel.summarize(urlOrText.toUri(), settings)
        else summaryViewModel.summarize(urlOrText, settings)
    }

    val appStartAction by appViewModel.appStartAction.collectAsState()
    LaunchedEffect(appStartAction) {
        appStartAction.content?.let {
            summaryViewModel.clearCurrentSummary()
            documentFilename = null
            urlOrText = it
            if (appStartAction.autoTrigger) {
                summarize()
            }
            appViewModel.onStartActionHandled()
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current

    val options = listOf(
        stringResource(id = R.string.short_length),
        stringResource(id = R.string.middle_length),
        stringResource(id = R.string.long_length)
    ) // Lengths
    val summarizationState by summaryViewModel.summarizationState.collectAsState()

    val isLoading = summarizationState.isLoading
    val summaryResult = summarizationState.summaryResult
    val error = summarizationState.error
    val apiKey = settings.apiKey
    val showLength = settings.showLength
    val summaryLength = settings.summaryLength

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    val hasResult = summaryResult?.summary?.isNotEmpty() == true
    val isDirty = showLength && (summaryResult?.let { it.length != summaryLength } ?: false)

    val result = remember { mutableStateOf<Uri?>(null) }
    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            result.value = uri
            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                summaryViewModel.clearCurrentSummary()
                documentFilename = getFileName(context, uri)
                urlOrText = uri.toString()
            }
        }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch {
                summaryViewModel.clearCurrentSummary()
                documentFilename = getFileName(context, uri)
                urlOrText = uri.toString()
            }
        }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    scope.launch {
                        summaryViewModel.clearCurrentSummary()
                        documentFilename = getFileName(context, uri)
                        urlOrText = uri.toString()
                    }
                }
            }
        }

    fun clearInput() {
        urlOrText = ""
        summaryViewModel.clearCurrentSummary()
        focusRequester.requestFocus()
        documentFilename = null
    }

    val listState = rememberLazyListState()
    val fabVisible by remember { derivedStateOf { !listState.canScrollBackward } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { HomeTopAppBar(onNav, scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButtons(
                fabVisible = fabVisible,
                onPaste = {
                    clearInput()
                    scope.launch {
                        clipboard.getClipEntry()?.let {
                            urlOrText = it.clipData.getItemAt(0).text.toString()
                        }
                    }
                },
                onSummarize = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    summarize()
                },
                isLoading = isLoading,
                hasResult = hasResult,
                isDirty = isDirty,
                onShowSnackbar = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
                onLaunchFilePicker = {
                    filePickerLauncher.launch(MimeTypes.allSupported)
                },
                onLaunchImagePicker = {
                    imagePickerLauncher.launch("image/*")
                },
                onLaunchCamera = {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "new_image.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES
                        )
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                    uri?.let {
                        cameraImageUri = it
                        cameraLauncher.launch(it)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
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

                    InputSection(
                        urlOrText = urlOrText,
                        onUrlChange = { urlOrText = it },
                        onSummarize = { summarize() },
                        error = error,
                        apiKey = apiKey,
                        onClear = { clearInput() },
                        focusRequester = focusRequester,
                        documentFilename = documentFilename,
                        isLoading = isLoading,
                    )

                    if (showLength) {
                        LengthSelector(
                            selectedIndex = summaryLength.ordinal,
                            onSelectedIndexChange = { appViewModel.setSummaryLength(SummaryLength.entries[it]) },
                            options = options,
                            enabled = !isLoading,
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

                    summaryResult?.takeIf { it.summary.isNotEmpty() }?.let { summaryOutput ->
                        SummaryCard(
                            modifier = Modifier.padding(vertical = 15.dp),
                            summary = summaryOutput,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipData.newPlainText(
                                            "User Input",
                                            summaryOutput.summary
                                        ).toClipEntry()
                                    )
                                }
                            },
                            onShowSnackbar = {
                                scope.launch { snackbarHostState.showSnackbar(it) }
                            }
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
    onNav: (dest: Nav) -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
) {
    MediumFlexibleTopAppBar(
        modifier = Modifier.height(100.dp),
        colors = TopAppBarDefaults.topAppBarColors(),
        title = { },
        navigationIcon = {
            IconButton(
                onClick = { onNav(Nav.Settings) }
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
        actions = {
            IconButton(
                onClick = { onNav(Nav.History) }
            ) {
                Icon(
                    Icons.Outlined.History,
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
private fun InputSection(
    urlOrText: String,
    onUrlChange: (String) -> Unit,
    onSummarize: () -> Unit,
    documentFilename: String?,
    error: Throwable?,
    apiKey: String?,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    isLoading: Boolean,
) {
    val isDocument = documentFilename != null
    val isUrl = urlOrText.startsWith("http://", ignoreCase = true)
            || urlOrText.startsWith("https://", ignoreCase = true)

    val isExpandable = !isDocument && (urlOrText.length >= 100 || urlOrText.contains('\n'))
    var isExpanded by rememberSaveable(isExpandable) { mutableStateOf(isExpandable) }

    val hasText = remember(urlOrText) { urlOrText.isNotBlank() }
    val textToShow = documentFilename ?: urlOrText

    OutlinedTextField(
        value = textToShow,
        onValueChange = onUrlChange,
        label = { Text("URL/Text") },
        enabled = !isLoading,
        readOnly = isDocument,
        isError = error != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSummarize() }),
        supportingText = {
            if (error != null) {
                ErrorMessage(error.message, apiKey)
            } else if (hasText && !isDocument && !isUrl) {
                Column {
                    // Based on the rule of thumb that 100 tokens is about 75 words.
                    // ref: https://platform.openai.com/tokenizer
                    val wordCount = urlOrText.trim().split(Regex("\\s+")).size
                    val tokenCount = (wordCount * 4) / 3
                    Text(
                        text = "Approximate tokens: $tokenCount",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.tertiaryFixedDim
                    )
                }
            }
        },
        trailingIcon = {
            val clearButton = @Composable {
                AnimatedVisibility(
                    visible = hasText,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = onClear, enabled = !isLoading) {
                        Icon(
                            Icons.Outlined.Cancel,
                            contentDescription = "Clear",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            val expandButton = @Composable {
                AnimatedVisibility(
                    visible = isExpandable,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        
            AnimatedContent(
                targetState = isExpanded,
                label = "trailing-icon-swap",
            ) { targetIsExpanded ->
                if (!targetIsExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        clearButton()
                        expandButton()
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        clearButton()
                        expandButton()
                    }
                }
            }
        },
        maxLines = if (isExpanded) 7 else 1,
        singleLine = !isExpanded,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .focusRequester(focusRequester)
            .animateContentSize()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LengthSelector(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    options: List<String>,
    enabled: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { onSelectedIndexChange(index) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingActionButtons(
    fabVisible: Boolean,
    onPaste: () -> Unit,
    onSummarize: () -> Unit,
    isLoading: Boolean,
    hasResult: Boolean,
    isDirty: Boolean,
    onShowSnackbar: (String) -> Unit,
    onLaunchFilePicker: () -> Unit,
    onLaunchImagePicker: () -> Unit,
    onLaunchCamera: () -> Unit,
) {
    val stillLoading = stringResource(id = R.string.stillLoading)
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(menuExpanded) { menuExpanded = false }

    val attachmentItems = listOf(
        Triple(Icons.Rounded.Image, "Image", onLaunchImagePicker),
        Triple(Icons.Rounded.CameraAlt, "Camera", onLaunchCamera),
        Triple(Icons.Rounded.Description, "Document", onLaunchFilePicker)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButtonMenu(
            expanded = menuExpanded,
            button = {
                ToggleFloatingActionButton(
                    checked = menuExpanded,
                    onCheckedChange = { if (!isLoading) menuExpanded = !menuExpanded },
                    modifier = Modifier
                        .semantics {
                            stateDescription = if (menuExpanded) "Expanded" else "Collapsed"
                            contentDescription = "Toggle attachments menu"
                        }
                        .animateFloatingActionButton(
                            visible = fabVisible,
                            alignment = Alignment.BottomEnd
                        )
                ) {
                    val imageVector by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                        }
                    }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = "More actions",
                        modifier = Modifier.animateIcon({ checkedProgress }),
                    )
                }
            },
        ) {
            attachmentItems.forEach { (icon, text, onClick) ->
                FloatingActionButtonMenuItem(
                    icon = { Icon(icon, contentDescription = null) },
                    text = { Text(text) },
                    onClick = {
                        onClick()
                        menuExpanded = false
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { if (!isLoading) onPaste() },
            modifier = Modifier
                .padding(bottom = 16.dp)
                .animateFloatingActionButton(
                    visible = fabVisible && !menuExpanded,
                    alignment = Alignment.BottomEnd
                )
        ) {
            Icon(
                Icons.Rounded.ContentPaste,
                contentDescription = "Paste from clipboard",
            )
        }

        FloatingActionButton(
            onClick = {
                if (isLoading) onShowSnackbar(stillLoading)
                else onSummarize()
            },
            modifier = Modifier.animateFloatingActionButton(
                visible = fabVisible && !menuExpanded,
                alignment = Alignment.TopStart
            )
        ) {
            if (isLoading) {
                LoadingIndicator()
            } else if (hasResult && !isDirty) {
                Icon(Icons.Rounded.Refresh, stringResource(R.string.regenerate))
            } else {
                Icon(Icons.Rounded.Check, "Summarize")
            }
        }
    }
}

@Composable
private fun ErrorMessage(errorMessage: String?, apiKey: String?) {
    val errMsg = when (errorMessage) {
        SummaryException.NoInternetException.message -> stringResource(id = R.string.noInternet)
        SummaryException.InvalidLinkException.message -> stringResource(id = R.string.invalidURL)
        SummaryException.NoTranscriptException.message -> stringResource(id = R.string.noTranscript)
        SummaryException.NoContentException.message -> stringResource(id = R.string.noContent)
        SummaryException.TooShortException.message -> stringResource(id = R.string.tooShort)
        SummaryException.PaywallException.message -> stringResource(id = R.string.paywallDetected)
        SummaryException.TooLongException.message -> stringResource(id = R.string.tooLong)
        SummaryException.IncorrectKeyException.message -> {
            if (apiKey.isNullOrBlank()) {
                stringResource(id = R.string.incorrectKeyOpenSource)
            } else {
                stringResource(id = R.string.incorrectKey)
            }
        }

        SummaryException.RateLimitException.message -> stringResource(id = R.string.rateLimit)
        SummaryException.NoKeyException.message -> stringResource(id = R.string.noKey)
        else -> errorMessage ?: "unknown error"
    }
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = errMsg,
        color = MaterialTheme.colorScheme.error
    )
}


@Preview
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InputSectionPreview() {
    val focusRequester = remember { FocusRequester() }

    Column {
        InputSection(
            urlOrText = "A very long text to test the multiline feature. This text is intentionally made long to exceed the one hundred character limit that is used to trigger the visibility of the expand and collapse button. It also includes\na line break.",
            onUrlChange = {},
            onSummarize = {},
            error = null,
            apiKey = "test_api_key",
            onClear = {},
            focusRequester = focusRequester,
            documentFilename = null,
            isLoading = false,
        )

        HorizontalDivider()

        InputSection(
            urlOrText = "uri://for/some/file",
            onUrlChange = {},
            onSummarize = {},
            error = SummaryException.InvalidLinkException,
            apiKey = "test_api_key",
            onClear = {},
            focusRequester = focusRequester,
            documentFilename = "some image or documentations",
            isLoading = false,
        )
    }
}

@Preview
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LengthSelectorPreview() {
    val selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Short", "Medium", "Long")

    LengthSelector(
        selectedIndex = selectedIndex,
        onSelectedIndexChange = {},
        options = options,
        enabled = true
    )
}

@Preview
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingActionButtonsPreview() {
    FloatingActionButtons(
        fabVisible = true,
        onPaste = {},
        onSummarize = {},
        isLoading = false,
        hasResult = false,
        isDirty = false,
        onShowSnackbar = {},
        onLaunchFilePicker = {},
        onLaunchImagePicker = {},
        onLaunchCamera = {}
    )
}
