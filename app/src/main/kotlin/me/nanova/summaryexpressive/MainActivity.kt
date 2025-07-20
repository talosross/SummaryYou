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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.ui.page.HistoryScreen
import me.nanova.summaryexpressive.ui.page.HomeScreen
import me.nanova.summaryexpressive.ui.page.OnboardingScreen
import me.nanova.summaryexpressive.ui.page.SettingsScreen
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
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

            SummaryExpressiveTheme(design = design, OledModeEnabled = oledMode) {
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
}


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