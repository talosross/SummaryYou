package me.nanova.summaryexpressive.vm

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.nanova.summaryexpressive.UserPreferencesRepository
import me.nanova.summaryexpressive.model.HistorySummary
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val historySummaries = mutableStateListOf<HistorySummary>()

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

    private fun saveHistorySummaries() {
        viewModelScope.launch {
            val historySummariesJson = Json.encodeToString(historySummaries.toList())
            userPreferencesRepository.setTextSummaries(historySummariesJson)
        }
    }

    fun removeHistorySummary(id: String) {
        val summaryToRemove = historySummaries.firstOrNull { it.id == id }
        summaryToRemove?.let {
            historySummaries.remove(it)
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
}