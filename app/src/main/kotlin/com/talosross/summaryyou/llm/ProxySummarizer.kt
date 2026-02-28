package com.talosross.summaryyou.llm

import android.util.Log
import com.talosross.summaryyou.di.FlavorConfig
import com.talosross.summaryyou.model.SummaryException
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Calls the Cloudflare Worker proxy to summarize content using server-side Gemini.
 * Used in the gms flavor when the user has no API key set.
 */
class ProxySummarizer(
    private val httpClient: HttpClient,
    private val flavorConfig: FlavorConfig,
) {
    @Serializable
    private data class ProxyRequest(
        val content: String,
        val length: String,
        val useContentLanguage: Boolean,
        val appLanguage: String,
        val model: String? = null,
    )

    @Serializable
    private data class ProxyResponse(
        val summary: String? = null,
        val error: String? = null,
        val message: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun summarize(
        content: String,
        length: SummaryLength,
        useContentLanguage: Boolean,
        appLanguage: String,
        model: String? = null,
    ): String {
        val baseUrl = flavorConfig.proxyBaseUrl
            ?: throw SummaryException.NoKeyException()

        val integrityToken = flavorConfig.getIntegrityToken()
        Log.d("ProxySummarizer", "Proxy URL: $baseUrl, integrity token present: ${integrityToken != null}")

        val requestBody = ProxyRequest(
            content = content,
            length = length.name,
            useContentLanguage = useContentLanguage,
            appLanguage = appLanguage,
            model = model,
        )

        val response = httpClient.post("$baseUrl/summarize") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ProxyRequest.serializer(), requestBody))
            if (integrityToken != null) {
                header("X-Integrity-Token", integrityToken)
            }
        }

        val responseText = response.bodyAsText()
        Log.d("ProxySummarizer", "Proxy response: status=${response.status.value}")

        if (response.status.value !in 200..299) {
            val proxyResponse = try {
                json.decodeFromString(ProxyResponse.serializer(), responseText)
            } catch (_: Exception) {
                null
            }

            val errorMessage = proxyResponse?.message
                ?: "Proxy error ${response.status.value}: $responseText"
            Log.e("ProxySummarizer", errorMessage)

            throw when (response.status.value) {
                429 -> SummaryException.RateLimitException()
                else -> SummaryException.UnknownException(errorMessage)
            }
        }

        val proxyResponse = json.decodeFromString(ProxyResponse.serializer(), responseText)
        return proxyResponse.summary
            ?: throw SummaryException.UnknownException(
                proxyResponse.message ?: "Empty response from proxy"
            )
    }
}

