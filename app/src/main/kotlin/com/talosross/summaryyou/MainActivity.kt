package com.talosross.summaryyou

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.talosross.summaryyou.ui.AppNavigation
import com.talosross.summaryyou.ui.theme.SummaryYouTheme
import com.talosross.summaryyou.vm.AppStartAction
import com.talosross.summaryyou.vm.AppViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

            SummaryYouTheme(
                darkTheme = when (settingsState.theme) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = settingsState.dynamicColor
            ) {
                if (startDestination == null) {
                    return@SummaryYouTheme
                }
                AppNavigation(
                    navController = navController,
                    startDestination = startDestination!!,
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
                val type = intent.type ?: ""
                if (type.startsWith("application/") || type.startsWith("image/")) {
                    @Suppress("DEPRECATION")
                    val contentUri: Uri? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        } else {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM)
                        }
                    contentUri?.let { viewModel.onEvent(AppStartAction(it.toString())) }
                } else if (type == "text/plain") {
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
