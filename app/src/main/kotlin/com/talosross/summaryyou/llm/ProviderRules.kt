package com.talosross.summaryyou.llm

internal fun shouldUseProxyForSummary(
    provider: AIProvider,
    apiKey: String?,
    hasProxy: Boolean,
): Boolean {
    if (!hasProxy) return false
    if (provider == AIProvider.OLLAMA) return false
    return provider == AIProvider.INTEGRATED || apiKey.isNullOrEmpty()
}

internal fun isProviderSettingsValid(
    provider: AIProvider,
    baseUrl: String,
    apiKey: String,
    hasProxy: Boolean,
): Boolean {
    if (provider == AIProvider.INTEGRATED) {
        return hasProxy
    }

    val baseUrlValid = !provider.isMandatoryBaseUrl || baseUrl.isNotBlank()
    val apiKeyValid = !provider.isRequiredApiKey || apiKey.isNotBlank() || hasProxy
    return baseUrlValid && apiKeyValid
}

