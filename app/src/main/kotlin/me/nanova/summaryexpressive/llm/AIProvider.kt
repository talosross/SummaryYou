package me.nanova.summaryexpressive.llm

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.dashscope.DashscopeModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.list
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels
import me.nanova.summaryexpressive.R

enum class AIProvider(
    val id: LLMProvider,
    val isMandatoryBaseUrl: Boolean,
    val isRequiredApiKey: Boolean,
    val icon: Int,
    val isMonochromeIcon: Boolean = false,
    val models: List<LLModel> = emptyList(),
) {
    OPENAI(
        LLMProvider.OpenAI,
        false,
        true,
        R.drawable.chatgpt,
        true,
        OpenAIModels.list()
            .filter {
                it.supports(LLMCapability.Completion)
                        && !it.supports(LLMCapability.Audio)
            }
            .distinct()
            .sortedBy { it.id }
    ),
    GEMINI(
        LLMProvider.Google,
        false,
        true,
        R.drawable.gemini,
        false,
        GoogleModels.list().filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    CLAUDE(
        LLMProvider.Anthropic,
        false,
        true,
        R.drawable.claude,
        false,
        AnthropicModels.list().filter { it.supports(LLMCapability.Completion) }
            .sortedWith(compareBy<LLModel> { model ->
                val versionRegex = Regex("(\\d[\\d.-]*\\d|\\d)")
                val match = versionRegex.find(model.id)
                match?.value?.replace('-', '.')?.toFloatOrNull() ?: Float.MAX_VALUE
            }.thenBy { it.id })
    ),
    DEEPSEEK(
        LLMProvider.DeepSeek,
        false,
        true,
        R.drawable.deepseek,
        false,
        DeepSeekModels.list().filter { it.supports(LLMCapability.Completion) }
            .sortedBy { it.id }
    ),
    QWEN(
        LLMProvider.Alibaba,
        false,
        true,
        R.drawable.qwen,
        false,
        DashscopeModels.list().filter { it.supports(LLMCapability.Completion) }
    ),
    OLLAMA(
        LLMProvider.Ollama,
        true,
        false,
        R.drawable.ollama,
        true,
        // just name a few most popular models
        listOf(
            OllamaModels.Meta.LLAMA_3_2,
            OllamaModels.Meta.LLAMA_4,
            OllamaModels.Alibaba.QWEN_2_5_05B,
            OllamaModels.Alibaba.QWEN_3_06B,
            LLModel(
                provider = LLMProvider.Google,
                id = "gemma3n",
                capabilities = listOf(LLMCapability.Completion),
                contextLength = 32_768,
            )
        )
    )

}