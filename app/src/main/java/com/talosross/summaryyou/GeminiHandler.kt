package com.talosross.summaryyou

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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
                        modelName = "gemini-2.5-flash-lite",
                        apiKey = apiKey,
                        generationConfig = config
                    )
                    val response = model.generateContent("$instructions\n$text")
                    response.text ?: throw Exception("Empty response from Gemini")
                } catch (e: Exception) {
                    throw Exception("Error: ${e.localizedMessage}")
                }
            }
        }
    }
}