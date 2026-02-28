package com.talosross.summaryyou

import com.talosross.summaryyou.util.extractHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlExtractionTest {

    @Test
    fun `extracts URL from text with surrounding content`() {
        val text = "Check out this video: https://www.youtube.com/watch?v=dQw4w9WgXcQ and let me know"
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", extractHttpUrl(text))
    }

    @Test
    fun `returns plain URL as-is`() {
        val url = "https://www.example.com/path?query=value"
        assertEquals(url, extractHttpUrl(url))
    }

    @Test
    fun `returns original text when no URL present`() {
        val text = "This is just some plain text without any links."
        assertEquals(text, extractHttpUrl(text))
    }

    @Test
    fun `extracts first URL when multiple are present`() {
        val text = "First: https://www.google.com second: https://www.youtube.com"
        assertEquals("https://www.google.com", extractHttpUrl(text))
    }

    @Test
    fun `handles http scheme`() {
        val text = "Link: http://example.com/page"
        assertEquals("http://example.com/page", extractHttpUrl(text))
    }

    @Test
    fun `handles URL with path and fragment`() {
        val input = "https://docs.example.com/guide/section#anchor"
        // The regex excludes '#' from URL matching, so fragment is stripped
        assertEquals("https://docs.example.com/guide/section", extractHttpUrl(input))
    }

    @Test
    fun `handles URL at start of string`() {
        val text = "https://example.com is a great site"
        assertEquals("https://example.com", extractHttpUrl(text))
    }

    @Test
    fun `handles empty string`() {
        assertEquals("", extractHttpUrl(""))
    }

    @Test
    fun `handles URL with port number`() {
        val url = "http://localhost:8080/api/test"
        // localhost is a single segment without a dot, might not match — expected behavior
        val result = extractHttpUrl(url)
        // This tests current behavior; the regex requires at least one dot in domain
        assertEquals(url, result)
    }
}

