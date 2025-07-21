package me.nanova.summaryexpressive.ui.page

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.TextSummaryViewModel
import me.nanova.summaryexpressive.llm.AIProvider


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: TextSummaryViewModel
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
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
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        ScrollContent(innerPadding, viewModel)
    }
}

@Composable
fun ScrollContent(innerPadding: PaddingValues, viewModel: TextSummaryViewModel) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var showDialogDesign by remember { mutableStateOf(false) }
    var showDialogRestart by remember { mutableStateOf(false) }
    var showDialogKey by remember { mutableStateOf(false) }
    var showDialogModel by remember { mutableStateOf(false) }
    val design by viewModel.designNumber.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val apiBaseUrl by viewModel.baseUrl.collectAsState()
    val model by viewModel.model.collectAsState()
    val useOriginalLanguage by viewModel.useOriginalLanguage.collectAsState()
    val ultraDark by viewModel.ultraDark.collectAsState()
    val multiLine by viewModel.multiLine.collectAsState()
    val showLength by viewModel.showLength.collectAsState()
    var showTutorial by remember { mutableStateOf(false) }

    if (showTutorial) {
        OnboardingScreen(
            onContinueClicked = { showTutorial = false },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showDialogDesign) {
        AlertDialog(
            onDismissRequest = { showDialogDesign = false },
            title = { Text(stringResource(id = R.string.design)) },
            text = {
                Column {
                    RadioButtonItem(
                        stringResource(id = R.string.systemDesign),
                        selected = design == 0
                    ) {
                        viewModel.setDesignNumber(0)
                        activity?.recreate()
                    }
                    RadioButtonItem(
                        stringResource(id = R.string.lightDesign),
                        selected = design == 2
                    ) {
                        viewModel.setDesignNumber(2)
                        activity?.recreate()
                    }
                    RadioButtonItem(
                        stringResource(id = R.string.darkDesign),
                        selected = design == 1
                    ) {
                        viewModel.setDesignNumber(1)
                        activity?.recreate()
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialogDesign = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
        )
    }

    if (showDialogRestart) {
        AlertDialog(
            onDismissRequest = { showDialogRestart = false },
            title = { Text(stringResource(id = R.string.restartRequired)) },
            text = { Text(stringResource(id = R.string.restartRequiredDescription)) },
            confirmButton = {
                TextButton(onClick = {
                    activity?.recreate()
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogRestart = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showDialogKey) {
        var apiTextFieldValue by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showDialogKey = false },
            title = { Text(stringResource(id = R.string.setApiKey)) },
            text = {
                OutlinedTextField(
                    value = apiTextFieldValue,
                    onValueChange = {
                        apiTextFieldValue = it // Update the value in the local variable
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setApiKeyValue(apiTextFieldValue) // Set the API key to the value from the text field
                        showDialogKey = false
                    }
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogKey = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showDialogModel) {
        var selectedModel by remember { mutableStateOf(model) }
        var apiBaseUrlTextFieldValue by remember { mutableStateOf(apiBaseUrl) }

        AlertDialog(
            onDismissRequest = { showDialogModel = false },
            title = { Text(stringResource(id = R.string.setModel)) },
            text = {
                Column {
                    AIProvider.entries.filter { it != AIProvider.GROQ }.map {
                        RadioButtonItem(it.displayName, selected = (selectedModel == it)) {
                            selectedModel = it
                        }
                    }
                    if (selectedModel.isBaseUrlCustomisable) {
                        OutlinedTextField(
                            value = apiBaseUrlTextFieldValue,
                            onValueChange = { apiBaseUrlTextFieldValue = it },
                            placeholder = { Text("Custom API Base URL for ${selectedModel}-Compatible Endpoints (Optional)") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setModelValue(selectedModel.name)
                        viewModel.setBaseUrlValue(apiBaseUrlTextFieldValue)
                        showDialogModel = false
                    }
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogModel = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
        )
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.useOriginalLanguage)) },
                    supportingContent = { Text(stringResource(id = R.string.useOriginalLanguageDescription)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_translate_24),
                            contentDescription = "Localized description",
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = useOriginalLanguage,
                            onCheckedChange = { newValue ->
                                viewModel.setUseOriginalLanguageValue(newValue)
                            }
                        )
                    }
                )
                if (Build.VERSION.SDK_INT >= 33) {
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                                val uri = Uri.fromParts("package", context.packageName, null)
                                intent.data = uri
                                context.startActivity(intent)
                            }
                            .fillMaxWidth(),
                        headlineContent = { Text(stringResource(id = R.string.chooseLanguage)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.outline_language_24),
                                contentDescription = "Localized description",
                            )
                        },
                        supportingContent = { Text(stringResource(id = R.string.chooseLanguageDescription)) },
                    )
                }
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { showDialogDesign = showDialogDesign.not() })
                        .fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.design)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_dark_mode_24),
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = {
                        Text(
                            when (design) {
                                1 -> stringResource(id = R.string.darkDesign)
                                2 -> stringResource(id = R.string.lightDesign)
                                else -> stringResource(id = R.string.systemDesign)
                            }
                        )
                    }
                )
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.useUltraDark)) },
                    supportingContent = { Text(stringResource(id = R.string.useUltraDarkDescription)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_invert_colors_24),
                            contentDescription = "Localized description",
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = ultraDark,
                            onCheckedChange = { newValue ->
                                viewModel.setUltraDarkValue(newValue)
                                activity?.recreate()
                            }
                        )
                    }
                )
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.useMultiLines)) },
                    supportingContent = { Text(stringResource(id = R.string.useMultiLinesDescription)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_format_line_spacing_24),
                            contentDescription = "Localized description",
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = multiLine,
                            onCheckedChange = { newValue ->
                                viewModel.setMultiLineValue(newValue)
                            }
                        )
                    }
                )
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.useLengthOptions)) },
                    supportingContent = { Text(stringResource(id = R.string.useLengthOptionsDescription)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_short_text_24),
                            contentDescription = "Localized description",
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = showLength,
                            onCheckedChange = { newValue ->
                                viewModel.setShowLengthValue(newValue)
                            }
                        )
                    }
                )
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { showDialogKey = showDialogKey.not() })
                        .fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.setApiKey)) },
                    supportingContent = { Text(stringResource(id = R.string.setApiKeyDescription)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_vpn_key_24),
                            contentDescription = "Localized description",
                        )
                    }
                )
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { showDialogModel = showDialogModel.not() })
                        .fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.setModel)) },
                    supportingContent = { Text(stringResource(id = R.string.setModelDescription)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_person_edit_24),
                            contentDescription = "Localized description",
                        )
                    }
                )
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = {
                            viewModel.setShowOnboardingScreenValue(true)
                            val intent = Intent(context, context.javaClass)
                            context.startActivity(intent)
                            (context as? ComponentActivity)?.finish()
                        })
                        .fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.tutorial)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.round_help_center_24),
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.tutorialDescription)) },
                )
                ListItem(
                    modifier = Modifier
                        .clickable {
                            val url = "https://github.com/kid1412621/SummaryExpressive"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(),
                    headlineContent = { Text(stringResource(id = R.string.repository)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.githubDescription)) },
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
                    headlineContent = { Text(stringResource(id = R.string.googlePlay)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.round_star_rate_24),
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.googlePlayDescription)) },
                )
                Text(
                    text = "Version ${getVersionName(context)} - $model",
                    modifier = Modifier
                        .align(alignment = CenterHorizontally)
                        .padding(top = 10.dp)
                )
                Text(
                    text = stringResource(id = R.string.madeBy),
                    modifier = Modifier
                        .align(alignment = CenterHorizontally)
                        .clickable {
                            val url = "https://github.com/kid1412621"
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                        }
                )
            }
        }
    }
}

fun getVersionName(context: Context): String? {
    val packageInfo: PackageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        throw RuntimeException(e)
    }
    return packageInfo.versionName
}

fun getVersionCode(context: Context): Int {
    val packageInfo: PackageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        throw RuntimeException(e)
    }
    @Suppress("DEPRECATION")
    return packageInfo.versionCode
}


@Composable
fun RadioButtonItem(
    text: String,
    selected: Boolean,
    onSelectionChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChange() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onSelectionChange() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
