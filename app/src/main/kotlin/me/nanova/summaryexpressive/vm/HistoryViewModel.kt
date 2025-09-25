package me.nanova.summaryexpressive.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.model.HistorySummary
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _historySummaries = MutableStateFlow<List<HistorySummary>>(emptyList())
    val historySummaries: StateFlow<List<HistorySummary>> = _historySummaries.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    val summariesToShow: StateFlow<List<HistorySummary>> = searchText
        .combine(_historySummaries) { text, summaries ->
            if (text.isBlank()) {
                summaries
            } else {
                summaries.filter {
                    it.title.contains(text, ignoreCase = true) || it.author.contains(
                        text,
                        ignoreCase = true
                    ) || it.summary.contains(text, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = _historySummaries.value
        )


    init {
        loadSummaries()
    }

    private fun loadSummaries() {
        viewModelScope.launch {
            userPreferencesRepository.getTextSummaries().collect { summariesJson ->
                if (summariesJson.isNotEmpty()) {
                    val summaries =
                        kotlin.runCatching { Json.decodeFromString<List<HistorySummary>>(summariesJson) }
                            .getOrNull()
                    _historySummaries.value = summaries?.sortedByDescending { it.createdOn } ?: emptyList()
                } else {
                    _historySummaries.value = emptyList()
                }
            }
        }
    }

    private fun saveHistorySummaries() {
        viewModelScope.launch {
            val historySummariesJson = Json.encodeToString(_historySummaries.value)
            userPreferencesRepository.setTextSummaries(historySummariesJson)
        }
    }

    fun onSearchTextChanged(text: String) {
        _searchText.value = text
    }

    fun addHistorySummary(summary: HistorySummary) {
        val currentList = _historySummaries.value.toMutableList()
        currentList.add(summary)
        _historySummaries.value = currentList.sortedByDescending { it.createdOn }
        saveHistorySummaries()
    }

    fun removeHistorySummary(id: String) {
        val currentList = _historySummaries.value.toMutableList()
        val summaryToRemove = currentList.firstOrNull { it.id == id }
        summaryToRemove?.let {
            currentList.remove(it)
            _historySummaries.value = currentList
            saveHistorySummaries()
        }
    }
}