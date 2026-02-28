package com.talosross.summaryyou.ui.page

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpCenter
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import com.talosross.summaryyou.BuildConfig
import com.talosross.summaryyou.R
import com.talosross.summaryyou.llm.AIProvider
import com.talosross.summaryyou.ui.Nav
import com.talosross.summaryyou.ui.theme.SummaryYouTheme
import com.talosross.summaryyou.vm.AppViewModel
import com.talosross.summaryyou.vm.SettingsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class DialogState {
    NONE, THEME, AI_PROVIDER, MODEL
}

data class SettingsActions(
    val onThemeChange: (Int) -> Unit,
    val onApiKeyChange: (String) -> Unit,
    val onProviderChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onBaseUrlChange: (String) -> Unit,
    val onUseOriginalLanguageChange: (Boolean) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onShowLengthChange: (Boolean) -> Unit,
    val onAutoExtractUrlChange: (Boolean) -> Unit,
    val onSessDataChange: (String, Long) -> Unit,
    val onSessDataClear: () -> Unit,
    val onDeveloperModeChange: (Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNav: (Nav) -> Unit = {},
    appViewModel: AppViewModel,
    highlightSection: String?,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val state by appViewModel.settingsUiState.collectAsState()
    val actions = SettingsActions(
        onThemeChange = appViewModel::setTheme,
        onApiKeyChange = appViewModel::setApiKeyValue,
        onProviderChange = appViewModel::setAIProviderValue,
        onModelChange = appViewModel::setModel,
        onBaseUrlChange = appViewModel::setBaseUrlValue,
        onUseOriginalLanguageChange = appViewModel::setUseOriginalLanguageValue,
        onDynamicColorChange = appViewModel::setDynamicColorValue,
        onShowLengthChange = appViewModel::setShowLengthValue,
        onAutoExtractUrlChange = appViewModel::setAutoExtractUrlValue,
        onSessDataChange = appViewModel::setSessData,
        onSessDataClear = appViewModel::clearSessData,
        onDeveloperModeChange = { enabled ->
            if (enabled) appViewModel.setDeveloperMode(true)
            else appViewModel.disableDeveloperMode()
        }
    )

    var dialogState by remember { mutableStateOf(DialogState.NONE) }
    var showBiliBiliLoginSheet by remember { mutableStateOf(false) }
    var showClearSessDataDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showClearSessDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessDataDialog = false },
            title = { Text("Clear BiliBili Login") },
            text = { Text("Are you sure you want to clear your BiliBili login information?") },
            confirmButton = {
                TextButton(onClick = {
                    actions.onSessDataClear()
                    showClearSessDataDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessDataDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBiliBiliLoginSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBiliBiliLoginSheet = false },
            sheetState = sheetState
        ) {
            fun hide() {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (sheetState.currentValue == SheetValue.Hidden) {
                        showBiliBiliLoginSheet = false
                    }
                }
            }
            BiliBiliLoginSheetContent(
                onDismiss = { hide() },
                onSessDataFound = { sessData, expires ->
                    actions.onSessDataChange(sessData, expires)
                    hide()
                }
            )
        }
    }

    AnimatedContent(
        targetState = dialogState,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
            } else {
                slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
            }
        },
        label = "dialog animation"
    ) { targetDialog ->
        when (targetDialog) {
            DialogState.NONE -> {}
            DialogState.THEME -> {
                ThemeSettingsDialog(
                    onDismissRequest = { dialogState = DialogState.NONE },
                    currentTheme = state.theme,
                    onThemeChange = actions.onThemeChange,
                )
            }

            DialogState.AI_PROVIDER -> {
                AIProviderSettingsDialog(
                    initialProvider = state.aiProvider,
                    initialBaseUrl = state.baseUrl,
                    initialApiKey = state.apiKey,
                    hasProxy = state.hasProxy,
                    onDismissRequest = { dialogState = DialogState.NONE },
                    onConfirm = { provider, baseUrl, apiKey ->
                        actions.onProviderChange(provider.name)
                        actions.onBaseUrlChange(baseUrl)
                        actions.onApiKeyChange(apiKey)
                    },
                    onNext = { provider, baseUrl, apiKey ->
                        actions.onProviderChange(provider.name)
                        actions.onBaseUrlChange(baseUrl)
                        actions.onApiKeyChange(apiKey)
                        dialogState = DialogState.MODEL
                    }
                )
            }

            DialogState.MODEL -> {
                ModelSettingsDialog(
                    onDismissRequest = { dialogState = DialogState.NONE },
                    provider = state.aiProvider,
                    initialModelId = state.model,
                    onConfirm = { modelId ->
                        actions.onModelChange(modelId)
                    },
                )
            }
        }
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
            onShowThemeDialog = { dialogState = DialogState.THEME },
            onShowAIProviderDialog = { dialogState = DialogState.AI_PROVIDER },
            onShowModelDialog = { dialogState = DialogState.MODEL },
            onShowBiliBiliLoginSheet = { showBiliBiliLoginSheet = true },
            onShowClearSessDataDialog = { showClearSessDataDialog = true },
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
    onShowBiliBiliLoginSheet: () -> Unit,
    onShowClearSessDataDialog: () -> Unit,
    highlightSection: String?,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(highlightSection) {
        if (highlightSection == "ai") {
            lazyListState.animateScrollToItem(index = 1)
        } else if (highlightSection == "3rd-party-service") {
            lazyListState.animateScrollToItem(index = 2)
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

        // AI Provider & Model — always visible in FOSS, only in developer mode for Playstore
        if (!state.hasProxy) {
            item {
                SettingsGroup(highlighted = highlightSection == "ai") {
                    ListItem(
                        modifier = Modifier
                            .clickable(onClick = onShowAIProviderDialog)
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(id = R.string.setAIProvider)) },
                        supportingContent = { Text(stringResource(id = R.string.setAIProviderDescription)) },
                        leadingContent = {
                            Icon(
                                Icons.Default.Cloud,
                                contentDescription = "AI Provider",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )

                    ListItem(
                        modifier = Modifier
                            .clickable(onClick = onShowModelDialog)
                            .fillMaxWidth(),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(stringResource(id = R.string.setModel)) },
                        supportingContent = { Text(stringResource(id = R.string.setModelDescription)) },
                        leadingContent = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "LLM Model",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
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
                    headlineContent = { Text(stringResource(R.string.useAutoExtractLink)) },
                    supportingContent = { Text(stringResource(R.string.useAutoExtractLinkDescription)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Link,
                            contentDescription = "Auto Extract URL", // TODO: Use stringResource
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.autoExtractUrl,
                            onCheckedChange = { actions.onAutoExtractUrlChange(it) }
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
                                "https://play.google.com/store/apps/details?id=com.talosross.summaryyou"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(id = R.string.googlePlay)) },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.StarRate,
                            contentDescription = "Rate app",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.googlePlayDescription)) },
                )

                ListItem(
                    modifier = Modifier
                        .clickable {
                            val url = "https://github.com/talosross/SummaryYou"
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

        // Developer Mode — only shown in Playstore flavor (hasProxy)
        if (state.hasProxy) {
            item {
                var showDisableDialog by remember { mutableStateOf(false) }

                if (showDisableDialog) {
                    AlertDialog(
                        onDismissRequest = { showDisableDialog = false },
                        title = { Text(stringResource(id = R.string.developerModeDisableTitle)) },
                        text = { Text(stringResource(id = R.string.developerModeDisableMessage)) },
                        confirmButton = {
                            TextButton(onClick = {
                                actions.onDeveloperModeChange(false)
                                showDisableDialog = false
                            }) {
                                Text(stringResource(id = R.string.developerModeDisableConfirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDisableDialog = false }) {
                                Text(stringResource(id = R.string.cancel))
                            }
                        },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsGroup {
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(stringResource(id = R.string.developerMode))
                            },
                            supportingContent = {
                                Text(stringResource(id = R.string.developerModeDescription))
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.Code,
                                    contentDescription = "Developer Mode",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = state.developerMode,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            actions.onDeveloperModeChange(true)
                                        } else {
                                            showDisableDialog = true
                                        }
                                    }
                                )
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = state.developerMode,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingsGroup(highlighted = highlightSection == "ai") {
                                val isIntegrated = state.aiProvider == AIProvider.INTEGRATED
                                val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

                                ListItem(
                                    modifier = Modifier
                                        .clickable(onClick = onShowAIProviderDialog)
                                        .fillMaxWidth(),
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = { Text(stringResource(id = R.string.setAIProvider)) },
                                    supportingContent = { Text(stringResource(id = R.string.setAIProviderDescription)) },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Cloud,
                                            contentDescription = "AI Provider",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                )

                                ListItem(
                                    modifier = Modifier
                                        .then(
                                            if (isIntegrated) Modifier
                                            else Modifier.clickable(onClick = onShowModelDialog)
                                        )
                                        .fillMaxWidth(),
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = {
                                        Text(
                                            stringResource(id = R.string.setModel),
                                            color = if (isIntegrated) disabledColor else Color.Unspecified
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            stringResource(id = R.string.setModelDescription),
                                            color = if (isIntegrated) disabledColor else Color.Unspecified
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "LLM Model",
                                            modifier = Modifier.size(24.dp),
                                            tint = if (isIntegrated) disabledColor else LocalContentColor.current
                                        )
                                    }
                                )
                            }

                            SettingsGroup(highlighted = highlightSection == "3rd-party-service") {
                                val sessDataValid =
                                    state.sessData.isNotBlank() && state.sessDataExpires > System.currentTimeMillis()
                                val itemColor =
                                    if (sessDataValid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else LocalContentColor.current

                                ListItem(
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            if (!sessDataValid) {
                                                onShowBiliBiliLoginSheet()
                                            }
                                        },
                                        onLongClick = {
                                            if (sessDataValid) {
                                                onShowClearSessDataDialog()
                                            }
                                        }
                                    ),
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = { Text("BiliBili Account", color = itemColor) },
                                    supportingContent = {
                                        if (sessDataValid) {
                                            val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                .format(Date(state.sessDataExpires))
                                            Text(
                                                "Logged in, expires on $expiryDate. Long press to clear.",
                                                color = itemColor
                                            )
                                        } else {
                                            Text("BiliBili required login to get transcripts which used for video summary")
                                        }
                                    },
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.bilibili),
                                            contentDescription = "BiliBili",
                                            modifier = Modifier.size(24.dp),
                                            tint = itemColor
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
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
                    text = "Version ${BuildConfig.VERSION_NAME}",
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
                            val url = "https://github.com/talosross"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ScrollContentPreview() {
    SummaryYouTheme {
        val state = SettingsUiState(
            theme = 0,
            apiKey = "test_key",
            baseUrl = "",
            aiProvider = AIProvider.OPENAI,
            useOriginalLanguage = false,
            dynamicColor = true,
            showLength = true,
            autoExtractUrl = true
        )
        val actions = SettingsActions(
            onThemeChange = {},
            onApiKeyChange = {},
            onProviderChange = {},
            onModelChange = {},
            onBaseUrlChange = {},
            onUseOriginalLanguageChange = {},
            onDynamicColorChange = {},
            onShowLengthChange = {},
            onAutoExtractUrlChange = {},
            onSessDataChange = { _, _ -> },
            onSessDataClear = {},
            onDeveloperModeChange = {}
        )
        Scaffold { innerPadding ->
            SettingsContent(
                innerPadding = innerPadding,
                state = state,
                actions = actions,
                onShowThemeDialog = {},
                onShowAIProviderDialog = {},
                onShowModelDialog = {},
                highlightSection = null,
                onNav = {},
                onShowBiliBiliLoginSheet = {},
                onShowClearSessDataDialog = {}
            )
        }
    }
}
