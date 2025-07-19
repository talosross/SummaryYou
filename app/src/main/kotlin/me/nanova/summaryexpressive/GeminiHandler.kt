package me.nanova.summaryexpressive

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
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
                    val config = generationConfig {
                        temperature = 0.9f
                    }
                    val model = GenerativeModel(
                        modelName = "gemini-2.0-flash",
                        apiKey = apiKey,
                        generationConfig = config
                    )
                    val response = model.generateContent("$instructions\n$text")
                    response.text ?: throw Exception("Empty response from Gemini")
                } catch (e: Exception) {
                    "Error: ${e.localizedMessage}"
                }
            }
        }
    }
}