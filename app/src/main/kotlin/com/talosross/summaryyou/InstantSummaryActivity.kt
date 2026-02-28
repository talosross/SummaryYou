package com.talosross.summaryyou

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import com.talosross.summaryyou.ui.component.InstantSummaryDialog
import com.talosross.summaryyou.ui.theme.SummaryYouTheme
import com.talosross.summaryyou.vm.AppViewModel
import com.talosross.summaryyou.vm.SummaryViewModel

@AndroidEntryPoint
class InstantSummaryActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val summaryViewModel: SummaryViewModel by viewModels()
    private val textToSummarizeStateFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            val settings by appViewModel.settingsUiState.collectAsState()
            val textToSummarize by textToSummarizeStateFlow.collectAsState()


            LaunchedEffect(textToSummarize, settings) {
                textToSummarize?.let {
                    if (it.isNotBlank()) {
                        summaryViewModel.summarize(it, settings)
                    }
                }
            }

            SummaryYouTheme(
                darkTheme = when (settings.theme) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = settings.dynamicColor
            ) {
                InstantSummaryDialog(
                    viewModel = summaryViewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val textToSummarize = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

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
                    contentUri?.toString()
                } else {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            }

            else -> {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            }
        }

        if (textToSummarize.isNullOrBlank()) {
            finish()
            return
        }
        textToSummarizeStateFlow.value = textToSummarize
    }
}

