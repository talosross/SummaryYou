package me.nanova.summaryexpressive.vm

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nanova.summaryexpressive.data.HistoryRepository
import me.nanova.summaryexpressive.llm.LLMHandler
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.llm.tools.getFileName
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.model.SummaryType
import me.nanova.summaryexpressive.model.VideoSubtype
import java.net.URL
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val llmHandler: LLMHandler,
    private val application: Application,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private sealed class SummarySource {
        data class Video(val url: String) : SummarySource()
        data class Article(val url: String) : SummarySource()
        data class Text(val content: String) : SummarySource()
        data class Document(val filename: String, val uri: String) : SummarySource()
        object None : SummarySource()
    }

    private val _summarizationState = MutableStateFlow(SummarizationState())
    val summarizationState: StateFlow<SummarizationState> = _summarizationState.asStateFlow()

    fun clearCurrentSummary() {
        _summarizationState.update { it.copy(summaryResult = null, error = null) }
    }

    private fun extractHttpUrl(text: String): String {
        val urlRegex = Regex(
            "(?:^|\\W)((http|https)://)" + // Protocol
                    "([\\w\\-]+\\.){1,}" + // Domain name
                    "([\\w\\-]+)" + // Top-level domain
                    "([^\\s<>\"#%{}|\\\\^`]*)" // Path, query, and fragment
        )
        return urlRegex.find(text)?.value?.trim() ?: text
    }

    fun summarize(text: String, settings: SettingsUiState) {
        val processedText = if (settings.autoExtractUrl) extractHttpUrl(text) else text
        processedText.let {
            val source = when {
                it.startsWith("http://", ignoreCase = true)
                        || it.startsWith("https://", ignoreCase = true) ->
                    if (YouTubeTranscriptTool.isYouTubeLink(it) || it.contains("bilibili.com")) SummarySource.Video(
                        it
                    )
                    else SummarySource.Article(it)

                it.isNotBlank() -> SummarySource.Text(it)
                else -> SummarySource.None
            }
            viewModelScope.launch {
                summarizeInternal(source, settings)
            }
        }
    }

    fun summarize(uri: Uri, settings: SettingsUiState) {
        viewModelScope.launch {
            val filename = getFileName(application, uri)
            val source = SummarySource.Document(filename, uri.toString())
            summarizeInternal(source, settings)
        }
    }

    private suspend fun summarizeInternal(source: SummarySource, settings: SettingsUiState) {
        _summarizationState.value = SummarizationState(isLoading = true)
        try {
            val currentApiKey = settings.apiKey
            if (currentApiKey.isNullOrEmpty()) {
                throw SummaryException.NoKeyException()
            }

            val language = if (settings.useOriginalLanguage) "the same language as the content"
            else application.resources.configuration.locales[0].getDisplayLanguage(Locale.ENGLISH)

            val agent = llmHandler.getSummarizationAgent(
                provider = settings.aiProvider,
                apiKey = currentApiKey,
                baseUrl = settings.baseUrl,
                model = settings.model,
                summaryLength = settings.summaryLength,
                useOriginalLanguage = settings.useOriginalLanguage,
                language = language
            )

            val inputString = prepareSummarizationInput(source)

            val summaryOutput = withContext(Dispatchers.IO) {
                agent.run(inputString)
            }

            _summarizationState.update { it.copy(summaryResult = summaryOutput) }
            saveSummaryToHistory(summaryOutput, settings.summaryLength, source)

        } catch (e: Exception) {
            Log.e("LLMViewModel", "Failed to summarize", e)
            val error =
                e as? SummaryException
                    ?: SummaryException.UnknownException(
                        e.message ?: "An unknown error occurred."
                    )
            _summarizationState.update { it.copy(error = error) }
        } finally {
            _summarizationState.update { it.copy(isLoading = false) }
        }
    }

    private fun prepareSummarizationInput(source: SummarySource): String {
        val inputString = when (source) {
            is SummarySource.Article -> source.url
            is SummarySource.Document -> source.uri
            is SummarySource.Text -> source.content
            is SummarySource.Video -> source.url
            is SummarySource.None -> ""
        }

        if (inputString.isBlank()) {
            throw SummaryException.NoContentException()
        }

        if (source is SummarySource.Article || source is SummarySource.Video) {
            if (runCatching { URL(inputString).host }.getOrNull().isNullOrEmpty()) {
                throw SummaryException.InvalidLinkException()
            }
        }
        return inputString
    }

    private suspend fun saveSummaryToHistory(
        summaryOutput: SummaryOutput,
        summaryLength: SummaryLength,
        source: SummarySource
    ) {
        if (source is SummarySource.None) return

        val type: SummaryType
        var subtype: VideoSubtype? = null
        var sourceLink: String? = null
        var sourceText: String? = null

        when (source) {
            is SummarySource.Article -> {
                type = SummaryType.ARTICLE
                sourceLink = source.url
            }

            is SummarySource.Document -> {
                type = SummaryType.DOCUMENT
                sourceLink = source.uri
            }

            is SummarySource.Text -> {
                type = SummaryType.TEXT
                sourceText = source.content
            }

            is SummarySource.Video -> {
                type = SummaryType.VIDEO
                sourceLink = source.url
                subtype = when {
                    YouTubeTranscriptTool.isYouTubeLink(source.url) -> VideoSubtype.YOUTUBE
                    source.url.contains("bilibili.com") -> VideoSubtype.BILIBILI
                    else -> null
                }
            }

            is SummarySource.None -> return
        }

        val summary = HistorySummary(
            title = summaryOutput.title,
            author = summaryOutput.author,
            summary = summaryOutput.summary.trim(),
            length = summaryLength,
            type = type,
            subtype = subtype,
            sourceLink = sourceLink,
            sourceText = sourceText
        )
        if (summary.summary.isNotBlank() && summary.summary != "invalid link") {
            historyRepository.addSummary(summary)
        }
    }
}