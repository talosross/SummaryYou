package com.talosross.summaryyou.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import com.talosross.summaryyou.UserPreferencesRepository
import com.talosross.summaryyou.data.AppDatabase
import com.talosross.summaryyou.data.HistoryDao
import com.talosross.summaryyou.data.HistoryRepository
import com.talosross.summaryyou.llm.LLMHandler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "summary_expressive_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
        return appDatabase.historyDao()
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(historyDao: HistoryDao): HistoryRepository {
        return HistoryRepository(historyDao)
    }

    @Provides
    @Singleton
    fun provideLLMHandler(
        @ApplicationContext context: Context,
        httpClient: HttpClient
    ): LLMHandler {
        return LLMHandler(context, httpClient)
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) { json() }
        }
    }
}