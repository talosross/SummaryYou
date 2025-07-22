package me.nanova.summaryexpressive.llm

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

class GeminiHandler {
    companion object {
        @JvmStatic
        suspend fun generateContent(
            apiKey: String,
            instructions: String,
            text: String
        ): String {
            return try {
                val agent = AIAgent(
                    executor = simpleGoogleAIExecutor(apiKey),
                    systemPrompt = instructions,
                    llmModel = GoogleModels.Gemini2_5Flash,
                    temperature = 0.9
                )

                agent.run(text)
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }
}