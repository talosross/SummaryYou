package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable
import me.nanova.summaryexpressive.llm.SummaryLength

@Serializable
data class HistorySummary(
    val id: String,
    override val title: String,
    override val author: String,
    override val summary: String,
    val isYoutubeLink: Boolean,
    val length: SummaryLength,
) : SummaryData