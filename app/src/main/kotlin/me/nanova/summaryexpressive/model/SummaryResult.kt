package me.nanova.summaryexpressive.model

data class SummaryResult(
    val title: String?,
    val author: String?,
    val summary: String?,
    val isError: Boolean = false
)
