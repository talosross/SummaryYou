package com.talosross.summaryyou.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FossFlavorModule {

    @Provides
    @Singleton
    fun provideFlavorConfig(): FlavorConfig {
        return FossFlavorConfig()
    }
}

private class FossFlavorConfig : FlavorConfig {
    override val proxyBaseUrl: String? = null
    override suspend fun getIntegrityToken(): String? = null
}
