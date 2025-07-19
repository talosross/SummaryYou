package com.talosross.summaryexpressive

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object UserPreferencesRepository {
    private val USE_ORIGINAL_LANGUAGE = booleanPreferencesKey("use_original_language")
    private val MULTI_LINE = booleanPreferencesKey("multi_line")
    private val ULTRA_DARK = booleanPreferencesKey("ultra_dark")
    private val DESIGN_NUMBER = intPreferencesKey("design_number")
    private val BASE_URL = stringPreferencesKey("base_url")
    private val API_KEY = stringPreferencesKey("api_key")
    private val MODEL = stringPreferencesKey("model")
    private val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
    private val SHOW_LENGTH = booleanPreferencesKey("show_length")
    private val SHOW_LENGTH_NUMBER = intPreferencesKey("show_length_number")
    private val TEXT_SUMMARIES = stringPreferencesKey("text_summaries_json")

    fun getUseOriginalLanguage(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[USE_ORIGINAL_LANGUAGE] ?: false }

    suspend fun setUseOriginalLanguage(context: Context, value: Boolean) =
        context.dataStore.edit { it[USE_ORIGINAL_LANGUAGE] = value }

    fun getMultiLine(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[MULTI_LINE] ?: true }

    suspend fun setMultiLine(context: Context, value: Boolean) =
        context.dataStore.edit { it[MULTI_LINE] = value }

    fun getUltraDark(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ULTRA_DARK] ?: false }

    suspend fun setUltraDark(context: Context, value: Boolean) =
        context.dataStore.edit { it[ULTRA_DARK] = value }

    fun getDesignNumber(context: Context): Flow<Int> =
        context.dataStore.data.map { it[DESIGN_NUMBER] ?: 0 }

    suspend fun setDesignNumber(context: Context, value: Int) =
        context.dataStore.edit { it[DESIGN_NUMBER] = value }

    fun getBaseUrl(context: Context): Flow<String> =
        context.dataStore.data.map { it[BASE_URL] ?: "" }

    suspend fun setBaseUrl(context: Context, value: String) =
        context.dataStore.edit { it[BASE_URL] = value }

    fun getApiKey(context: Context): Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    suspend fun setApiKey(context: Context, value: String) =
        context.dataStore.edit { it[API_KEY] = value }

    fun getModel(context: Context): Flow<String> =
        context.dataStore.data.map { it[MODEL] ?: "Gemini" }

    suspend fun setModel(context: Context, value: String) =
        context.dataStore.edit { it[MODEL] = value }

    fun getShowOnboarding(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SHOW_ONBOARDING] ?: true }

    suspend fun setShowOnboarding(context: Context, value: Boolean) =
        context.dataStore.edit { it[SHOW_ONBOARDING] = value }

    fun getShowLength(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SHOW_LENGTH] ?: true }

    suspend fun setShowLength(context: Context, value: Boolean) =
        context.dataStore.edit { it[SHOW_LENGTH] = value }

    fun getShowLengthNumber(context: Context): Flow<Int> =
        context.dataStore.data.map { it[SHOW_LENGTH_NUMBER] ?: 0 }

    suspend fun setShowLengthNumber(context: Context, value: Int) =
        context.dataStore.edit { it[SHOW_LENGTH_NUMBER] = value }

    fun getTextSummaries(context: Context): Flow<String> =
        context.dataStore.data.map { it[TEXT_SUMMARIES] ?: "[]" }

    suspend fun setTextSummaries(context: Context, json: String) =
        context.dataStore.edit { it[TEXT_SUMMARIES] = json }
}