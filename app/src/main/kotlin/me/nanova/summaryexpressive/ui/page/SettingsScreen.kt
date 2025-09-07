package me.nanova.summaryexpressive.ui.page

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpCenter
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.BuildConfig
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.SettingsUiState
import me.nanova.summaryexpressive.vm.AppViewModel

data class SettingsActions(
    val onThemeChange: (Int) -> Unit,
    val onApiKeyChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onBaseUrlChange: (String) -> Unit,
    val onUseOriginalLanguageChange: (Boolean) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onShowLengthChange: (Boolean) -> Unit,
    val onAutoExtractUrlChange: (Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNav: (Nav) -> Unit = {},
    viewModel: AppViewModel,
    highlightSection: String?,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val state by viewModel.settingsUiState.collectAsState()
    val actions = SettingsActions(
        onThemeChange = viewModel::setTheme,
        onApiKeyChange = viewModel::setApiKeyValue,
        onModelChange = viewModel::setAIProviderValue,
        onBaseUrlChange = viewModel::setBaseUrlValue,
        onUseOriginalLanguageChange = viewModel::setUseOriginalLanguageValue,
        onDynamicColorChange = viewModel::setDynamicColorValue,
        onShowLengthChange = viewModel::setShowLengthValue,
        onAutoExtractUrlChange = viewModel::setAutoExtractUrlValue
    )

    var showDialogTheme by remember { mutableStateOf(false) }
    var showAIProviderDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    if (showDialogTheme) {
        ThemeSettingsDialog(
            onDismissRequest = { showDialogTheme = false },
            currentTheme = state.theme,
            onThemeChange = actions.onThemeChange,
        )
    }

    if (showAIProviderDialog) {
        ModelSettingsDialog(
            onDismissRequest = { showAIProviderDialog = false },
            initialModel = state.aiProvider,
            initialBaseUrl = state.baseUrl,
            onConfirm = { model, baseUrl ->
                actions.onModelChange(model.name)
                actions.onBaseUrlChange(baseUrl)
            }
        )
    }

    if (showModelDialog) {
        TODO()
    }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(
            onDismissRequest = { showApiKeyDialog = false },
            currentApiKey = state.apiKey,
            onApiKeyChange = actions.onApiKeyChange
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        SettingsContent(
            innerPadding,
            state,
            actions,
            onNav = { onNav(it) },
            onShowThemeDialog = { showDialogTheme = true },
            onShowAIProviderDialog = { showAIProviderDialog = true },
            onShowModelDialog = { showModelDialog = true },
            onShowApiKeyDialog = { showApiKeyDialog = true },
            highlightSection = highlightSection
        )
    }
}

@Composable
private fun SettingsContent(
    innerPadding: PaddingValues,
    state: SettingsUiState,
    actions: SettingsActions,
    onNav: (Nav) -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowAIProviderDialog: () -> Unit,
    onShowModelDialog: () -> Unit,
    onShowApiKeyDialog: () -> Unit,
    highlightSection: String?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(highlightSection) {
        if (highlightSection == "ai") {
            lazyListState.animateScrollToItem(index = 1)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsGroup {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                            val uri = Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.chooseLanguage)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Language,
                            contentDescription = "Localized description",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.chooseLanguageDescription)) },
                )

                ListItem(
                    modifier = Modifier
                        .clickable(onClick = onShowThemeDialog)
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.theme)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.DarkMode,
                            contentDescription = "Dark mode",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    supportingContent = {
                        Text(
                            when (state.theme) {
                                1 -> stringResource(id = R.string.darkTheme)
                                2 -> stringResource(id = R.string.lightTheme)
                                else -> stringResource(id = R.string.systemTheme)
                            }
                        )
                    }
                )

                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.useDynamicColor)) },
                    supportingContent = { Text(stringResource(id = R.string.useDynamicColorDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = "Dynamic Color",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.dynamicColor,
                            onCheckedChange = {
                                actions.onDynamicColorChange(it)
                            }
                        )
                    }
                )
            }
        }

        item {
            SettingsGroup(highlighted = highlightSection == "ai") {
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = onShowAIProviderDialog)
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.setModel)) },
                    supportingContent = { Text(stringResource(id = R.string.setModelDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI Model",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                ListItem(
                    modifier = Modifier
                        .clickable(onClick = onShowApiKeyDialog)
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.setApiKey)) },
                    supportingContent = { Text(stringResource(id = R.string.setApiKeyDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.VpnKey,
                            contentDescription = "API Key",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }

        item {
            SettingsGroup {
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.useOriginalLanguage)) },
                    supportingContent = { Text(stringResource(id = R.string.useOriginalLanguageDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Translate,
                            contentDescription = "Language Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.useOriginalLanguage,
                            onCheckedChange = { newValue ->
                                actions.onUseOriginalLanguageChange(newValue)
                            }
                        )
                    }
                )

                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.useLengthOptions)) },
                    supportingContent = { Text(stringResource(id = R.string.useLengthOptionsDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Rounded.ShortText,
                            contentDescription = "Length Options",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.showLength,
                            onCheckedChange = { actions.onShowLengthChange(it) }
                        )
                    }
                )

                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.useAutoExtractUrl)) },
                    supportingContent = { Text(stringResource(R.string.useAutoExtractUrlDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Link,
                            contentDescription = "Auto Extract URL", // TODO: Use stringResource
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.autoExtractUrl, // Assumes state.autoExtractUrl exists in SettingsUiState
                            onCheckedChange = { newValue ->
                                actions.onAutoExtractUrlChange(newValue)
                            }
                        )
                    }
                )
            }
        }

        item {
            SettingsGroup {
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { onNav(Nav.Onboarding) })
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.tutorial)) },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Rounded.HelpCenter,
                            contentDescription = "Tutorial",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.tutorialDescription)) },
                )

                ListItem(
                    modifier = Modifier
                        .clickable {
                            val url =
                                "https://play.google.com/store/apps/details?id=me.nanova.SummaryExpressive"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.googlePlay)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Rate app",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.googlePlayDescription)) },
                )

                ListItem(
                    modifier = Modifier
                        .clickable {
                            val url = "https://discord.gg/WjN73wKTqd"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.discord)) },
                    leadingContent = {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.discord),
                            contentDescription = "Discord",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.discordDescription)) },
                )

                ListItem(
                    modifier = Modifier
                        .clickable {
                            val url = "https://github.com/kid1412621/SummaryExpressive"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.repository)) },
                    leadingContent = {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.github),
                            contentDescription = "Codebase",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.githubDescription)) },
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = CenterHorizontally
            ) {
                val appInfo =
                    "${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE} (${BuildConfig.FLAVOR})"
                Text(
                    text = "Version $appInfo",
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipData.newPlainText("App info", appInfo).toClipEntry()
                                )
                            }
                        })
                Text(
                    text = stringResource(id = R.string.madeBy),
                    modifier = Modifier
                        .clickable {
                            val url = "https://nanova.me"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                )
            }
        }
    }
}

