package me.nanova.summaryexpressive.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistorySummary(
    val id: String,
    override val title: String,
    override val author: String,
    override val summary: String,
    val isYoutubeLink: Boolean,
) : SummaryData