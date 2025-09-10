package me.nanova.summaryexpressive.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
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
        OpenAIModels.list()
            .filter {
                it.supports(LLMCapability.Completion)
                        && !it.supports(LLMCapability.Audio)
            }
            .sortedBy { it.id }
    ),
    GEMINI(
        "Gemini",
        true,
        R.drawable.gemini,
        GoogleModels.list().filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    CLAUDE(
        "Anthropic",
        true,
        R.drawable.claude,
        AnthropicModels.list().filter { it.supports(LLMCapability.Completion) }
            .sortedWith(compareBy<LLModel> { model ->
                val versionRegex = Regex("(\\d[\\d.-]*\\d|\\d)")
                val match = versionRegex.find(model.id)
                match?.value?.replace('-', '.')?.toFloatOrNull() ?: Float.MAX_VALUE
            }.thenBy { it.id })
    ),
    DEEPSEEK(
        "DeepSeek",
        true,
        R.drawable.deepseek,
        DeepSeekModels.list().filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    )
}