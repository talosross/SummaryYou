package me.nanova.summaryexpressive.vm

import android.app.Application
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
import me.nanova.summaryexpressive.model.SummaryResult
import me.nanova.summaryexpressive.model.TextSummary
import me.nanova.summaryexpressive.util.extractTextFromArticleUrl
import java.net.URL
import java.util.Locale
import java.util.UUID

class SummaryViewModel(application: Application) : AndroidViewModel(application) {
    val textSummaries = mutableStateListOf<TextSummary>()

    // Original Language in summary
    private val _useOriginalLanguage = MutableStateFlow(false)
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
    private val _showOnboardingScreen = MutableStateFlow(true)
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

    fun getAllTextSummaries(): List<String> {
        return textSummaries
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

    fun clearCurrentSummary() {
        _currentSummaryResult.value = null
    }

    fun summarize(textOrUrl: String, length: Int, isDocOrImage: Boolean, filename: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentSummaryResult.value = null // Clear previous result

            val result = summarizeInternal(textOrUrl, length, isDocOrImage, filename)

            _currentSummaryResult.value = result
            _isLoading.value = false
        }
    }

    private suspend fun summarizeInternal(
        textOrUrl: String,
        length: Int,
        isDocOrImage: Boolean,
        filename: String?
    ): SummaryResult {
        if (textOrUrl.isBlank()) {
            return SummaryResult(null, null, "Exception: no content", isError = true)
        }

        val isUrl =
            !isDocOrImage && (textOrUrl.startsWith("http") || textOrUrl.startsWith("https"))
        if (isUrl && runCatching { URL(textOrUrl).host }.getOrNull().isNullOrEmpty()) {
            return SummaryResult(textOrUrl, null, "Error: invalid link", isError = true)
        }

        // API key is required for all LLM calls, regardless of input type
        if (apiKey.value.isEmpty()) {
            return SummaryResult(null, null, "Exception: no key", isError = true)
        }

        val isYouTube = YouTube.isYouTubeLink(textOrUrl)

        return when {
            isYouTube -> summarizeYouTubeVideo(textOrUrl, length)
            isUrl -> summarizeArticle(textOrUrl, length)
            isDocOrImage -> summarizeDocument(filename ?: "Document", textOrUrl, length)
            else -> summarizeText(textOrUrl, length)
        }
    }

