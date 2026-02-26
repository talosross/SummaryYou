package com.talosross.summaryyou.vm

import com.talosross.summaryyou.llm.AIProvider
import com.talosross.summaryyou.llm.SummaryLength
import com.talosross.summaryyou.llm.SummaryOutput

data class AppStartAction(val content: String? = null, val autoTrigger: Boolean = false)

data class SettingsUiState(
    val useOriginalLanguage: Boolean = true,
    val dynamicColor: Boolean = true,
    val theme: Int = 0,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val aiProvider: AIProvider = AIProvider.OPENAI,
    val model: String? = null,
    val autoExtractUrl: Boolean = true,
    val showLength: Boolean = true,
    val summaryLength: SummaryLength = SummaryLength.MEDIUM,
    val sessData: String = "",
    val sessDataExpires: Long = 0L,
    /** Whether a server-side proxy is available (gms flavor with proxy URL configured). */
    val hasProxy: Boolean = false,
    val developerMode: Boolean = false,
)

data class SummarizationState(
    val isLoading: Boolean = false,
    val summaryResult: SummaryOutput? = null,
    val error: Throwable? = null
)
