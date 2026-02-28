package com.talosross.summaryyou

import com.talosross.summaryyou.model.SummaryException
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryExceptionTest {

    @Test
    fun `paywall message maps to PaywallException`() {
        val result = SummaryException.fromMessage("Paywall detected.")
        assertTrue(result is SummaryException.PaywallException)
    }

    @Test
    fun `bilibili login message maps to BiliBiliLoginRequiredException`() {
        val result = SummaryException.fromMessage("BiliBili login required to access subtitles.")
        assertTrue(result is SummaryException.BiliBiliLoginRequiredException)
    }

    @Test
    fun `video ID extraction failure maps to InvalidLinkException`() {
        val result = SummaryException.fromMessage("Could not extract video ID from URL")
        assertTrue(result is SummaryException.InvalidLinkException)
    }

    @Test
    fun `transcript error maps to NoTranscriptException`() {
        val result = SummaryException.fromMessage("Could not get transcript.")
        assertTrue(result is SummaryException.NoTranscriptException)
    }

    @Test
    fun `URL extraction failure maps to NoContentException`() {
        val result = SummaryException.fromMessage("Could not extract text from URL.")
        assertTrue(result is SummaryException.NoContentException)
    }

    @Test
    fun `unsupported file type maps to InvalidLinkException`() {
        val result = SummaryException.fromMessage("Unsupported file type for URI.")
        assertTrue(result is SummaryException.InvalidLinkException)
    }

    @Test
    fun `empty file text maps to NoContentException`() {
        val result = SummaryException.fromMessage("Extracted text from file is empty.")
        assertTrue(result is SummaryException.NoContentException)
    }

    @Test
    fun `API key error maps to IncorrectKeyException`() {
        val result = SummaryException.fromMessage("Invalid API key provided.")
        assertTrue(result is SummaryException.IncorrectKeyException)
    }

    @Test
    fun `rate limit error maps to RateLimitException`() {
        val result = SummaryException.fromMessage("Rate limit exceeded, try later.")
        assertTrue(result is SummaryException.RateLimitException)
    }

    @Test
    fun `unknown message maps to UnknownException`() {
        val result = SummaryException.fromMessage("Something completely unexpected happened.")
        assertTrue(result is SummaryException.UnknownException)
    }

    @Test
    fun `unknown exception preserves message`() {
        val msg = "Some detailed error message"
        val result = SummaryException.fromMessage(msg)
        assertTrue(result is SummaryException.UnknownException)
        assertTrue(result.message == msg)
    }
}
