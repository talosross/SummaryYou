package com.talosross.summaryyou

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(modifier: Modifier = Modifier, navController: NavHostController, viewModel: TextSummaryViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
                            imageVector = Icons.Filled.ArrowBack,
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
    val context2 = LocalContext.current as Activity
    var showDialogDesign by remember { mutableStateOf(false) }
    var showDialogRestart by remember { mutableStateOf(false) }
    var showDialogKey by remember { mutableStateOf(false) }
    var showDialogModel by remember { mutableStateOf(false) }
    var design = viewModel.getDesignNumber()
    val currentLocale = Resources.getSystem().configuration.locales[0]
    val currentLanguage = currentLocale.language
    val key: String = APIKeyLibrary.getAPIKey()
    var showTutorial by remember { mutableStateOf(false) }

    // Existing code...

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
                    RadioButtonItem(stringResource(id = R.string.systemDesign), selected = !(viewModel.getDesignNumber() == 1 || viewModel.getDesignNumber() == 2)) {
                        viewModel.setDesignNumber(0)
                        context2.recreate()
                    }
                    RadioButtonItem(stringResource(id = R.string.lightDesign), selected = (viewModel.getDesignNumber() == 2)) {
                        viewModel.setDesignNumber(2)
                        context2.recreate()
                    }
                    RadioButtonItem(stringResource(id = R.string.darkDesign), selected = (viewModel.getDesignNumber() == 1)) {
                        viewModel.setDesignNumber(1)
                        context2.recreate()
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

    if(showDialogRestart) {
        AlertDialog(
            onDismissRequest = { showDialogRestart = false },
            title = { Text(stringResource(id = R.string.restartRequired))},
            text = { Text(stringResource(id = R.string.restartRequiredDescription))},
            confirmButton =  {
                TextButton(onClick = {
                    context2.recreate()
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

    if(showDialogKey) {
        var apiTextFieldValue by remember { mutableStateOf(viewModel.getApiKeyValue()?.toString() ?: "") }
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

    if(showDialogModel) {
        var selectedModel by remember { mutableStateOf(viewModel.getModelValue()) }
        AlertDialog(
            onDismissRequest = { showDialogModel = false },
            title = { Text(stringResource(id = R.string.setModel)) },
            text = {
                Column {
                    RadioButtonItem("Groq", selected = (selectedModel == "Groq")) {
                        selectedModel = "Groq"
                    }
                    RadioButtonItem("OpenAI", selected = (selectedModel == "OpenAI")) {
                        selectedModel = "OpenAI"
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setModelValue(selectedModel)
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
                        modifier = Modifier.fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
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
                                checked = viewModel.getUseOriginalLanguageValue(), // Hier wird der Zustand aus der ViewModel-Variable abgerufen
                                onCheckedChange = { newValue ->
                                    viewModel.setUseOriginalLanguageValue(newValue) // Hier wird der Wert der ViewModel-Variable gesetzt
                                }
                            )
                        }
                )
                if (Build.VERSION.SDK_INT >= 33) {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                            val uri = Uri.fromParts("package", context?.packageName, null)
                            intent.data = uri

                            context.startActivity(intent)

                        }
                        .fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
                    headlineContent = { Text(stringResource(id = R.string.chooseLanguage)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_language_24),
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.chooseLanguageDescription))},
                ) }
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
                    supportingContent = { Text(when (viewModel.getDesignNumber()) {
                        1 -> stringResource(id = R.string.darkDesign)
                        2 -> stringResource(id = R.string.lightDesign)
                        else -> stringResource(id = R.string.systemDesign)
                    }) }
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
                            checked = viewModel.getUltraDarkValue(),
                            onCheckedChange = { newValue ->
                                viewModel.setUltraDarkValue(newValue)
                                context2.recreate()
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
                            checked = viewModel.getMultiLineValue(),
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
                            checked = viewModel.getShowLengthValue(),
                            onCheckedChange = { newValue ->
                                viewModel.setShowLengthValue(newValue)
                            }
                        )
                    }
                )
                if (key.isEmpty()) {
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
                }
                if (key.isEmpty()) {
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
                }
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
                            val url = "https://github.com/talosross/SummaryYou"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
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
                                "https://play.google.com/store/apps/details?id=com.talosross.summaryyou"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                        .fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
                    headlineContent = { Text(stringResource(id = R.string.googlePlay)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.round_star_rate_24),
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.googlePlayDescription)) },
                )
                Text(text = "Version ${getVersionName(context)} - ${viewModel.getModelValue()}",
                    modifier = Modifier
                        .align(alignment = CenterHorizontally)
                        .padding(top = 10.dp))
                Text(text = stringResource(id = R.string.madeBy),
                        modifier = Modifier
                            .align(alignment = CenterHorizontally)
                            .clickable {
                                val url = "https://github.com/talosross"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                )
            }
        }
    }
}

fun getVersionName(context: Context): String? {
    val packageInfo: PackageInfo
    packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        throw RuntimeException(e)
    }
    return packageInfo.versionName
}

fun getVersionCode(context: Context): Int {
    val packageInfo: PackageInfo
    packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        throw RuntimeException(e)
    }
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
            .fillMaxWidth(),
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

