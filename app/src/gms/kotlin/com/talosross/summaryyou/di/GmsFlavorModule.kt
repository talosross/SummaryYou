package com.talosross.summaryyou.di

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.talosross.summaryyou.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GmsFlavorModule {

    @Provides
    @Singleton
    fun provideFlavorConfig(@ApplicationContext context: Context): FlavorConfig {
        return GmsFlavorConfig(context)
    }
}

private class GmsFlavorConfig(private val context: Context) : FlavorConfig {

    override val proxyBaseUrl: String?
        get() = BuildConfig.PROXY_URL.takeIf { it.isNotBlank() }

    override suspend fun getIntegrityToken(): String? {
        return try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(BuildConfig.GCP_PROJECT_NUMBER)
                .build()
            val tokenResponse = integrityManager.requestIntegrityToken(request).await()
            tokenResponse.token()
        } catch (e: Exception) {
            android.util.Log.e("GmsFlavorConfig", "Failed to get integrity token", e)
            null
        }
    }
}

