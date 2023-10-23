package com.example.summaryyoupython

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(modifier: Modifier = Modifier, navController: NavHostController) {
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
        ScrollContent(innerPadding)
    }
}

@Composable
fun ScrollContent(innerPadding: PaddingValues) {
    val range = 1..10
    val context = LocalContext.current // Holen Sie sich den Context aus AmbientContext


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
                        trailingContent = {
                            Switch(
                                checked = true, // Hier können Sie den Zustand des Schalters anpassen
                                onCheckedChange = { /* Hier können Sie die Logik für den Schalter einfügen */ }
                            )
                        }
                )
                HorizontalDivider()
                ListItem(
                    modifier = Modifier.fillMaxWidth(), // Optional, um die ListItem auf die volle Breite zu strecken
                    headlineContent = { Text(stringResource(id = R.string.useUltraDark)) },
                    supportingContent = { Text(stringResource(id = R.string.useUltraDarkDescription)) },
                    trailingContent = {
                        Switch(
                            checked = true, // Hier können Sie den Zustand des Schalters anpassen
                            onCheckedChange = { /* Hier können Sie die Logik für den Schalter einfügen */ }
                        )
                    }
                )
                HorizontalDivider()
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
