package me.nanova.summaryexpressive.model

import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable

@Serializable
data class ExtractedContent(
    val title: String = "Unknown",
    val author: String = "Unknown",
    val content: String,
    val error: Boolean = false
) : ToolResult {
    override fun toStringDefault(): String = "title: $title, author: $author, content: $content"
}