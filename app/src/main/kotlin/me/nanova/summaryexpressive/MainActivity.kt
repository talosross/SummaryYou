package me.nanova.summaryexpressive

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import me.nanova.summaryexpressive.ui.AppNavigation
import me.nanova.summaryexpressive.ui.page.OnboardingScreen
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import me.nanova.summaryexpressive.vm.SummaryViewModel


class MainActivity : ComponentActivity() {
    private var sharedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Check if a link has been shared
        val intent: Intent? = intent
        if (Intent.ACTION_SEND == intent?.action && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        setContent {
            val viewModel: SummaryViewModel = viewModel()
            val design by viewModel.designNumber.collectAsState()
            val oLedMode by viewModel.ultraDark.collectAsState()
            val showOnboarding by viewModel.showOnboardingScreen.collectAsState()

            SummaryExpressiveTheme(design = design, oLedModeEnabled = oLedMode) {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showOnboarding) {
                        OnboardingScreen(onDone = {
                            viewModel.setShowOnboardingScreenValue(false)
                        })
                    } else {
                        AppNavigation(navController, viewModel, sharedUrl)
                    }
                }
            }
        }
    }
}