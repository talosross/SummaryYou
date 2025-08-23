package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.llm.AIProvider
import me.nanova.summaryexpressive.llm.SummaryLength
import javax.inject.Inject


@HiltViewModel
class UIViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val settingsUiState: StateFlow<SettingsUiState> =
        userPreferencesRepository.preferencesFlow.map { prefs ->
            SettingsUiState(
                useOriginalLanguage = prefs.useOriginalLanguage,
                dynamicColor = prefs.dynamicColor,
                theme = prefs.theme,
                apiKey = prefs.apiKey.takeIf { it.isNotBlank() },
                baseUrl = prefs.baseUrl.takeIf { it.isNotBlank() },
                model = AIProvider.valueOf(prefs.model),
                showOnboarding = prefs.showOnboarding,
                showLength = prefs.showLength,
                summaryLength = SummaryLength.valueOf(prefs.summaryLength)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsUiState()
        )

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
    fun setBaseUrlValue(newValue: String) =
        savePreference(userPreferencesRepository::setBaseUrl, newValue)

    // AI-Model
    fun setModelValue(newValue: String) =
        savePreference(userPreferencesRepository::setModel, newValue)

    // OnboardingScreen
    fun setShowOnboardingScreenValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowOnboarding, newValue)

    // Show length
    fun setShowLengthValue(newValue: Boolean) =
        savePreference(userPreferencesRepository::setShowLength, newValue)

    // Summary Length
    fun setSummaryLength(newValue: SummaryLength) =
        savePreference(userPreferencesRepository::setSummaryLength, newValue.name)


    // --- App Start Action ---
    private val _appStartAction = MutableStateFlow(AppStartAction())
    val appStartAction: StateFlow<AppStartAction> = _appStartAction.asStateFlow()

    fun setAppStartAction(action: AppStartAction) {
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