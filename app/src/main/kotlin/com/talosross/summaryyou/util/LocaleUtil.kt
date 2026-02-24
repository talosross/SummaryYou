package com.talosross.summaryyou.util

import android.content.Context
import android.util.Log
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private suspend fun identifyLanguage(context: Context, text: String): String? {
    return withContext(Dispatchers.Default) {
        try {
            val textClassificationManager =
                context.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE) as TextClassificationManager
            val textClassifier = textClassificationManager.textClassifier
            val textRequest = TextLanguage.Request.Builder(text).build()
            val detectedLanguage = textClassifier.detectLanguage(textRequest)
            if (detectedLanguage.localeHypothesisCount > 0) {
                (0 until detectedLanguage.localeHypothesisCount)
                    .map { i -> detectedLanguage.getLocale(i) }
                    .maxByOrNull { locale -> detectedLanguage.getConfidenceScore(locale) }
                    ?.toLanguageTag()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LangId", "Language identification with TextClassifier failed", e)
            null
        }
    }
}

suspend fun getLanguageCode(context: Context, text: String): String? {
    return identifyLanguage(context, text)?.takeIf { it != "und" }
}