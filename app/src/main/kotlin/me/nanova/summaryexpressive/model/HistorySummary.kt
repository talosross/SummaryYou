package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable
import me.nanova.summaryexpressive.llm.SummaryLength

@Serializable
data class HistorySummary(
    val id: String,
    override val title: String,
    override val author: String,
    override val summary: String,
    val sourceLink: String?,
    val isYoutubeLink: Boolean,
    val isBiliBiliLink: Boolean,
    val length: SummaryLength,
    val createdOn: Long = 0L,
) : SummaryData