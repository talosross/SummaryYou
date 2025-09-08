package me.nanova.summaryexpressive

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.nanova.summaryexpressive.ui.AppNavigation
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppStartAction
import me.nanova.summaryexpressive.vm.AppViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Check if a link has been shared
        handleIntent(intent)

        setContent {
            val navController = rememberNavController()
            val settingsState by viewModel.settingsUiState.collectAsState()
            val startDestination by viewModel.startDestination.collectAsState()

            SummaryExpressiveTheme(
                darkTheme = when (settingsState.theme) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = settingsState.dynamicColor
            ) {
                AppNavigation(
                    navController = navController,
                    startDestination = startDestination,
                    appViewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    val content = intent.getStringExtra(Intent.EXTRA_TEXT)
                    viewModel.onEvent(AppStartAction(content))
                }
            }
            Intent.ACTION_VIEW -> {
                if (intent.data?.host != "clipboard") return
                // To avoid re-triggering on configuration change, we clear the data.
                intent.data = null
                // Postpone clipboard access until the window has focus.
                window.decorView.post {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip?.getItemAt(0)?.text?.let {
                        viewModel.onEvent(
                            AppStartAction(
                                content = it.toString(),
                                autoTrigger = true
                            )
                        )
                    }
                }
            }
        }
    }
}