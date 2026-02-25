package com.talosross.summaryyou.llm

import ai.koog.prompt.dsl.Prompt
import kotlinx.serialization.Serializable

enum class SummaryLength {
    SHORT, MEDIUM, LONG
}

@Serializable
data class PromptConfig(
    val template: String,
    val lengthInstructions: Map<String, String>,
    val languageInstruction: LanguageInstructionConfig,
)

@Serializable
data class LanguageInstructionConfig(
    val useContentLanguage: String,
    val useAppLanguage: String,
)

fun createSummarizationPrompt(
    config: PromptConfig,
    length: SummaryLength,
    useContentLanguage: Boolean,
    appLanguage: String,
): Prompt {
    val lengthInstruction = config.lengthInstructions[length.name]
        ?: config.lengthInstructions[SummaryLength.MEDIUM.name]
        ?: ""

    val languageInstruction = if (useContentLanguage) {
        config.languageInstruction.useContentLanguage
    } else {
        config.languageInstruction.useAppLanguage.replace("{{APP_LANGUAGE}}", appLanguage)
    }

    val promptText = config.template
        .replace("{{LENGTH_INSTRUCTION}}", lengthInstruction)
        .replace("{{LANGUAGE_INSTRUCTION}}", languageInstruction)

    return Prompt.build("summarizer-prompt") {
        system(promptText)
    }
}