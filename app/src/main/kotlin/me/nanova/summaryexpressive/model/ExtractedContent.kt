package me.nanova.summaryexpressive.model

import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable

@Serializable
data class ExtractedContent(
    val title: String,
    val author: String,
    val content: String,
) : ToolResult {
    override fun toStringDefault(): String = "title: $title, author: $author, content: $content"
}