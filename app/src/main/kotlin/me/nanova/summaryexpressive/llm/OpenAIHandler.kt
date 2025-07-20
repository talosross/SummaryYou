package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking

class OpenAIHandler {
    companion object {
        @JvmStatic
        fun generateContentSync(
            apiKey: String,
            instructions: String,
            text: String,
            baseUrl: String?
        ): String {
            return runBlocking {
                try {
                    val agent = if (baseUrl.isNullOrBlank()) {
                        AIAgent(
                            executor = simpleOpenAIExecutor(apiKey),
                            systemPrompt = instructions,
                            llmModel = OpenAIModels.Chat.GPT4o,
                        )
                    } else {
                        val client = OpenAILLMClient(
                            apiKey,
                            settings = OpenAIClientSettings(baseUrl = baseUrl)
                        )
                        val executor = SingleLLMPromptExecutor(client)
                        AIAgent(
                            executor = executor,
                            systemPrompt = instructions,
                            llmModel = OpenAIModels.Chat.GPT4o,
                        )
                    }
                    val result = agent.run(text)
                    result.toString()
                } catch (e: Exception) {
                    "Error: ${e.localizedMessage}"
                }
            }
        }
    }
}