    private suspend fun summarizeYouTubeVideo(url: String, length: Int): SummaryResult {
        try {
            val videoId = YouTube.extractVideoId(url)
                ?: return SummaryResult(url, "YouTube", "Error: Invalid YouTube URL.", true)

            val detailsResult = YouTube.getVideoDetails(videoId)
                ?: return SummaryResult(
                    url,
                    "YouTube",
                    "Error: Could not retrieve video details.",
                    true
                )

            val (details, playerResponse) = detailsResult

            val contentToSummarize: String
            val systemPrompt: String

            val currentLocale: Locale =
                getApplication<Application>().resources.configuration.locales[0]
            var languageName =
                if (useOriginalLanguage.value) currentLocale.displayLanguage else "English"

            when (model.value) {
                AIProvider.GEMINI -> {
                    contentToSummarize = url // Gemini uses URL for YouTube videos
                    systemPrompt = Prompts.geminiPrompt(
                        Prompts.ContentType.VIDEO_TRANSCRIPT,
                        details.title,
                        length,
                        languageName
                    )
                }

                AIProvider.OPENAI, AIProvider.GROQ -> {
                    val transcriptData = YouTube.getTranscript(videoId, playerResponse)
                        ?: return SummaryResult(
                            details.title,
                            details.author,
                            "Error: Could not retrieve video transcript.",
                            true
                        )

                    contentToSummarize = transcriptData.first // The transcript text
                    val langCode = transcriptData.second
                    languageName = if (useOriginalLanguage.value)
                        Locale.forLanguageTag(langCode.split("-").first()).displayLanguage
                    else "English"

                    systemPrompt = when (model.value) {
                        AIProvider.OPENAI -> Prompts.openAIPrompt(
                            Prompts.ContentType.VIDEO_TRANSCRIPT,
                            details.title,
                            length,
                            languageName
                        )

                        AIProvider.GROQ -> Prompts.groqPrompt(
                            Prompts.ContentType.VIDEO_TRANSCRIPT,
                            details.title,
                            length,
                            languageName
                        )

                        else -> "" // Should not happen
                    }
                }
            }

            if (systemPrompt.isBlank()) {
                return SummaryResult(
                    details.title,
                    details.author,
                    "Error: Unsupported model for YouTube summary.",
                    true
                )
            }

            return executeSummary(
                contentToSummarize,
                systemPrompt,
                details.title,
                details.author,
                true
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return SummaryResult(url, "YouTube", "Error: ${e.message}", true)
        }
    }

    private suspend fun summarizeArticle(url: String, length: Int): SummaryResult =
        withContext(Dispatchers.IO) {
            try {
                val article = extractTextFromArticleUrl(url)
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
                        Prompts.ContentType.ARTICLE,
                        title,
                        length,
                        language
                    )

                    AIProvider.GEMINI -> Prompts.geminiPrompt(
                        Prompts.ContentType.ARTICLE,
                        title,
                        length,
                        language
                    )

                    AIProvider.GROQ -> Prompts.groqPrompt(
                        Prompts.ContentType.ARTICLE,
                        title,
                        length,
                        language
                    )
                }

                return@withContext executeSummary(transcript, systemPrompt, title, author, false)

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext SummaryResult(url, "Article", "Error: ${e.message}", true)
            }
        }

    private suspend fun summarizeDocument(
        documentName: String,
        text: String,
        length: Int
    ): SummaryResult {
        if (text.length < 100) {
            return SummaryResult(null, null, "Exception: too short", isError = true)
        }

        val language: String = if (useOriginalLanguage.value) {
            "the same language as the text"
        } else {
            getApplication<Application>().resources.configuration.locales[0].getDisplayLanguage(
                Locale.ENGLISH
            )
        }

        val contentType = Prompts.ContentType.DOCUMENT
        val systemPrompt = when (model.value) {
            AIProvider.OPENAI -> Prompts.openAIPrompt(contentType, null, length, language)
            AIProvider.GEMINI -> Prompts.geminiPrompt(contentType, null, length, language)
            AIProvider.GROQ -> Prompts.groqPrompt(contentType, null, length, language)
        }

        return executeSummary(
            text,
            systemPrompt,
            title = documentName,
            author = "Document",
            isYoutube = false
        )
    }

    private suspend fun summarizeText(
        text: String,
        length: Int
    ): SummaryResult {
        val language: String = if (useOriginalLanguage.value) {
            "the same language as the text"
        } else {
            getApplication<Application>().resources.configuration.locales[0].getDisplayLanguage(
                Locale.ENGLISH
            )
        }

        val contentType = Prompts.ContentType.TEXT
        val systemPrompt = when (model.value) {
            AIProvider.OPENAI -> Prompts.openAIPrompt(contentType, null, length, language)
            AIProvider.GEMINI -> Prompts.geminiPrompt(contentType, null, length, language)
            AIProvider.GROQ -> Prompts.groqPrompt(contentType, null, length, language)
        }

        return executeSummary(
            text,
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

            // The handlers already return a string starting with "Error: " on failure.
            // We can add more specific error mapping here if needed.
            if (summary.contains(
                    "API key not valid",
                    ignoreCase = true
                ) || summary.contains("API key is invalid", ignoreCase = true)
            ) {
                return@withContext "Error: Exception: incorrect key"
            }
            if (summary.contains("rate limit", ignoreCase = true)) {
                return@withContext "Error: Exception: rate limit"
            }
            return@withContext summary
        }

    private suspend fun executeSummary(
        textToSummarize: String,
        systemPrompt: String,
        title: String?,
        author: String?,
        isYoutube: Boolean
    ): SummaryResult {
        val summary = llmSummarize(textToSummarize, systemPrompt)
        val isError = summary.startsWith("Error:")
        val resultSummary = if (isError) summary else summary.trim()

        val result = SummaryResult(title, author, resultSummary, isError)
        if (!isError) {
            addTextSummary(result.title, result.author, result.summary, isYoutube)
        }
        return result
    }

    // --- Preference Handling Helpers ---
    private fun <T, R> Flow<T>.collectInto(
        stateFlow: MutableStateFlow<R>,
        transform: (T) -> R
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