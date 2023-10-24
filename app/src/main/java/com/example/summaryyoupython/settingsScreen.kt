package com.example.summaryyoupython

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController


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
                    IconButton(onClick = {navController.navigate("home")}) {
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
    val range = 1..10
    val context = LocalContext.current // Holen Sie sich den Context aus AmbientContext
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(id = R.string.design)) },
            text = {
                Column {
                    RadioButtonItem("System", selected = false) {
                        // Hier können Sie die Logik hinzufügen, um den Systemmodus einzustellen
                    }
                    RadioButtonItem("Light", selected = false) {
                        // Hier können Sie die Logik hinzufügen, um den Light-Modus einzustellen
                    }
                    RadioButtonItem("Dark", selected = false) {
                        // Hier können Sie die Logik hinzufügen, um den Dark-Modus einzustellen
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
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
                ListItem(
                    modifier = Modifier
                        .clickable(onClick = { showDialog = showDialog.not() })
                        .fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
                    headlineContent = { Text(stringResource(id = R.string.design)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_dark_mode_24),
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.githubDescription)) }
                )
                ListItem(
                    modifier = Modifier.fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
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
                            checked = true, // Hier können Sie den Zustand des Schalters anpassen
                            onCheckedChange = { /* Hier können Sie die Logik für den Schalter einfügen */ }
                        )
                    }
                )
                ListItem(
                    modifier = Modifier.fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
                    headlineContent = { Text(stringResource(id = R.string.use2lines)) },
                    supportingContent = { Text(stringResource(id = R.string.use2linesDescription)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_format_line_spacing_24),
                            contentDescription = "Localized description",
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = true, // Hier können Sie den Zustand des Schalters anpassen
                            onCheckedChange = { /* Hier können Sie die Logik für den Schalter einfügen */ }
                        )
                    }
                )
                ListItem(
                    modifier = Modifier.fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
                    headlineContent = { Text("Github") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Localized description",
                        )
                    },
                    supportingContent = { Text(stringResource(id = R.string.githubDescription)) }
                )
                Text(text = "Version ${getVersionName(context)}", modifier = Modifier.align(alignment = CenterHorizontally))
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

