package com.talosross.summaryyou.di

/**
 * Flavor-specific configuration provided via Hilt.
 * - gms flavor: provides proxy URL + Play Integrity
 * - standalone flavor: returns null (BYOK only)
 */
interface FlavorConfig {
    /** Proxy base URL for server-side Gemini access, or null if not available. */
    val proxyBaseUrl: String?

    /** Get a Play Integrity token for the current request, or null if not supported. */
    suspend fun getIntegrityToken(): String?
}

