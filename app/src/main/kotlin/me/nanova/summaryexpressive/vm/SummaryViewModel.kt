package me.nanova.summaryexpressive.vm

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.LLMHandler
import me.nanova.summaryexpressive.llm.Prompts
import me.nanova.summaryexpressive.llm.YouTube
import me.nanova.summaryexpressive.model.SummaryException
import me.nanova.summaryexpressive.model.SummaryResult
import me.nanova.summaryexpressive.model.TextSummary
import me.nanova.summaryexpressive.util.extractTextFromArticleUrl
import java.net.URL
import java.util.Locale
import java.util.UUID

sealed class SummarySource {
    data class Document(val filename: String?, val content: String) : SummarySource()
    data class Article(val url: String) : SummarySource()
    data class Video(val url: String) : SummarySource()
    data class Text(val content: String) : SummarySource()
    data object None : SummarySource()

    val contentType: Prompts.ContentType?
        get() = when (this) {
            is Article -> Prompts.ContentType.ARTICLE
            is Document -> Prompts.ContentType.DOCUMENT
            is Text -> Prompts.ContentType.TEXT
            is Video -> Prompts.ContentType.VIDEO_TRANSCRIPT
            is None -> null
        }
}

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    val textSummaries = mutableStateListOf<TextSummary>()

    // Original Language in summary
    private val _useOriginalLanguage = MutableStateFlow(true)
    val useOriginalLanguage: StateFlow<Boolean> = _useOriginalLanguage.asStateFlow()
    fun setUseOriginalLanguageValue(newValue: Boolean) =
        savePreference(UserPreferencesRepository::setUseOriginalLanguage, newValue)

    // Multiline URL-Field
    private val _multiLine = MutableStateFlow(true)
    val multiLine: StateFlow<Boolean> = _multiLine.asStateFlow()
    fun setMultiLineValue(newValue: Boolean) =
        savePreference(UserPreferencesRepository::setMultiLine, newValue)

    // UltraDark - Mode
    private val _ultraDark = MutableStateFlow(false)
    val ultraDark: StateFlow<Boolean> = _ultraDark.asStateFlow()
    fun setUltraDarkValue(newValue: Boolean) =
        savePreference(UserPreferencesRepository::setUltraDark, newValue)

    // DesignNumber for Dark, Light or System
    private val _designNumber = MutableStateFlow(0)
    val designNumber: StateFlow<Int> = _designNumber.asStateFlow()
    fun setDesignNumber(newValue: Int) =
        savePreference(UserPreferencesRepository::setDesignNumber, newValue)

    // API Key
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()
    fun setApiKeyValue(newValue: String) =
        savePreference(UserPreferencesRepository::setApiKey, newValue)

    // API base url
    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()
    fun setBaseUrlValue(newValue: String) =
        savePreference(UserPreferencesRepository::setBaseUrl, newValue)

    // AI-Model
    private val _model = MutableStateFlow(AIProvider.OPENAI)
    val model: StateFlow<AIProvider> = _model.asStateFlow()
    fun setModelValue(newValue: String) =
        savePreference(UserPreferencesRepository::setModel, newValue)

    // OnboardingScreen
    private val _showOnboardingScreen = MutableStateFlow(false)
    val showOnboardingScreen: StateFlow<Boolean> = _showOnboardingScreen.asStateFlow()
    fun setShowOnboardingScreenValue(newValue: Boolean) =
        savePreference(UserPreferencesRepository::setShowOnboarding, newValue)

    // Show length
    private val _showLength = MutableStateFlow(true)
    val showLength: StateFlow<Boolean> = _showLength.asStateFlow()
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(UserPreferencesRepository::setShowLength, newValue)

    // Length number
    private val _lengthNumber = MutableStateFlow(0)
    val lengthNumber: StateFlow<Int> = _lengthNumber.asStateFlow()
    fun setShowLengthNumberValue(newValue: Int) =
        savePreference(UserPreferencesRepository::setShowLengthNumber, newValue)

    init {
        loadSummaries()
        loadPreferences()
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            UserPreferencesRepository.getTextSummaries(getApplication()).collect { summariesJson ->
                val type = object : TypeToken<List<TextSummary>>() {}.type
                val summaries =
                    kotlin.runCatching { Gson().fromJson<List<TextSummary>>(summariesJson, type) }
                        .getOrNull()
                textSummaries.clear()
                summaries?.let { textSummaries.addAll(it) }
            }
        }
    }

    private fun loadPreferences() {
        UserPreferencesRepository.getUseOriginalLanguage(getApplication())
            .collectInto(_useOriginalLanguage)
        UserPreferencesRepository.getMultiLine(getApplication()).collectInto(_multiLine)
        UserPreferencesRepository.getUltraDark(getApplication()).collectInto(_ultraDark)
        UserPreferencesRepository.getDesignNumber(getApplication()).collectInto(_designNumber)
        UserPreferencesRepository.getApiKey(getApplication()).collectInto(_apiKey)
        UserPreferencesRepository.getBaseUrl(getApplication()).collectInto(_baseUrl)
        UserPreferencesRepository.getModel(getApplication())
            .collectInto(_model) { AIProvider.valueOf(it) }
        UserPreferencesRepository.getShowOnboarding(getApplication())
            .collectInto(_showOnboardingScreen)
        UserPreferencesRepository.getShowLength(getApplication()).collectInto(_showLength)
        UserPreferencesRepository.getShowLengthNumber(getApplication())
            .collectInto(_lengthNumber)
    }

    fun addTextSummary(title: String?, author: String?, text: String?, youtubeLink: Boolean) {
        val nonNullTitle = title ?: ""
        val nonNullAuthor = author ?: ""

        if (!text.isNullOrBlank() && text != "invalid link") {
            val uniqueId = UUID.randomUUID().toString()
            val newTextSummary =
                TextSummary(uniqueId, nonNullTitle, nonNullAuthor, text, youtubeLink)
            textSummaries.add(newTextSummary)
            // Save text data in SharedPreferences
            saveTextSummaries()
        }
    }

    private fun saveTextSummaries() {
        viewModelScope.launch {
            val textSummariesJson = Gson().toJson(textSummaries)
            UserPreferencesRepository.setTextSummaries(getApplication(), textSummariesJson)
        }
    }

    fun removeTextSummary(id: String) {
        // Find the TextSummary object to remove based on id
        val textSummaryToRemove = textSummaries.firstOrNull { it.id == id }

        // Check whether a matching TextSummary object was found and remove it
        textSummaryToRemove?.let {
            textSummaries.remove(it)

            // After removing, save the updated ViewModel (if necessary)
            saveTextSummaries()
        }
    }

    fun searchTextSummary(searchText: String): List<String> {
        return textSummaries
            .filter {
                it.title.contains(searchText, ignoreCase = true) or it.author.contains(
                    searchText,
                    ignoreCase = true
                ) or it.text.contains(searchText, ignoreCase = true)
            }
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.toList()
            ?: emptyList()
    }

    // --- Summarization State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentSummaryResult = MutableStateFlow<SummaryResult?>(null)
    val currentSummaryResult: StateFlow<SummaryResult?> = _currentSummaryResult.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    fun clearCurrentSummary() {
        _currentSummaryResult.value = null
        _error.value = null
    }

    fun summarize(source: SummarySource) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentSummaryResult.value = null
            _error.value = null
            try {
                _currentSummaryResult.value = summarizeInternal(source)
            } catch (e: Exception) {
                Log.e("SummaryViewModel", "Failed to summerize", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun summarizeInternal(source: SummarySource): SummaryResult {
        // API key is required for all LLM calls, regardless of input type
        if (apiKey.value.isEmpty()) {
            throw SummaryException.NoKeyException
        }

        if (source is SummarySource.None) {
            throw SummaryException.NoContentException // Or a more specific "unsupported type"
        }

        val content = when (source) {
            is SummarySource.Article -> source.url
            is SummarySource.Document -> source.content
            is SummarySource.Text -> source.content
            is SummarySource.Video -> source.url
            is SummarySource.None -> throw SummaryException.NoContentException
        }

        if (content.isBlank()) {
            throw SummaryException.NoContentException
        }

        if (source is SummarySource.Article || source is SummarySource.Video) {
            if (runCatching { URL(content).host }.getOrNull().isNullOrEmpty()) {
                throw SummaryException.InvalidLinkException
            }
        }

        return when (source) {
            is SummarySource.Document -> summarizeDocument(source)
            is SummarySource.Article -> summarizeArticle(source)
            is SummarySource.Video -> summarizeYouTubeVideo(source)
            is SummarySource.Text -> summarizeText(source)
            is SummarySource.None -> throw SummaryException.NoContentException
        }
    }

    private suspend fun summarizeYouTubeVideo(source: SummarySource.Video): SummaryResult {
        try {
            val videoId = YouTube.extractVideoId(source.url)
                ?: throw SummaryException.InvalidLinkException

            val detailsResult = YouTube.getVideoDetails(videoId)
                ?: throw SummaryException.NoContentException // Or more specific

            val (details, playerResponse) = detailsResult

            val contentToSummarize: String
            val systemPrompt: String

            val currentLocale: Locale =
                getApplication<Application>().resources.configuration.locales[0]
            var languageName =
                if (useOriginalLanguage.value) currentLocale.displayLanguage else "English"

            when (model.value) {
                AIProvider.GEMINI -> {
                    contentToSummarize = source.url // Gemini uses URL for YouTube videos
                    systemPrompt = Prompts.geminiPrompt(
                        source.contentType!!,
                        details.title,
                        lengthNumber.value,
                        languageName
                    )
                }

                AIProvider.OPENAI, AIProvider.GROQ -> {
                    val transcriptData = YouTube.getTranscript(videoId, playerResponse)
                        ?: throw SummaryException.NoTranscriptException

                    contentToSummarize = transcriptData.first // The transcript text
                    val langCode = transcriptData.second
                    languageName = if (useOriginalLanguage.value)
                        Locale.forLanguageTag(langCode.split("-").first()).displayLanguage
                    else "English"

                    systemPrompt = when (model.value) {
                        AIProvider.OPENAI -> Prompts.openAIPrompt(
                            source.contentType!!,
                            details.title,
                            lengthNumber.value,
                            languageName
                        )

                        AIProvider.GROQ -> Prompts.groqPrompt(
                            source.contentType!!,
                            details.title,
                            lengthNumber.value,
                            languageName
                        )

                        else -> "" // Should not happen
                    }
                }
            }

            if (systemPrompt.isBlank()) {
                throw SummaryException.UnknownException("Error: Unsupported model for YouTube summary.")
            }

            return executeSummary(
                contentToSummarize,
                systemPrompt,
                details.title,
                details.author,
                isYoutube = true
            )

        } catch (e: SummaryException) {
            throw e // re-throw known exceptions
        } catch (e: Exception) {
            Log.e("SummaryViewModel", "Failed to summerize youtube", e)
            throw SummaryException.UnknownException("Error: ${e.message}")
        }
    }

    private suspend fun summarizeArticle(source: SummarySource.Article): SummaryResult =
        withContext(Dispatchers.IO) {
            try {
                val article = extractTextFromArticleUrl(source.url)
                val transcript = article.text
                val title = article.title
                val author = article.author

                val language: String = if (useOriginalLanguage.value) {
                    "the same language as the text"
                } else {
                    getApplication<Application>().resources.configuration.locales[0].getDisplayLanguage(
                        Locale.ENGLISH
                    )
                }

                val systemPrompt = when (model.value) {
                    AIProvider.OPENAI -> Prompts.openAIPrompt(
                        source.contentType!!,
                        title,
                        lengthNumber.value,
                        language
                    )

                    AIProvider.GEMINI -> Prompts.geminiPrompt(
                        source.contentType!!,
                        title,
                        lengthNumber.value,
                        language
                    )

                    AIProvider.GROQ -> Prompts.groqPrompt(
                        source.contentType!!,
                        title,
                        lengthNumber.value,
                        language
                    )
                }

                return@withContext executeSummary(transcript, systemPrompt, title, author, isYoutube = false)

            } catch (e: SummaryException) {
                throw e
            } catch (e: Exception) {
                Log.e("SummaryViewModel", "Failed to summerize article", e)
                throw SummaryException.UnknownException("Error: ${e.message}")
            }
        }

    private suspend fun summarizeDocument(source: SummarySource.Document): SummaryResult {
        if (source.content.length < 100) {
            throw SummaryException.TooShortException
        }

        val language: String = if (useOriginalLanguage.value) {
            "the same language as the text"
        } else {
            getApplication<Application>().resources.configuration.locales[0].getDisplayLanguage(
                Locale.ENGLISH
            )
        }

        val systemPrompt = when (model.value) {
            AIProvider.OPENAI -> Prompts.openAIPrompt(
                source.contentType!!,
                null,
                lengthNumber.value,
                language
            )

            AIProvider.GEMINI -> Prompts.geminiPrompt(
                source.contentType!!,
                null,
                lengthNumber.value,
                language
            )

            AIProvider.GROQ -> Prompts.groqPrompt(
                source.contentType!!,
                null,
                lengthNumber.value,
                language
            )
        }

        return executeSummary(
            source.content,
            systemPrompt,
            title = source.filename ?: "Document",
            author = "Document",
            isYoutube = false
        )
    }

    private suspend fun summarizeText(source: SummarySource.Text): SummaryResult {
        val language: String = if (useOriginalLanguage.value) {
            "the same language as the text"
        } else {
            getApplication<Application>().resources.configuration.locales[0].getDisplayLanguage(
                Locale.ENGLISH
            )
        }

        val systemPrompt = when (model.value) {
            AIProvider.OPENAI -> Prompts.openAIPrompt(
                source.contentType!!,
                null,
                lengthNumber.value,
                language
            )

            AIProvider.GEMINI -> Prompts.geminiPrompt(
                source.contentType!!,
                null,
                lengthNumber.value,
                language
            )

            AIProvider.GROQ -> Prompts.groqPrompt(
                source.contentType!!,
                null,
                lengthNumber.value,
                language
            )
        }

        return executeSummary(
            source.content,
            systemPrompt,
            title = null,
            author = null,
            isYoutube = false
        )
    }

    private suspend fun llmSummarize(textToSummarize: String, systemPrompt: String): String =
        withContext(Dispatchers.IO) {
            val currentModel = model.value
            val currentApiKey = apiKey.value
            val currentBaseUrl = baseUrl.value

            val summary = LLMHandler.generateContent(
                provider = currentModel,
                apiKey = currentApiKey,
                instructions = systemPrompt,
                text = textToSummarize,
                baseUrl = if (currentModel == AIProvider.OPENAI) currentBaseUrl else null
            )

            if (summary.startsWith("Error:")) {
                // The handlers already return a string starting with "Error: " on failure.
                if (summary.contains(
                        "API key not valid",
                        ignoreCase = true
                    ) || summary.contains("API key is invalid", ignoreCase = true)
                ) {
                    throw SummaryException.IncorrectKeyException
                }
                if (summary.contains("rate limit", ignoreCase = true)) {
                    throw SummaryException.RateLimitException
                }
                // For other errors from LLMHandler, wrap them
                throw SummaryException.UnknownException(summary)
            }
            return@withContext summary
        }

    private suspend fun executeSummary(
        textToSummarize: String,
        systemPrompt: String,
        title: String?,
        author: String?,
        isYoutube: Boolean,
    ): SummaryResult {
        val summary = llmSummarize(textToSummarize, systemPrompt)
        val resultSummary = summary.trim()

        val result = SummaryResult(title, author, resultSummary, isYoutubeLink = isYoutube)
        addTextSummary(result.title, result.author, result.summary, isYoutube)
        return result
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

    private fun <T> savePreference(setter: suspend (android.content.Context, T) -> Unit, value: T) {
        viewModelScope.launch {
            setter(getApplication(), value)
        }
    }
}