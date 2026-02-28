package com.talosross.summaryyou.util

/**
 * Extracts the first HTTP/HTTPS URL found in the given text.
 * Returns the original text if no URL is found.
 */
fun extractHttpUrl(text: String): String {
    val urlRegex = Regex(
        "(?:^|\\W)((http|https)://)" + // Protocol
                "([\\w\\-]+\\.){1,}" + // Domain name
                "([\\w\\-]+)" + // Top-level domain
                "([^\\s<>\"#%{}|\\\\^`]*)" // Path, query, and fragment
    )
    return urlRegex.find(text)?.value?.trim() ?: text
}