private fun <T> inTween(): TweenSpec<T> = tween(durationMillis = 700)
private fun <T> outTween(): TweenSpec<T> = tween(durationMillis = 1000, delayMillis = 500)

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

    val animatedColor = remember(surfaceVariantColor) { Animatable(surfaceVariantColor) }
    val animatedBorderWidth = remember { Animatable(0f) }

    LaunchedEffect(highlighted, surfaceVariantColor, secondaryContainerColor) {
        if (highlighted) {
            launch {
                animatedColor.animateTo(secondaryContainerColor, animationSpec = inTween())
                animatedColor.animateTo(surfaceVariantColor, animationSpec = outTween())
            }
            launch {
                animatedBorderWidth.animateTo(3f, animationSpec = inTween())
                animatedBorderWidth.animateTo(0f, animationSpec = outTween())
            }
        } else {
            animatedColor.snapTo(surfaceVariantColor)
            animatedBorderWidth.snapTo(0f)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = animatedColor.value,
        ),
        border = if (animatedBorderWidth.value > 0) BorderStroke(
            animatedBorderWidth.value.dp,
            MaterialTheme.colorScheme.secondary
        ) else null
    ) {
        content()
    }
}

@Composable
private fun ThemeSettingsDialog(
    onDismissRequest: () -> Unit,
    currentTheme: Int,
    onThemeChange: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.theme)) },
        text = {
            Column {
                RadioButtonItem(
                    selected = currentTheme == 0,
                    onSelectionChange = {
                        onThemeChange(0)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.systemTheme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                RadioButtonItem(
                    selected = currentTheme == 2,
                    onSelectionChange = {
                        onThemeChange(2)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.lightTheme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                RadioButtonItem(
                    selected = currentTheme == 1,
                    onSelectionChange = {
                        onThemeChange(1)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.darkTheme),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
private fun ApiKeySettingsDialog(
    onDismissRequest: () -> Unit,
    currentApiKey: String?,
    onApiKeyChange: (String) -> Unit,
) {
    var apiTextFieldValue by remember { mutableStateOf(currentApiKey ?: "") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.setApiKey)) },
        text = {
            OutlinedTextField(
                value = apiTextFieldValue,
                onValueChange = { apiTextFieldValue = it },
                label = { Text("API Key") },
                shape = MaterialTheme.shapes.large,
                trailingIcon = {
                    if (apiTextFieldValue.isBlank()) {
                        IconButton(onClick = {
                            scope.launch {
                                clipboard.getClipEntry()?.let {
                                    apiTextFieldValue =
                                        it.clipData.getItemAt(0).text.toString()
                                }
                            }
                        }) {
                            Icon(
                                Icons.Outlined.ContentPaste,
                                contentDescription = "Paste",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = { apiTextFieldValue = "" }) {
                            Icon(
                                Icons.Outlined.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApiKeyChange(apiTextFieldValue)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun ModelSettingsDialog(
    onDismissRequest: () -> Unit,
    initialModel: AIProvider,
    initialBaseUrl: String?,
    onConfirm: (model: AIProvider, baseUrl: String) -> Unit,
) {
    var selectedModel by remember { mutableStateOf(initialModel) }
    var baseUrlTextFieldValue by remember { mutableStateOf(initialBaseUrl ?: "") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.setModel)) },
        text = {
            Column {
                AIProvider.entries.map {
                    AIProviderItem(it, selected = (selectedModel == it)) { selectedModel = it }
                }
                if (selectedModel.isBaseUrlCustomisable) {
                    Spacer(modifier = Modifier.height(9.dp))
                    OutlinedTextField(
                        value = baseUrlTextFieldValue,
                        onValueChange = { baseUrlTextFieldValue = it },
                        label = { Text("(Optional) Custom URL") },
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            if (baseUrlTextFieldValue.isBlank()) {
                                IconButton(onClick = {
                                    scope.launch {
                                        clipboard.getClipEntry()?.let {
                                            baseUrlTextFieldValue =
                                                it.clipData.getItemAt(0).text.toString()
                                        }
                                    }
                                }) {
                                    Icon(
                                        Icons.Outlined.ContentPaste,
                                        contentDescription = "Paste",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                IconButton(onClick = { baseUrlTextFieldValue = "" }) {
                                    Icon(
                                        Icons.Outlined.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedModel, baseUrlTextFieldValue)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
private fun RadioButtonItem(
    selected: Boolean,
    onSelectionChange: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelectionChange,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        content()
    }
}

@Composable
private fun AIProviderItem(
    llm: AIProvider,
    selected: Boolean,
    onSelectionChange: () -> Unit,
) {
    RadioButtonItem(selected, onSelectionChange) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ImageVector.vectorResource(id = llm.icon),
                tint = if (selected) Color.Unspecified else LocalContentColor.current,
                contentDescription = "${llm.displayName} icon",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = llm.displayName, style = MaterialTheme.typography.bodyLarge)

        }
    }
}

@Preview
@Composable
private fun ThemeSettingsDialogPreview() {
    SummaryExpressiveTheme {
        ThemeSettingsDialog(
            onDismissRequest = {},
            currentTheme = 0,
            onThemeChange = {}
        )
    }
}

@Preview
@Composable
private fun ApiKeySettingsDialogPreview() {
    SummaryExpressiveTheme {
        ApiKeySettingsDialog(
            onDismissRequest = {},
            currentApiKey = "test_api_key",
            onApiKeyChange = {}
        )
    }
}

@Preview
@Composable
private fun ModelSettingsDialogPreview() {
    SummaryExpressiveTheme {
        ModelSettingsDialog(
            onDismissRequest = {},
            initialModel = AIProvider.OPENAI,
            initialBaseUrl = "",
            onConfirm = { _, _ -> }
        )
    }
}

@Preview
@Composable
private fun RadioButtonItemPreview() {
    SummaryExpressiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RadioButtonItem(selected = true, onSelectionChange = {}) {
                Text("Option 1")
            }
            RadioButtonItem(selected = false, onSelectionChange = {}) {
                Text("Option 2")
            }
        }
    }
}

@Preview
@Composable
private fun AIProviderItemPreview() {
    SummaryExpressiveTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AIProvider.entries
                .map { AIProviderItem(it, selected = it == AIProvider.GEMINI) {} }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScrollContentPreview() {
    SummaryExpressiveTheme {
        val state = SettingsUiState(
            theme = 0,
            apiKey = "test_key",
            baseUrl = "",
            aiProvider = AIProvider.OPENAI,
            useOriginalLanguage = false,
            dynamicColor = true,
            showLength = true,
            autoExtractUrl = true // Added for preview
        )
        val actions = SettingsActions(
            onThemeChange = {},
            onApiKeyChange = {},
            onModelChange = {},
            onBaseUrlChange = {},
            onUseOriginalLanguageChange = {},
            onDynamicColorChange = {},
            onShowLengthChange = {},
            onAutoExtractUrlChange = {}
        )
        Scaffold { innerPadding ->
            SettingsContent(
                innerPadding = innerPadding,
                state = state,
                actions = actions,
                onShowThemeDialog = {},
                onShowAIProviderDialog = {},
                onShowModelDialog = {},
                onShowApiKeyDialog = {},
                highlightSection = null,
                onNav = {},
            )
        }
    }
}
