package me.nanova.summaryexpressive.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.list
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import me.nanova.summaryexpressive.R

enum class AIProvider(
    val displayName: String,
    val isBaseUrlCustomisable: Boolean,
    val icon: Int,
    val models: List<LLModel> = emptyList(),
) {
    OPENAI(
        "OpenAI",
        true,
        R.drawable.chatgpt,
        OpenAIModels.list().filter { it.capabilities.contains(LLMCapability.Completion) }
    ),
    GEMINI(
        "Gemini",
        true,
        R.drawable.gemini,
        GoogleModels.list().filter { it.capabilities.contains(LLMCapability.Completion) }
    ),
    CLAUDE(
        "Anthropic",
        true,
        R.drawable.claude,
        AnthropicModels.list().filter { it.capabilities.contains(LLMCapability.Completion) }),

}