package com.talosross.summaryexpressive

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class OpenAIHandler {
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
                        executor = simpleOpenAIExecutor(apiKey), // or Anthropic, Google, OpenRouter, etc.
                        systemPrompt = instructions,
                        llmModel = OpenAIModels.Chat.GPT4o
                    )

                    val result = agent.run(text)
                    result.toString() ?: throw Exception("Empty response from OpenAI")
                } catch (e: Exception) {
                    "Error: ${e.localizedMessage}"
                }
            }
        }
    }
}