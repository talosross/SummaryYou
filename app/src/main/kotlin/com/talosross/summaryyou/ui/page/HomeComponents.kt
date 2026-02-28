package com.talosross.summaryyou.ui.page

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import com.talosross.summaryyou.BuildConfig
import com.talosross.summaryyou.R
import com.talosross.summaryyou.model.SummaryException
import com.talosross.summaryyou.ui.Nav

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeTopAppBar(
    onNav: (dest: Nav) -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
) {
    MediumFlexibleTopAppBar(
        modifier = Modifier.height(100.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        title = { },
        navigationIcon = {
            IconButton(
                onClick = { onNav(Nav.Settings()) }
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
        actions = {
            IconButton(
                onClick = { onNav(Nav.History) },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors()
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
internal fun InputSection(
    urlOrText: String,
    onUrlChange: (String) -> Unit,
    onSummarize: () -> Unit,
    documentFilename: String?,
    error: Throwable?,
    apiKey: String?,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    isLoading: Boolean,
    onPaste: () -> Unit,
    developerMode: Boolean = false,
) {
    val isDocument = documentFilename != null
    val isUrl = urlOrText.startsWith("http://", ignoreCase = true)
            || urlOrText.startsWith("https://", ignoreCase = true)

    val isExpandable = !isDocument && (urlOrText.length >= 100 || urlOrText.contains('\n'))
    var isExpanded by rememberSaveable(isExpandable) { mutableStateOf(isExpandable) }

    val hasText = remember(urlOrText) { urlOrText.isNotBlank() }
    val textToShow = documentFilename ?: urlOrText

    val showApproximateTokens = BuildConfig.FLAVOR == "foss" || developerMode

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
                ErrorMessage(error, apiKey)
            } else if (hasText && !isDocument && !isUrl && showApproximateTokens) {
                Column {
                    // Based on the rule of thumb that 100 tokens is about 75 words.
                    // ref: https://platform.openai.com/tokenizer
                    val wordCount = urlOrText.trim().split(Regex("\\s+")).size
                    val tokenCount = (wordCount * 4) / 3
                    Text(
                        text = stringResource(R.string.approximate_tokens, tokenCount),
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
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.key == Key.V &&
                    keyEvent.nativeKeyEvent.isCtrlPressed
                ) {
                    onPaste()
                    true
                } else {
                    false
                }
            }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LengthSelector(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    options: List<String>,
    enabled: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = selectedIndex == index
            val animatedWeight by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                ),
                label = "buttonWeight"
            )

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelectedIndexChange(index) },
                enabled = enabled,
                modifier = Modifier
                    .weight(animatedWeight)
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
internal fun FloatingActionButtons(
    fabVisible: Boolean,
    onPaste: () -> Unit,
    onSummarize: () -> Unit,
    isLoading: Boolean,
    hasResult: Boolean,
    isDirty: Boolean,
    onShowSnackbar: (String) -> Unit,
    onCancel: () -> Unit,
    onLaunchFilePicker: () -> Unit,
    onLaunchImagePicker: () -> Unit,
    onLaunchCamera: () -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(menuExpanded) { menuExpanded = false }

    val attachmentItems = listOf(
        Triple(Icons.Rounded.Image, stringResource(R.string.fab_image), onLaunchImagePicker),
        Triple(Icons.Rounded.CameraAlt, stringResource(R.string.fab_camera), onLaunchCamera),
        Triple(Icons.Rounded.Description, stringResource(R.string.fab_document), onLaunchFilePicker)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
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
                if (isLoading) onCancel()
                else onSummarize()
            },
            modifier = Modifier.animateFloatingActionButton(
                visible = fabVisible && !menuExpanded,
                alignment = Alignment.TopStart
            )
        ) {
            if (isLoading) {
                Icon(Icons.Filled.Close, stringResource(R.string.stop))
            } else if (hasResult && !isDirty) {
                Icon(Icons.Rounded.Refresh, stringResource(R.string.regenerate))
            } else {
                Icon(Icons.Rounded.Check, "Summarize")
            }
        }
    }
}

@Composable
internal fun ErrorMessage(error: Throwable?, apiKey: String?) {
    val errMsg = when (error) {
        is SummaryException -> {
            val resId = error.getUserMessageResId(apiKey)
            if (resId != null) stringResource(id = resId) else error.message ?: "unknown error"
        }

        else -> error?.message ?: "unknown error"
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
            onPaste = {},
            developerMode = true
        )

        HorizontalDivider()

        InputSection(
            urlOrText = "uri://for/some/file",
            onUrlChange = {},
            onSummarize = {},
            error = SummaryException.InvalidLinkException(),
            apiKey = "test_api_key",
            onClear = {},
            focusRequester = focusRequester,
            documentFilename = "some image or documentations",
            isLoading = false,
            onPaste = {},
            developerMode = false
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
        onCancel = {},
        onLaunchFilePicker = {},
        onLaunchImagePicker = {},
        onLaunchCamera = {}
    )
}

