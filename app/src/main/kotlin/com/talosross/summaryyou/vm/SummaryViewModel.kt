package com.talosross.summaryyou.vm

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.talosross.summaryyou.data.HistoryRepository
import com.talosross.summaryyou.di.FlavorConfig
import com.talosross.summaryyou.llm.AIProvider
import com.talosross.summaryyou.llm.LLMHandler
import com.talosross.summaryyou.llm.ProxySummarizer
import com.talosross.summaryyou.llm.SummaryLength
import com.talosross.summaryyou.llm.SummaryOutput
import com.talosross.summaryyou.llm.tools.BiliBiliSubtitleTool
import com.talosross.summaryyou.llm.tools.YouTubeTranscriptTool
import com.talosross.summaryyou.llm.tools.getFileName
import com.talosross.summaryyou.model.HistorySummary
import com.talosross.summaryyou.model.SummaryException
import com.talosross.summaryyou.model.SummaryType
import com.talosross.summaryyou.model.VideoSubtype
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val llmHandler: LLMHandler,
    private val proxySummarizer: ProxySummarizer,
    private val flavorConfig: FlavorConfig,
    private val application: Application,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    sealed class SummarySource {
        data class Video(val url: String) : SummarySource()
        data class Article(val url: String) : SummarySource()
        data class Text(val content: String) : SummarySource()
        data class Document(val filename: String, val uri: String) : SummarySource()
        object None : SummarySource()
    }

    private val _summarizationState = MutableStateFlow(SummarizationState())
    val summarizationState: StateFlow<SummarizationState> = _summarizationState.asStateFlow()

    private var summarizationJob: Job? = null

    fun clearCurrentSummary() {
        _summarizationState.update { it.copy(summaryResult = null, error = null) }
    }

    fun cancelSummarization() {
        summarizationJob?.cancel()
        summarizationJob = null
        _summarizationState.update { it.copy(isLoading = false) }
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

    private val isFileUri =
        { input: String -> input.startsWith("content://") || input.startsWith("file://") }

    fun summarize(text: String, settings: SettingsUiState) {
        summarizationJob?.cancel()
        summarizationJob = viewModelScope.launch {
            val source = if (isFileUri(text)) {
                val uri = text.toUri()
                val filename = getFileName(application, uri)
                SummarySource.Document(filename, text)
            } else {
                val processedText = if (settings.autoExtractUrl) extractHttpUrl(text) else text
                when {
                    processedText.startsWith("http://", ignoreCase = true)
                            || processedText.startsWith("https://", ignoreCase = true) ->
                        if (YouTubeTranscriptTool.isYouTubeLink(processedText)
                            || BiliBiliSubtitleTool.isBiliBiliLink(processedText)
                        )
                            SummarySource.Video(processedText)
                        else SummarySource.Article(processedText)

                    processedText.isNotBlank() -> SummarySource.Text(processedText)
                    else -> SummarySource.None
                }
            }

            summarizeInternal(source, settings)
        }
    }

    private suspend fun summarizeInternal(source: SummarySource, settings: SettingsUiState) {
        _summarizationState.value = SummarizationState(isLoading = true)
        try {
            if (source is SummarySource.None) {
                throw SummaryException.NoContentException()
            }

            val currentApiKey = settings.apiKey
            val isIntegrated = settings.aiProvider == AIProvider.INTEGRATED
            val useProxy = (currentApiKey.isNullOrEmpty() || isIntegrated) && flavorConfig.proxyBaseUrl != null

            if (currentApiKey.isNullOrEmpty() && !useProxy) {
                throw SummaryException.NoKeyException()
            }

            val appLanguage = application.resources.configuration.locales[0]

            val summaryOutput = if (useProxy) {
                summarizeViaProxy(source, settings, appLanguage)
            } else {
                summarizeViaAgent(source, settings, currentApiKey!!, appLanguage)
            }

            _summarizationState.update { it.copy(summaryResult = summaryOutput) }
            saveSummaryToHistory(summaryOutput, settings.summaryLength, source)

        } catch (e: CancellationException) {
            throw e
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

    /**
     * Standard path: use the koog agent with the user's own API key.
     */
    private suspend fun summarizeViaAgent(
        source: SummarySource,
        settings: SettingsUiState,
        apiKey: String,
        appLanguage: java.util.Locale,
    ): SummaryOutput {
        val agent = llmHandler.getSummarizationAgent(
            provider = settings.aiProvider,
            apiKey = apiKey,
            baseUrl = settings.baseUrl,
            model = settings.model,
            summaryLength = settings.summaryLength,
            useContentLanguage = settings.useOriginalLanguage,
            appLanguage = appLanguage
        )

        return withContext(Dispatchers.IO) {
            agent.run(source)
        }
    }

    /**
     * Proxy path: extract content locally, then send to Cloudflare Worker proxy.
     * Used in gms flavor when user has no API key set.
     */
    private suspend fun summarizeViaProxy(
        source: SummarySource,
        settings: SettingsUiState,
        appLanguage: java.util.Locale,
    ): SummaryOutput {
        // Extract content locally using the existing tools via LLMHandler
        val extractedContent = withContext(Dispatchers.IO) {
            llmHandler.extractContent(source)
        }

        val summaryText = withContext(Dispatchers.IO) {
            proxySummarizer.summarize(
                content = extractedContent.content,
                length = settings.summaryLength,
                useContentLanguage = settings.useOriginalLanguage,
                appLanguage = appLanguage.getDisplayLanguage(java.util.Locale.ENGLISH),
                model = settings.model,
            )
        }

        val isYoutube = source is SummarySource.Video &&
                YouTubeTranscriptTool.isYouTubeLink((source).url)
        val isBiliBili = source is SummarySource.Video &&
                BiliBiliSubtitleTool.isBiliBiliLink((source).url)
        val sourceLink = when (source) {
            is SummarySource.Video -> source.url
            is SummarySource.Article -> source.url
            else -> null
        }

        return SummaryOutput(
            title = extractedContent.title,
            author = extractedContent.author,
            summary = summaryText,
            sourceLink = sourceLink,
            isYoutubeLink = isYoutube,
            isBiliBiliLink = isBiliBili,
            length = settings.summaryLength,
        )
    }

    private suspend fun saveSummaryToHistory(
        summaryOutput: SummaryOutput,
        summaryLength: SummaryLength,
        source: SummarySource,
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