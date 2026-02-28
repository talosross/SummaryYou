package com.talosross.summaryyou

import com.talosross.summaryyou.llm.tools.YouTubeTranscriptTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class YouTubeExtractorTest {

    // --- extractVideoId ---

    @Test
    fun `standard watch URL extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `short youtu_be URL extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `shorts URL extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test
    fun `embed URL extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test
    fun `live URL extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/live/dQw4w9WgXcQ"))
    }

    @Test
    fun `URL without scheme extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `URL with extra query params extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLtest&index=1"))
    }

    @Test
    fun `mobile URL extracts video ID`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptTool.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `invalid URL returns null`() {
        assertNull(YouTubeTranscriptTool.extractVideoId("https://www.google.com"))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(YouTubeTranscriptTool.extractVideoId(""))
    }

    @Test
    fun `plain text returns null`() {
        assertNull(YouTubeTranscriptTool.extractVideoId("not a url at all"))
    }

    @Test
    fun `URL with invalid video ID length returns null`() {
        assertNull(YouTubeTranscriptTool.extractVideoId("https://www.youtube.com/watch?v=short"))
    }

    // --- isYouTubeLink ---

    @Test
    fun `standard youtube_com is YouTube link`() {
        assertTrue(YouTubeTranscriptTool.isYouTubeLink("https://www.youtube.com/watch?v=test12345ab"))
    }

    @Test
    fun `youtu_be is YouTube link`() {
        assertTrue(YouTubeTranscriptTool.isYouTubeLink("https://youtu.be/test12345ab"))
    }

    @Test
    fun `mobile youtube is YouTube link`() {
        assertTrue(YouTubeTranscriptTool.isYouTubeLink("https://m.youtube.com/watch?v=test12345ab"))
    }

    @Test
    fun `non-youtube domain is not YouTube link`() {
        assertFalse(YouTubeTranscriptTool.isYouTubeLink("https://www.google.com"))
    }

    @Test
    fun `bilibili is not YouTube link`() {
        assertFalse(YouTubeTranscriptTool.isYouTubeLink("https://www.bilibili.com/video/BV1xx411c7mD"))
    }

    @Test
    fun `empty string is not YouTube link`() {
        assertFalse(YouTubeTranscriptTool.isYouTubeLink(""))
    }
}

