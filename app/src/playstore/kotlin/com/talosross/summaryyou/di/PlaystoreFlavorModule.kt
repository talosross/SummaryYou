package com.talosross.summaryyou.di

import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.talosross.summaryyou.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaystoreFlavorModule {

    @Provides
    @Singleton
    fun provideFlavorConfig(@ApplicationContext context: Context): FlavorConfig {
        return PlaystoreFlavorConfig(context)
    }
}

private class PlaystoreFlavorConfig(private val context: Context) : FlavorConfig {

    companion object {
        private const val TAG = "PlaystoreFlavorConfig"
        /** Cache tokens for 4 minutes (tokens are valid for ~5 min). */
        private const val TOKEN_CACHE_TTL_MS = 4 * 60 * 1000L
        /** Maximum backoff delay (2 minutes). */
        private const val MAX_BACKOFF_MS = 2 * 60 * 1000L
        /** Base backoff delay (5 seconds). */
        private const val BASE_BACKOFF_MS = 5_000L
    }

    override val proxyBaseUrl: String?
        get() = BuildConfig.PROXY_URL.takeIf { it.isNotBlank() }

    private val mutex = Mutex()

    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedTokenTimestamp: Long = 0L

    /** Tracks consecutive throttle failures for exponential backoff. */
    @Volatile private var throttleRetryCount: Int = 0
    /** Earliest time we're allowed to retry after throttling. */
    @Volatile private var throttleBackoffUntil: Long = 0L

    override suspend fun getIntegrityToken(): String? {
        val now = System.currentTimeMillis()

        // Return cached token if still fresh
        cachedToken?.let { token ->
            if (now - cachedTokenTimestamp < TOKEN_CACHE_TTL_MS) {
                return token
            }
        }

        return mutex.withLock {
            // Double-check after acquiring lock
            cachedToken?.let { token ->
                if (System.currentTimeMillis() - cachedTokenTimestamp < TOKEN_CACHE_TTL_MS) {
                    return@withLock token
                }
            }

            // Respect backoff window
            val currentTime = System.currentTimeMillis()
            if (currentTime < throttleBackoffUntil) {
                val waitSec = (throttleBackoffUntil - currentTime) / 1000
                android.util.Log.w(TAG, "Throttle backoff active, skipping request (retry in ${waitSec}s)")
                return@withLock cachedToken  // return stale token if available, else null
            }

            fetchIntegrityToken()
        }
    }

    private suspend fun fetchIntegrityToken(): String? {
        val projectNumber = BuildConfig.GCP_PROJECT_NUMBER.toLongOrNull()
        if (projectNumber == null || projectNumber <= 0L) {
            android.util.Log.e(TAG, "Failed: invalid GCP project number")
            return null
        }

        return try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(projectNumber)
                .setNonce(generateNonce())
                .build()
            val tokenResponse = integrityManager.requestIntegrityToken(request).await()
            val token = tokenResponse.token()

            // Success — cache the token and reset backoff
            cachedToken = token
            cachedTokenTimestamp = System.currentTimeMillis()
            throttleRetryCount = 0
            throttleBackoffUntil = 0L

            token
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("TOO_MANY_REQUESTS", ignoreCase = true) ||
                message.contains("-8", ignoreCase = false)
            ) {
                // Exponential backoff: 5s, 10s, 20s, 40s, … capped at 2 min
                val delay = (BASE_BACKOFF_MS shl throttleRetryCount).coerceAtMost(MAX_BACKOFF_MS)
                throttleRetryCount++
                throttleBackoffUntil = System.currentTimeMillis() + delay
                android.util.Log.w(
                    TAG,
                    "Integrity API throttled (attempt #$throttleRetryCount), backing off for ${delay / 1000}s",
                    e
                )
                // Return stale cached token if available
                return cachedToken
            }

            android.util.Log.e(TAG, "Failed to get integrity token", e)
            null
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
