package me.nanova.summaryexpressive

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking

class GeminiHandler {
    companion object {
        @JvmStatic
        fun generateContentSync(
            apiKey: String,
            instructions: String,
            text: String
        ): String {
            return runBlocking {
                try {
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
}