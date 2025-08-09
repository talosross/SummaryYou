package me.nanova.summaryexpressive.vm

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.LLMHandler
import me.nanova.summaryexpressive.llm.SummaryLength
import me.nanova.summaryexpressive.llm.SummaryOutput
import me.nanova.summaryexpressive.llm.tools.YouTubeTranscriptTool.Companion.isYouTubeLink
import me.nanova.summaryexpressive.llm.tools.getFileName
import me.nanova.summaryexpressive.model.HistorySummary
import me.nanova.summaryexpressive.model.SummaryException
import java.net.URL
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

sealed class SummarySource {
    data class Document(val filename: String?, val uri: String) : SummarySource()
    data class Article(val url: String) : SummarySource()
    data class Video(val url: String) : SummarySource()
    data class Text(val content: String) : SummarySource()
    data object None : SummarySource()
}

data class AppStartAction(val content: String? = null, val autoTrigger: Boolean = false)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val llmHandler: LLMHandler,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: Application,
) : ViewModel() {
    val historySummaries = mutableStateListOf<HistorySummary>()

    // Original Language in summary
    private val _useOriginalLanguage = MutableStateFlow(true)
    val useOriginalLanguage: StateFlow<Boolean> = _useOriginalLanguage.asStateFlow()
    fun setUseOriginalLanguageValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setUseOriginalLanguage, newValue)

    // Dynamic color
    private val _dynamicColor = MutableStateFlow(true)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()
    fun setDynamicColorValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setDynamicColor, newValue)

    // Theme for Dark, Light or System
    private val _theme = MutableStateFlow(0)
    val theme: StateFlow<Int> = _theme.asStateFlow()
    fun setTheme(newValue: Int) =
        savePreference(userPreferencesRepository::setTheme, newValue)

    // API Key
    private val _apiKey = MutableStateFlow<String?>(null)
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()
    fun setApiKeyValue(newValue: String) =
        savePreference(userPreferencesRepository::setApiKey, newValue)

    // API base url
    private val _baseUrl = MutableStateFlow<String?>(null)
    val baseUrl: StateFlow<String?> = _baseUrl.asStateFlow()
    fun setBaseUrlValue(newValue: String) =
        savePreference(userPreferencesRepository::setBaseUrl, newValue)

    // AI-Model
    private val _model = MutableStateFlow(AIProvider.OPENAI)
    val model: StateFlow<AIProvider> = _model.asStateFlow()
    fun setModelValue(newValue: String) =
        savePreference(userPreferencesRepository::setModel, newValue)

    // OnboardingScreen
    private val _showOnboardingScreen = MutableStateFlow(false)
    val showOnboardingScreen: StateFlow<Boolean> = _showOnboardingScreen.asStateFlow()
    fun setShowOnboardingScreenValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowOnboarding, newValue)

    // Show length
    private val _showLength = MutableStateFlow(true)
    val showLength: StateFlow<Boolean> = _showLength.asStateFlow()
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowLength, newValue)

    // Summary Length
    private val _summaryLength = MutableStateFlow(SummaryLength.MEDIUM)
    val summaryLength: StateFlow<SummaryLength> = _summaryLength.asStateFlow()
    fun setSummaryLength(newValue: SummaryLength) =
        savePreference(userPreferencesRepository::setSummaryLength, newValue.name)

    init {
        loadSummaries()
        loadPreferences()
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            userPreferencesRepository.getTextSummaries().collect { summariesJson ->
                if (summariesJson.isNotEmpty()) {
                    val summaries =
                        kotlin.runCatching { Json.decodeFromString<List<HistorySummary>>(summariesJson) }
                            .getOrNull()
                    historySummaries.clear()
                    summaries?.let { list ->
                        historySummaries.addAll(list.sortedByDescending { it.createdOn })
                    }
                } else {
                    historySummaries.clear()
                }
            }
        }
    }

    private fun loadPreferences() {
        userPreferencesRepository.getUseOriginalLanguage().collectInto(_useOriginalLanguage)
        userPreferencesRepository.getDynamicColor().collectInto(_dynamicColor)
        userPreferencesRepository.getTheme().collectInto(_theme)
        userPreferencesRepository.getApiKey().collectInto(_apiKey)
        userPreferencesRepository.getBaseUrl().collectInto(_baseUrl)
        userPreferencesRepository.getModel()
            .collectInto(_model) { AIProvider.valueOf(it) }
        userPreferencesRepository.getShowOnboarding()
            .collectInto(_showOnboardingScreen)
        userPreferencesRepository.getShowLength().collectInto(_showLength)
        userPreferencesRepository.getSummaryLength()
            .collectInto(_summaryLength) { SummaryLength.valueOf(it) }
    }

    private fun addHistorySummary(summary: HistorySummary) {
        if (summary.summary.isNotBlank() && summary.summary != "invalid link") {
            historySummaries.add(0, summary)
            // Save text data in SharedPreferences
            saveHistorySummaries()
        }
    }

    private fun saveHistorySummaries() {
        viewModelScope.launch {
            val historySummariesJson = Json.encodeToString(historySummaries.toList())
            userPreferencesRepository.setTextSummaries(historySummariesJson)
        }
    }

    fun removeHistorySummary(id: String) {
        // Find the TextSummary object to remove based on id
        val summaryToRemove = historySummaries.firstOrNull { it.id == id }

        // Check whether a matching TextSummary object was found and remove it
        summaryToRemove?.let {
            historySummaries.remove(it)

            // After removing, save the updated ViewModel (if necessary)
            saveHistorySummaries()
        }
    }

    fun searchHistorySummary(searchText: String): List<String> {
        return historySummaries
            .filter {
                it.title.contains(searchText, ignoreCase = true) || it.author.contains(
                    searchText,
                    ignoreCase = true
                ) || it.summary.contains(searchText, ignoreCase = true)
            }
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?: emptyList()
    }

    // --- App Start Action ---
    private val _appStartAction = MutableStateFlow(AppStartAction())
    val appStartAction: StateFlow<AppStartAction> = _appStartAction.asStateFlow()

    fun setAppStartAction(action: AppStartAction) {
        _appStartAction.value = action
    }

    fun onStartActionHandled() {
        _appStartAction.value = AppStartAction()
    }

    // --- Summarization State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSummaryResult = MutableStateFlow<SummaryOutput?>(null)
    val currentSummaryResult: StateFlow<SummaryOutput?> = _currentSummaryResult.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    fun clearCurrentSummary() {
        _currentSummaryResult.value = null
        _error.value = null
    }

    fun summarize(text: String) {
        val source = when {
            text.startsWith("http://", ignoreCase = true)
                    || text.startsWith("https://", ignoreCase = true) ->
                if (isYouTubeLink(text)) SummarySource.Video(text)
                else SummarySource.Article(text)

            text.isNotBlank() -> SummarySource.Text(text)
            else -> SummarySource.None
        }
        viewModelScope.launch {
            summarizeInternal(source)
        }
    }

    fun summarize(uri: Uri) {
        viewModelScope.launch {
            val filename = getFileName(application, uri)
            val source = SummarySource.Document(filename, uri.toString())
            summarizeInternal(source)
        }
    }

    private suspend fun summarizeInternal(source: SummarySource) {
        // Wait for preferences to be loaded before proceeding.
        apiKey.first { it != null }
        baseUrl.first { it != null }

        _isLoading.value = true
        _currentSummaryResult.value = null
        _error.value = null
        try {
            val currentApiKey = apiKey.value
            if (currentApiKey.isNullOrEmpty()) {
                throw SummaryException.NoKeyException
            }

            val language = if (useOriginalLanguage.value) "the same language as the content"
            else application.resources.configuration.locales[0].getDisplayLanguage(Locale.ENGLISH)

            val agent = llmHandler.createSummarizationAgent(
                provider = model.value,
                apiKey = currentApiKey,
                baseUrl = if (model.value == AIProvider.OPENAI) baseUrl.value else null,
                modelName = null, // TODO: user custom model selection
                summaryLength = summaryLength.value,
                language = language
            )

            val inputString = prepareSummarizationInput(source)

            val summaryOutput = withContext(Dispatchers.IO) {
                agent.run(inputString)
            }

            if (summaryOutput.summary.startsWith("Error:")) {
                val errorMsg = summaryOutput.summary
                if (errorMsg.contains("API key", ignoreCase = true))
                    throw SummaryException.IncorrectKeyException
                if (errorMsg.contains("rate limit", ignoreCase = true))
                    throw SummaryException.RateLimitException
                throw SummaryException.UnknownException(errorMsg)
            }

            _currentSummaryResult.value = summaryOutput
            addHistorySummary(
                HistorySummary(
                    id = UUID.randomUUID().toString(),
                    title = summaryOutput.title,
                    author = summaryOutput.author,
                    summary = summaryOutput.summary.trim(),
                    isYoutubeLink = summaryOutput.isYoutubeLink,
                    length = summaryLength.value,
                    createdOn = System.currentTimeMillis()
                )
            )

        } catch (e: Exception) {
            Log.e("SummaryViewModel", "Failed to summarize", e)
            _error.value =
                e as? SummaryException
                    ?: SummaryException.UnknownException(
                        e.message ?: "An unknown error occurred."
                    )
        } finally {
            _isLoading.value = false
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

    // --- Preference Handling Helpers ---
    private fun <T, R> Flow<T>.collectInto(
        stateFlow: MutableStateFlow<R>,
        transform: (T) -> R,
    ) {
        viewModelScope.launch {
            this@collectInto.collect { stateFlow.value = transform(it) }
        }
    }

    private fun <T> Flow<T>.collectInto(stateFlow: MutableStateFlow<T>) =
        collectInto(stateFlow) { it }

    private fun <T> savePreference(setter: suspend (T) -> Unit, value: T) {
        viewModelScope.launch {
            setter(value)
        }
    }
}