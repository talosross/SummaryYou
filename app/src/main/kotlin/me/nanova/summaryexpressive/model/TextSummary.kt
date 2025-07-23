package me.nanova.summaryexpressive.model

data class TextSummary(
    val id: String,
    val title: String,
    val author: String,
    val text: String,
    val youtubeLink: Boolean
)