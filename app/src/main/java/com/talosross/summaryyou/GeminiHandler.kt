package com.talosross.summaryyou

import com.google.ai.client.generativeai.GenerativeModel
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
                    val model = GenerativeModel(
                        modelName = "gemini-2.0-flash",
                        apiKey = apiKey
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