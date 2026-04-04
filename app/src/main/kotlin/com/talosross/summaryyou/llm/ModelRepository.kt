package com.talosross.summaryyou.llm

import ai.koog.prompt.llm.LLModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MILLIS = 15 * 60 * 1000L

@Singleton
class ModelRepository @Inject constructor(
    private val httpClient: HttpClient,
) {
    private val cacheMutex = Mutex()
    private val modelCache = mutableMapOf<ModelCacheKey, CachedModels>()

    suspend fun loadModels(
        provider: AIProvider,
        baseUrl: String?,
        apiKey: String?,
    ): List<LLModel> {
        if (provider == AIProvider.INTEGRATED) return provider.models

        val cacheKey = ModelCacheKey(provider = provider, baseUrl = baseUrl.orEmpty())
        val cachedModels = cacheMutex.withLock { modelCache[cacheKey] }
        if (cachedModels?.isFresh() == true) {
            return cachedModels.models
        }

        val remoteModels = runCatching {
            fetchModels(provider = provider, baseUrl = baseUrl, apiKey = apiKey)
        }.getOrElse {
            null
        }

        val resolvedModels = when {
            remoteModels.isNullOrEmpty() && cachedModels != null -> cachedModels.models
            remoteModels.isNullOrEmpty() -> provider.models
            else -> remoteModels.distinctBy { it.id }
        }

        if (!remoteModels.isNullOrEmpty()) {
            cacheMutex.withLock {
                modelCache[cacheKey] = CachedModels(resolvedModels, System.currentTimeMillis())
            }
        }

        return resolvedModels
    }

    private suspend fun fetchModels(
        provider: AIProvider,
        baseUrl: String?,
        apiKey: String?,
    ): List<LLModel> {
        return when (provider) {
            AIProvider.INTEGRATED -> provider.models
            AIProvider.OPENAI -> fetchOpenAICompatibleModels(
                provider = provider,
                url = buildUrl(baseUrl, "https://api.openai.com", "/v1/models"),
                apiKey = apiKey,
            )
            AIProvider.GEMINI -> fetchGeminiModels(
                provider = provider,
                url = buildUrl(baseUrl, "https://generativelanguage.googleapis.com", "/v1beta/models"),
                apiKey = apiKey,
            )
            AIProvider.CLAUDE -> fetchAnthropicModels(
                provider = provider,
                url = buildUrl(baseUrl, "https://api.anthropic.com", "/v1/models"),
                apiKey = apiKey,
            )
            AIProvider.DEEPSEEK -> fetchOpenAICompatibleModels(
                provider = provider,
                url = buildUrl(baseUrl, "https://api.deepseek.com", "/models"),
                apiKey = apiKey,
            )
            AIProvider.MISTRAL -> fetchOpenAICompatibleModels(
                provider = provider,
                url = buildUrl(baseUrl, "https://api.mistral.ai", "/v1/models"),
                apiKey = apiKey,
            )
            AIProvider.QWEN -> fetchOpenAICompatibleModels(
                provider = provider,
                url = buildUrl(baseUrl, "https://dashscope-intl.aliyuncs.com", "/compatible-mode/v1/models"),
                apiKey = apiKey,
            )
            AIProvider.OLLAMA -> fetchOllamaModels(
                provider = provider,
                url = buildUrl(baseUrl, "http://localhost:11434", "/api/tags"),
            )
            AIProvider.OPEN_ROUTER -> fetchOpenAICompatibleModels(
                provider = provider,
                url = buildUrl(baseUrl, "https://openrouter.ai", "/api/v1/models"),
                apiKey = apiKey,
            )
        }
    }

    private suspend fun fetchOpenAICompatibleModels(
        provider: AIProvider,
        url: String,
        apiKey: String?,
    ): List<LLModel> {
        val response = httpClient.get(url) {
            if (!apiKey.isNullOrBlank()) {
                header("Authorization", "Bearer $apiKey")
            }
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Model request failed for ${provider.name}: ${response.status}")
        }

        val body = response.body<OpenAICompatibleModelsResponse>()
        return body.data
            .mapNotNull { model -> model.id?.trim()?.takeIf { it.isNotBlank() } }
            .distinct()
            .sortedBy { it }
            .map { modelId -> provider.toLLModel(modelId) }
    }

    private suspend fun fetchAnthropicModels(
        provider: AIProvider,
        url: String,
        apiKey: String?,
    ): List<LLModel> {
        val response = httpClient.get(url) {
            if (!apiKey.isNullOrBlank()) {
                header("x-api-key", apiKey)
            }
            header("anthropic-version", "2023-06-01")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Model request failed for ${provider.name}: ${response.status}")
        }

        val body = response.body<AnthropicModelsResponse>()
        return body.data
            .mapNotNull { model -> model.id?.trim()?.takeIf { it.isNotBlank() } }
            .distinct()
            .sortedBy { it }
            .map { modelId -> provider.toLLModel(modelId) }
    }

    private suspend fun fetchGeminiModels(
        provider: AIProvider,
        url: String,
        apiKey: String?,
    ): List<LLModel> {
        val requestUrl = if (apiKey.isNullOrBlank()) {
            url
        } else {
            "$url?key=$apiKey"
        }

        val response = httpClient.get(requestUrl)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Model request failed for ${provider.name}: ${response.status}")
        }

        val body = response.body<GeminiModelsResponse>()
        return body.models
            .asSequence()
            .filter { model -> model.supportedGenerationMethods.any { method -> method.contains("generate", ignoreCase = true) } }
            .mapNotNull { model -> model.name?.trim()?.takeIf { it.isNotBlank() } }
            .map { modelName -> modelName.substringAfterLast('/') }
            .distinct()
            .sortedBy { it }
            .map { modelId -> provider.toLLModel(modelId) }
            .toList()
    }

    private suspend fun fetchOllamaModels(
        provider: AIProvider,
        url: String,
    ): List<LLModel> {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Model request failed for ${provider.name}: ${response.status}")
        }

        val body = response.body<OllamaTagsResponse>()
        return body.models
            .mapNotNull { model -> model.name?.trim()?.takeIf { it.isNotBlank() } }
            .distinct()
            .sortedBy { it }
            .map { modelId -> provider.toLLModel(modelId) }
    }

    private fun buildUrl(baseUrl: String?, defaultBaseUrl: String, path: String): String {
        val normalizedPath = path.trimStart('/')
        val defaultBase = defaultBaseUrl.trimEnd('/')
        val selectedBase = baseUrl?.trim()?.takeIf { it.isNotBlank() }?.trimEnd('/') ?: return "$defaultBase/$normalizedPath"

        val basePath = runCatching {
            URI(selectedBase).path.orEmpty().trim('/')
        }.getOrDefault("")

        if (basePath.isNotEmpty()) {
            if (basePath == normalizedPath || basePath.endsWith(normalizedPath)) {
                return selectedBase
            }
            if (normalizedPath.startsWith("$basePath/")) {
                return "$selectedBase/${normalizedPath.removePrefix("$basePath/")}"
            }
        }

        return "$selectedBase/$normalizedPath"
    }

    private fun AIProvider.toLLModel(modelId: String): LLModel {
        return models.firstOrNull { it.id == modelId } ?: CustomLLModel(this, modelId).toLLModel()
    }

    private data class ModelCacheKey(
        val provider: AIProvider,
        val baseUrl: String,
    )

    private data class CachedModels(
        val models: List<LLModel>,
        val fetchedAtMillis: Long,
    ) {
        fun isFresh(): Boolean = System.currentTimeMillis() - fetchedAtMillis < CACHE_TTL_MILLIS
    }

    @Serializable
    private data class OpenAICompatibleModelsResponse(
        val data: List<OpenAICompatibleModelDto> = emptyList(),
    )

    @Serializable
    private data class OpenAICompatibleModelDto(
        val id: String? = null,
    )

    @Serializable
    private data class AnthropicModelsResponse(
        val data: List<AnthropicModelDto> = emptyList(),
    )

    @Serializable
    private data class AnthropicModelDto(
        val id: String? = null,
    )

    @Serializable
    private data class GeminiModelsResponse(
        val models: List<GeminiModelDto> = emptyList(),
    )

    @Serializable
    private data class GeminiModelDto(
        val name: String? = null,
        val supportedGenerationMethods: List<String> = emptyList(),
    )

    @Serializable
    private data class OllamaTagsResponse(
        val models: List<OllamaModelDto> = emptyList(),
    )

    @Serializable
    private data class OllamaModelDto(
        val name: String? = null,
    )
}
