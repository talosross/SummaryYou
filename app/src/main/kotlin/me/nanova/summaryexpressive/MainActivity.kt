package me.nanova.summaryexpressive

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.GeminiHandler
import me.nanova.summaryexpressive.llm.OpenAIHandler
import me.nanova.summaryexpressive.llm.Prompts
import me.nanova.summaryexpressive.llm.YouTube
import me.nanova.summaryexpressive.ui.page.HistoryScreen
import me.nanova.summaryexpressive.ui.page.HomeScreen
import me.nanova.summaryexpressive.ui.page.OnboardingScreen
import me.nanova.summaryexpressive.ui.page.SettingsScreen
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import java.net.URL
import java.util.Locale
import java.util.UUID


class MainActivity : ComponentActivity() {
    private var sharedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lay app behind the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check if a link has been shared
        val intent: Intent? = intent
        if (Intent.ACTION_SEND == intent?.action && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        setContent {
            val viewModel: TextSummaryViewModel = viewModel()
            val design by viewModel.designNumber.collectAsState()
            val oledMode by viewModel.ultraDark.collectAsState()
            val showOnboarding by viewModel.showOnboardingScreen.collectAsState()

            SummaryExpressiveTheme(design = design, oLedModeEnabled = oledMode) {
                val navController = rememberNavController()
                var shouldShowOnboarding by rememberSaveable { mutableStateOf(showOnboarding) }
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (shouldShowOnboarding) {
                        OnboardingScreen(onContinueClicked = {
                            shouldShowOnboarding = false
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

class TextSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext

    val textSummaries = mutableStateListOf<TextSummary>()

    // Original Language in summary
    private val _useOriginalLanguage = MutableStateFlow(false)
    val useOriginalLanguage: StateFlow<Boolean> = _useOriginalLanguage.asStateFlow()
    fun setUseOriginalLanguageValue(newValue: Boolean) = viewModelScope.launch {
        UserPreferencesRepository.setUseOriginalLanguage(
            context,
            newValue
        )
    }

    // Multiline URL-Field
    private val _multiLine = MutableStateFlow(true)
    val multiLine: StateFlow<Boolean> = _multiLine.asStateFlow()
    fun setMultiLineValue(newValue: Boolean) =
        viewModelScope.launch { UserPreferencesRepository.setMultiLine(context, newValue) }

    // UltraDark - Mode
    private val _ultraDark = MutableStateFlow(false)
    val ultraDark: StateFlow<Boolean> = _ultraDark.asStateFlow()
    fun setUltraDarkValue(newValue: Boolean) =
        viewModelScope.launch { UserPreferencesRepository.setUltraDark(context, newValue) }

    // DesignNumber for Dark, Light or System
    private val _designNumber = MutableStateFlow(0)
    val designNumber: StateFlow<Int> = _designNumber.asStateFlow()
    fun setDesignNumber(newValue: Int) =
        viewModelScope.launch { UserPreferencesRepository.setDesignNumber(context, newValue) }

    // API Key
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()
    fun setApiKeyValue(newValue: String) =
        viewModelScope.launch { UserPreferencesRepository.setApiKey(context, newValue) }

    // API base url
    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()
    fun setBaseUrlValue(newValue: String) =
        viewModelScope.launch { UserPreferencesRepository.setBaseUrl(context, newValue) }

    // AI-Model
    private val _model = MutableStateFlow(AIProvider.OPENAI)
    val model: StateFlow<AIProvider> = _model.asStateFlow()
    fun setModelValue(newValue: String) =
        viewModelScope.launch { UserPreferencesRepository.setModel(context, newValue) }

    // OnboardingScreen
    private val _showOnboardingScreen = MutableStateFlow(true)
    val showOnboardingScreen: StateFlow<Boolean> = _showOnboardingScreen.asStateFlow()
    fun setShowOnboardingScreenValue(newValue: Boolean) =
        viewModelScope.launch { UserPreferencesRepository.setShowOnboarding(context, newValue) }

    // Show length
    private val _showLength = MutableStateFlow(true)
    val showLength: StateFlow<Boolean> = _showLength.asStateFlow()
    fun setShowLengthValue(newValue: Boolean) =
        viewModelScope.launch { UserPreferencesRepository.setShowLength(context, newValue) }

    // Show length number
    private val _showLengthNumber = MutableStateFlow(0)
    val showLengthNumber: StateFlow<Int> = _showLengthNumber.asStateFlow()
    fun setShowLengthNumberValue(newValue: Int) =
        viewModelScope.launch { UserPreferencesRepository.setShowLengthNumber(context, newValue) }

    init {
        // Load summaries from DataStore
        viewModelScope.launch {
            UserPreferencesRepository.getTextSummaries(context).collect { summariesJson ->
                val type = object : TypeToken<List<TextSummary>>() {}.type
                val summaries = Gson().fromJson<List<TextSummary>>(summariesJson, type)
                textSummaries.clear()
                textSummaries.addAll(summaries)
            }
        }

        // Collect preferences from DataStore
        viewModelScope.launch {
            UserPreferencesRepository.getUseOriginalLanguage(context)
                .collect { _useOriginalLanguage.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getMultiLine(context).collect { _multiLine.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getUltraDark(context).collect { _ultraDark.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getDesignNumber(context).collect { _designNumber.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getApiKey(context).collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getBaseUrl(context).collect { _baseUrl.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getModel(context)
                .collect { _model.value = AIProvider.valueOf(it) }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getShowOnboarding(context)
                .collect { _showOnboardingScreen.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getShowLength(context).collect { _showLength.value = it }
        }
        viewModelScope.launch {
            UserPreferencesRepository.getShowLengthNumber(context)
                .collect { _showLengthNumber.value = it }
        }
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
            UserPreferencesRepository.setTextSummaries(context, textSummariesJson)
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

    fun summarize(urlOrText: String, length: Int, isDocument: Boolean, documentText: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentSummaryResult.value = null // Clear previous result

            val result = summarizeInternal(urlOrText, length, isDocument, documentText)

            _currentSummaryResult.value = result
            _isLoading.value = false
        }
    }

    private suspend fun summarizeInternal(
        urlOrText: String,
        length: Int,
        isDocument: Boolean,
        documentText: String?
    ): SummaryResult {
        val isYouTube = YouTube.isYouTubeLink(urlOrText)

        if (urlOrText.isBlank() || (isDocument && documentText.isNullOrBlank())) {
            return SummaryResult(null, null, "Exception: no content", isError = true)
        }

        // API key is required for all LLM calls, regardless of input type
        if (apiKey.value.isEmpty()) {
            return SummaryResult(null, null, "Exception: no key", isError = true)
        }

        return if (isYouTube) {
            summarizeYouTubeVideo(urlOrText, length)
        } else {
            summarizeTextOrDocument(urlOrText, length, isDocument, documentText)
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

            if (model.value == AIProvider.GEMINI) {
                contentToSummarize = url // Gemini uses URL
                val currentLocale: Locale = context.resources.configuration.locales[0]
                val languageName =
                    if (useOriginalLanguage.value) currentLocale.displayLanguage else "English"
                val promptFn = when (length) {
                    0 -> Prompts::geminiPromptVideo0
                    1 -> Prompts::geminiPromptVideo1
                    else -> Prompts::geminiPromptVideo3
                }
                systemPrompt = promptFn(details.title, languageName)
            } else { // OpenAI, etc.
                val transcriptData = YouTube.getTranscript(videoId, playerResponse)
                    ?: return SummaryResult(
                        details.title,
                        details.author,
                        "Error: Could not retrieve video transcript.",
                        true
                    )

                contentToSummarize = transcriptData.first // The transcript text
                val langCode = transcriptData.second
                val languageName = if (useOriginalLanguage.value)
                    Locale.forLanguageTag(langCode.split("-").first()).displayLanguage
                else "English"

                val promptFn = when (model.value) {
                    AIProvider.OPENAI -> when (length) {
                        0 -> Prompts::openAIPromptVideo0
                        1 -> Prompts::openAIPromptVideo1
                        else -> Prompts::openAIPromptVideo3
                    }

                    else -> return SummaryResult(
                        details.title,
                        details.author,
                        "Error: Unsupported model for YouTube summary.",
                        true
                    )
                }
                systemPrompt = promptFn(details.title, languageName)
            }

            val summary = llmSummarize(contentToSummarize, systemPrompt)
            val isError = summary.startsWith("Error:")
            val resultSummary = if (isError) summary else summary.trim()

            val result = SummaryResult(details.title, details.author, resultSummary, isError)
            if (!isError) {
                addTextSummary(result.title, result.author, result.summary, true)
            }
            return result

        } catch (e: Exception) {
            e.printStackTrace()
            return SummaryResult(url, "YouTube", "Error: ${e.message}", true)
        }
    }

    private suspend fun summarizeTextOrDocument(
        urlOrText: String,
        length: Int,
        isDocument: Boolean,
        documentText: String?
    ): SummaryResult {
        val textToSummarize = if (isDocument) documentText!! else urlOrText
        if (!isDocument && (textToSummarize.startsWith("http") || textToSummarize.startsWith("https")) && runCatching {
                URL(
                    textToSummarize
                ).host
            }.getOrNull().isNullOrEmpty()) {
            return SummaryResult(null, null, "Exception: invalid link", isError = true)
        }
        if (textToSummarize.length < 100) {
            return SummaryResult(null, null, "Exception: too short", isError = true)
        }

        val currentLocale: Locale = context.resources.configuration.locales[0]
        val language: String = if (useOriginalLanguage.value) {
            "the same language as the text"
        } else {
            currentLocale.getDisplayLanguage(Locale.ENGLISH)
        }

        val promptFn = when (model.value) {
            AIProvider.OPENAI -> when (length) {
                0 -> if (isDocument) Prompts::openAIPromptDocument0 else Prompts::openAIPromptText0
                1 -> if (isDocument) Prompts::openAIPromptDocument1 else Prompts::openAIPromptText1
                else -> if (isDocument) Prompts::openAIPromptDocument3 else Prompts::openAIPromptText3
            }

            AIProvider.GEMINI -> when (length) {
                0 -> if (isDocument) Prompts::geminiPromptDocument0 else Prompts::geminiPromptText0
                1 -> if (isDocument) Prompts::geminiPromptDocument1 else Prompts::geminiPromptText1
                else -> if (isDocument) Prompts::geminiPromptDocument3 else Prompts::geminiPromptText3
            }

            else -> return SummaryResult(null, null, "Error: Unsupported model.", true)
        }
        val systemPrompt = promptFn(language)

        val summary = llmSummarize(textToSummarize, systemPrompt)
        val isError = summary.startsWith("Error:")
        val resultSummary = if (isError) summary else summary.trim()

        val result = SummaryResult(
            if (isDocument) urlOrText else null,
            if (isDocument) "Document" else null,
            resultSummary,
            isError
        )
        if (!isError) {
            addTextSummary(result.title, result.author, result.summary, false)
        }
        return result
    }

    private suspend fun llmSummarize(textToSummarize: String, systemPrompt: String): String =
        withContext(Dispatchers.IO) {
            val currentModel = model.value
            val currentApiKey = apiKey.value
            val currentBaseUrl = baseUrl.value

            val summary = when (currentModel) {
                AIProvider.OPENAI -> OpenAIHandler.generateContentSync(
                    currentApiKey,
                    systemPrompt,
                    textToSummarize,
                    currentBaseUrl
                )

                AIProvider.GEMINI -> GeminiHandler.generateContentSync(
                    currentApiKey,
                    systemPrompt,
                    textToSummarize
                )

                AIProvider.GROQ -> "Error: Groq is not implemented."
            }

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
}

data class SummaryResult(
    val title: String?,
    val author: String?,
    val summary: String?,
    val isError: Boolean = false
)

data class TextSummary(
    val id: String,
    val title: String,
    val author: String,
    val text: String,
    val youtubeLink: Boolean
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: TextSummaryViewModel,
    initialUrl: String? = null
) {
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel,
                initialUrl
            )
        }
        composable("settings") {
            SettingsScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
            )
        }
        composable("history") {
            HistoryScreen(
                modifier = Modifier,
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}