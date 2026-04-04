package com.talosross.summaryyou.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.talosross.summaryyou.UserPreferencesRepository
import com.talosross.summaryyou.di.FlavorConfig
import com.talosross.summaryyou.llm.AIProvider
import com.talosross.summaryyou.llm.ModelRepository
import com.talosross.summaryyou.llm.SummaryLength
import com.talosross.summaryyou.ui.Nav
import javax.inject.Inject


@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val flavorConfig: FlavorConfig,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<Nav?>(null)
    val startDestination: StateFlow<Nav?> = _startDestination

    private val _modelSelectionUiState = MutableStateFlow(ModelSelectionUiState())
    val modelSelectionUiState: StateFlow<ModelSelectionUiState> = _modelSelectionUiState.asStateFlow()
    private var modelRequestId: Long = 0

    init {
        viewModelScope.launch {
            val isOnboardingCompleted = userPreferencesRepository.preferencesFlow.map { it.isOnboarded}.first()

            if (isOnboardingCompleted) {
                _startDestination.value = Nav.Home
            } else {
                _startDestination.value = Nav.Onboarding
            }
        }
    }

    val settingsUiState: StateFlow<SettingsUiState> =
        userPreferencesRepository.preferencesFlow.map { prefs ->
            SettingsUiState(
                useOriginalLanguage = prefs.useOriginalLanguage,
                dynamicColor = prefs.dynamicColor,
                theme = prefs.theme,
                apiKey = prefs.apiKey.takeIf { it.isNotBlank() },
                baseUrl = prefs.baseUrl.takeIf { it.isNotBlank() },
                aiProvider = AIProvider.valueOf(prefs.aiProvider),
                model = prefs.model.takeIf { it.isNotBlank() },
                showLength = prefs.showLength,
                summaryLength = SummaryLength.valueOf(prefs.summaryLength),
                autoExtractUrl = prefs.autoExtractUrl,
                sessData = prefs.sessData,
                sessDataExpires = prefs.sessDataExpires,
                hasProxy = flavorConfig.proxyBaseUrl != null,
                developerMode = prefs.developerMode,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsUiState(hasProxy = flavorConfig.proxyBaseUrl != null)
        )

    fun refreshModels(provider: AIProvider, baseUrl: String?, apiKey: String?) {
        val requestId = ++modelRequestId
        _modelSelectionUiState.value = ModelSelectionUiState(provider = provider, isLoading = true)

        viewModelScope.launch {
            val models = modelRepository.loadModels(provider, baseUrl, apiKey)
            if (requestId == modelRequestId) {
                _modelSelectionUiState.value = ModelSelectionUiState(
                    provider = provider,
                    models = models,
                    isLoading = false,
                )
            }
        }
    }

    // Original Language in summary
    fun setUseOriginalLanguageValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setUseOriginalLanguage, newValue)

    // Dynamic color
    fun setDynamicColorValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setDynamicColor, newValue)

    // Theme for Dark, Light or System
    fun setTheme(newValue: Int) =
        savePreference(userPreferencesRepository::setTheme, newValue)

    // API Key
    fun setApiKeyValue(newValue: String) =
        savePreference(userPreferencesRepository::setApiKey, newValue)

    // API base url
    fun setBaseUrlValue(newValue: String) {
        val baseUrlWithProtocol = if (newValue.isBlank() || newValue.startsWith("http")) newValue
        else "https://$newValue"
        savePreference(userPreferencesRepository::setBaseUrl, baseUrlWithProtocol)
    }

    // AI provider
    fun setAIProviderValue(newValue: String) =
        savePreference(userPreferencesRepository::setAIProvider, newValue)

    // Model
    fun setModel(newValue: String) =
        savePreference(userPreferencesRepository::setModel, newValue)

    // Show length
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowLength, newValue)

    // Summary Length
    fun setSummaryLength(newValue: SummaryLength) =
        savePreference(userPreferencesRepository::setSummaryLength, newValue.name)

    // Auto extract url
    fun setAutoExtractUrlValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setAutoExtractUrl, newValue)

    // OnboardingScreen
    fun setIsOnboarded(newValue: Boolean) =
        savePreference(userPreferencesRepository::setIsOnboarded, newValue)

    // BiliBili SESSDATA
    fun setSessData(data: String, expires: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setSessData(data, expires)
        }
    }

    fun clearSessData() {
        viewModelScope.launch {
            userPreferencesRepository.clearSessData()
        }
    }

    // Developer Mode
    fun setDeveloperMode(newValue: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDeveloperMode(newValue)
            if (newValue && flavorConfig.proxyBaseUrl != null) {
                // Default to INTEGRATED provider when enabling dev mode in Playstore
                userPreferencesRepository.setAIProvider(AIProvider.INTEGRATED.name)
                userPreferencesRepository.setModel("")
                userPreferencesRepository.setApiKey("")
                userPreferencesRepository.setBaseUrl("")
            }
        }
    }

    fun disableDeveloperMode() {
        viewModelScope.launch {
            userPreferencesRepository.disableDeveloperMode()
        }
    }

    // --- App Start Action ---
    private val _appStartAction = MutableStateFlow(AppStartAction())
    val appStartAction: StateFlow<AppStartAction> = _appStartAction.asStateFlow()

    fun onEvent(action: AppStartAction) {
        _appStartAction.value = action
    }

    fun onStartActionHandled() {
        _appStartAction.value = AppStartAction()
    }

    // --- Preference Handling Helpers ---
    private fun <T> savePreference(setter: suspend (T) -> Unit, value: T) {
        viewModelScope.launch {
            setter(value)
        }
    }
}