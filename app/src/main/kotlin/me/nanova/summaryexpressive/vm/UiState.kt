package me.nanova.summaryexpressive.vm

import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput

data class AppStartAction(val content: String? = null, val autoTrigger: Boolean = false)

data class SettingsUiState(
    val useOriginalLanguage: Boolean = true,
    val dynamicColor: Boolean = true,
    val theme: Int = 0,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val model: AIProvider = AIProvider.OPENAI,
    val showOnboarding: Boolean = false,
    val showLength: Boolean = true,
    val summaryLength: SummaryLength = SummaryLength.MEDIUM
)

data class SummarizationState(
    val isLoading: Boolean = false,
    val summaryResult: SummaryOutput? = null,
    val error: Throwable? = null
)

sealed class SummarySource {
    data class Document(val filename: String?, val uri: String) : SummarySource()
    data class Article(val url: String) : SummarySource()
    data class Video(val url: String) : SummarySource()
    data class Text(val content: String) : SummarySource()
    data object None : SummarySource()
}