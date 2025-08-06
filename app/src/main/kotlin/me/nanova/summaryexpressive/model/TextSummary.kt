package me.nanova.summaryexpressive.model

import kotlinx.serialization.Serializable

@Serializable
data class TextSummary(
    val id: String,
    val title: String,
    val author: String,
    val text: String,
    val youtubeLink: Boolean
)