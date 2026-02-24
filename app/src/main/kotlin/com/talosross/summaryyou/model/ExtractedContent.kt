package com.talosross.summaryyou.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedContent(
    val title: String = "Unknown",
    val author: String = "Unknown",
    val content: String,
    val error: Boolean = false,
)