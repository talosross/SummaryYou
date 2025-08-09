package me.nanova.summaryexpressive

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.nanova.summaryexpressive.ui.AppNavigation
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.AppStartAction
import me.nanova.summaryexpressive.vm.SummaryViewModel


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: SummaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Check if a link has been shared
        handleIntent(intent)

        setContent {
            val theme by viewModel.theme.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            val showOnboarding by viewModel.showOnboardingScreen.collectAsState()

            SummaryExpressiveTheme(theme = theme, dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController, showOnboarding)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when {
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                viewModel.setAppStartAction(AppStartAction(content = intent.getStringExtra(Intent.EXTRA_TEXT)))
            }

            intent?.action == Intent.ACTION_VIEW && intent.data?.host == "clipboard" -> {
                // To avoid re-triggering on configuration change, we clear the data.
                intent.data = null
                // Postpone clipboard access until the window has focus.
                window.decorView.post {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip?.getItemAt(0)?.text?.let {
                        viewModel.setAppStartAction(
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