package com.talosross.summaryyou.llm

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel

data class CustomLLModel(
    val provider: AIProvider,
    val name: String,
) {

    fun toLLModel(): LLModel = LLModel(
        provider = provider.id,
        id = name,
        // TODO: just hardcoded for now
        contextLength = 8_192,
        capabilities = listOfNotNull(
            LLMCapability.Completion,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            if (provider == AIProvider.OPENAI) LLMCapability.OpenAIEndpoint.Responses else null,
            if (provider == AIProvider.OPENAI) LLMCapability.OpenAIEndpoint.Completions else null,
        ),
    )
}
