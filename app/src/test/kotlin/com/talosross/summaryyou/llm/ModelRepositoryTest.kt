package com.talosross.summaryyou.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRepositoryTest {

    @Test
    fun `loads openai compatible models from remote response`() = runBlocking {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/v1/models", request.url.encodedPath)
                    assertEquals("Bearer sk-test", request.headers[HttpHeaders.Authorization])
                    respond(
                        content = """{"object":"list","data":[{"id":"gpt-4o-mini","owned_by":"openai"},{"id":"gpt-4.1-mini","foo":"bar"}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val repository = ModelRepository(client)
        val models = repository.loadModels(AIProvider.OPENAI, baseUrl = null, apiKey = "sk-test")

        assertEquals(listOf("gpt-4.1-mini", "gpt-4o-mini"), models.map { it.id })
    }

    @Test
    fun `loads deepseek models from models endpoint`() = runBlocking {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/models", request.url.encodedPath)
                    respond(
                        content = """{"data":[{"id":"deepseek-chat"}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val repository = ModelRepository(client)
        val models = repository.loadModels(AIProvider.DEEPSEEK, baseUrl = null, apiKey = "sk-test")

        assertEquals(listOf("deepseek-chat"), models.map { it.id })
    }

    @Test
    fun `base url with version path does not duplicate endpoint segments`() = runBlocking {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/v1/models", request.url.encodedPath)
                    respond(
                        content = """{"data":[{"id":"gpt-4o-mini"}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val repository = ModelRepository(client)
        val models = repository.loadModels(AIProvider.OPENAI, baseUrl = "https://api.openai.com/v1", apiKey = "sk-test")

        assertEquals(listOf("gpt-4o-mini"), models.map { it.id })
    }

    @Test
    fun `loads ollama models from remote tags endpoint`() = runBlocking {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/api/tags", request.url.encodedPath)
                    respond(
                        content = """{"models":[{"name":"llama3.2"},{"name":"qwen2.5"}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val repository = ModelRepository(client)
        val models = repository.loadModels(AIProvider.OLLAMA, baseUrl = "http://localhost:11434", apiKey = null)

        assertEquals(listOf("llama3.2", "qwen2.5"), models.map { it.id })
    }

    @Test
    fun `falls back to default models when remote request fails`() = runBlocking {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                addHandler { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    respond(
                        content = "boom",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
        }

        val repository = ModelRepository(client)
        val models = repository.loadModels(AIProvider.OPENAI, baseUrl = null, apiKey = "sk-test")

        assertEquals(AIProvider.OPENAI.models.map { it.id }, models.map { it.id })
    }

    @Test
    fun `uses cache on subsequent load after a successful fetch`() = runBlocking {
        var requestCount = 0
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                addHandler { _ ->
                    requestCount++
                    respond(
                        content = """{"data":[{"id":"cached-model"}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val repository = ModelRepository(client)
        val firstLoad = repository.loadModels(AIProvider.OPENAI, baseUrl = null, apiKey = "sk-test")
        val secondLoad = repository.loadModels(AIProvider.OPENAI, baseUrl = null, apiKey = "sk-test")

        assertEquals(listOf("cached-model"), firstLoad.map { it.id })
        assertEquals(listOf("cached-model"), secondLoad.map { it.id })
        assertEquals(1, requestCount)
    }
}
