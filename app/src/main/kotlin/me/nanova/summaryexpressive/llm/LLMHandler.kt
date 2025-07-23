package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor

class LLMHandler {
    companion object {
        @JvmStatic
        suspend fun generateContent(
            provider: AIProvider,
            apiKey: String,
            instructions: String,
            text: String,
            baseUrl: String? = null,
            modelName: String? = null
        ): String {
            return try {
                val agent = createAgent(provider, apiKey, instructions, baseUrl, modelName)
                agent.run(text)
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }

        private fun createAgent(
            provider: AIProvider,
            apiKey: String,
            instructions: String,
            baseUrl: String?,
            modelName: String?
        ): AIAgent<String, String> {
            return when (provider) {
                AIProvider.OPENAI -> createOpenAIAgent(apiKey, baseUrl, instructions)
                AIProvider.GEMINI -> createGeminiAgent(apiKey, instructions)
                AIProvider.GROQ -> TODO()
            }
        }

        private fun createOpenAIAgent(apiKey: String, baseUrl: String?, instructions: String): AIAgent<String, String> {
            val executor = if (baseUrl.isNullOrBlank()) {
                simpleOpenAIExecutor(apiKey)
            } else {
                val client = OpenAILLMClient(
                    apiKey,
                    settings = OpenAIClientSettings(baseUrl = baseUrl)
                )
                SingleLLMPromptExecutor(client)
            }
            return AIAgent(
                executor = executor,
                llmModel = OpenAIModels.Chat.GPT4o,
                systemPrompt = instructions
            )
        }

        private fun createGeminiAgent(apiKey: String, instructions: String): AIAgent<String, String> {
            return AIAgent(
                executor = simpleGoogleAIExecutor(apiKey),
                llmModel = GoogleModels.Gemini2_5Flash,
                systemPrompt = instructions,
                temperature = 0.9
            )
        }

//        private fun createGroqAgent(apiKey: String, modelName: String?): AIAgent<String, String> {
//             val client = OpenAILLMClient(
//                apiKey,
//                settings = OpenAIClientSettings(baseUrl = "https://api.groq.com/openai/v1")
//            )
//            val executor = SingleLLMPromptExecutor(client)
//            return AIAgent(
//                executor = executor,
//                llmModel = modelName ?: "llama3-8b-8192",
//                systemPrompt = instructions
//            )
//        }
    }
}