package com.talosross.summaryyou.llm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class ProviderRulesTest {
    @Test
    fun `ollama never uses proxy fallback`() {
        assertFalse(shouldUseProxyForSummary(AIProvider.OLLAMA, apiKey = null, hasProxy = true))
        assertFalse(shouldUseProxyForSummary(AIProvider.OLLAMA, apiKey = "", hasProxy = true))
    }
    @Test
    fun `openai uses proxy when key is missing and proxy exists`() {
        assertTrue(shouldUseProxyForSummary(AIProvider.OPENAI, apiKey = null, hasProxy = true))
        assertTrue(shouldUseProxyForSummary(AIProvider.OPENAI, apiKey = "", hasProxy = true))
    }
    @Test
    fun `openai does not use proxy when none is configured`() {
        assertFalse(shouldUseProxyForSummary(AIProvider.OPENAI, apiKey = null, hasProxy = false))
    }
    @Test
    fun `ollama settings require base url even if proxy exists`() {
        assertFalse(
            isProviderSettingsValid(
                provider = AIProvider.OLLAMA,
                baseUrl = "",
                apiKey = "",
                hasProxy = true,
            )
        )
        assertTrue(
            isProviderSettingsValid(
                provider = AIProvider.OLLAMA,
                baseUrl = "http://localhost:11434",
                apiKey = "",
                hasProxy = true,
            )
        )
    }
    @Test
    fun `cloud provider settings allow missing key when proxy exists`() {
        assertTrue(
            isProviderSettingsValid(
                provider = AIProvider.OPENAI,
                baseUrl = "",
                apiKey = "",
                hasProxy = true,
            )
        )
    }
}
