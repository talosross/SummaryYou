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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.LLMHandler
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool
import me.nanova.summaryexpressive.llm.tools.getFileName
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryException
import java.net.URL
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val llmHandler: LLMHandler,
    private val application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

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
        return urlRegex.find(text)?.value?.trim() ?: ""
    }

    fun summarize(text: String, settings: SettingsUiState) {
        val processedText = if (settings.autoExtractUrl) extractHttpUrl(text) else text
        processedText.let {
            val source = when {
                it.startsWith("http://", ignoreCase = true)
                        || it.startsWith("https://", ignoreCase = true) ->
                    if (YouTubeTranscriptTool.isYouTubeLink(it)) SummarySource.Video(it)
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
                throw SummaryException.NoKeyException
            }

            val language = if (settings.useOriginalLanguage) "the same language as the content"
            else application.resources.configuration.locales[0].getDisplayLanguage(Locale.ENGLISH)

            val agent = llmHandler.getSummarizationAgent(
                provider = settings.aiProvider,
                apiKey = currentApiKey,
                baseUrl = settings.baseUrl,
                modelName = null, // TODO: user custom model selection
                summaryLength = settings.summaryLength,
                language = language
            )

            val inputString = prepareSummarizationInput(source)

            val summaryOutput = withContext(Dispatchers.IO) {
                agent.run(inputString)
            }

            _summarizationState.update { it.copy(summaryResult = summaryOutput) }
            addHistorySummary(summaryOutput, settings.summaryLength)

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
            throw SummaryException.NoContentException
        }

        if (source is SummarySource.Article || source is SummarySource.Video) {
            if (runCatching { URL(inputString).host }.getOrNull().isNullOrEmpty()) {
                throw SummaryException.InvalidLinkException
            }
        }
        return inputString
    }

    private fun addHistorySummary(summaryOutput: SummaryOutput, summaryLength: SummaryLength) {
        viewModelScope.launch {
            val summary = HistorySummary(
                id = UUID.randomUUID().toString(),
                title = summaryOutput.title,
                author = summaryOutput.author,
                summary = summaryOutput.summary.trim(),
                sourceLink = summaryOutput.sourceLink,
                isYoutubeLink = summaryOutput.isYoutubeLink,
                length = summaryLength,
                createdOn = System.currentTimeMillis()
            )
            if (summary.summary.isNotBlank() && summary.summary != "invalid link") {
                val currentSummariesJson = userPreferencesRepository.getTextSummaries().first()
                val summaries = kotlin.runCatching {
                    Json.decodeFromString<List<HistorySummary>>(currentSummariesJson)
                }.getOrDefault(emptyList())
                val updatedSummaries = listOf(summary) + summaries
                val updatedSummariesJson = Json.encodeToString(updatedSummaries)
                userPreferencesRepository.setTextSummaries(updatedSummariesJson)
            }
        }
    }
}