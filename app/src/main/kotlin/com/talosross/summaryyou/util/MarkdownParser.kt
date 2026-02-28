package com.talosross.summaryyou.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Lightweight Markdown parser that converts a subset of Markdown into an
 * [AnnotatedString].  Only the formatting commonly produced by LLM summaries
 * is supported:
 *
 * - **bold** / __bold__
 * - *italic* / _italic_
 * - ***bold+italic***
 * - ~~strikethrough~~
 * - Unordered lists (`- ` / `* `)
 * - Ordered lists (`1. `)
 * - Headings (`# ` … `###### `) – rendered as **bold** text (same size)
 *
 * Everything else is passed through as plain text so the caller's [TextStyle]
 * (font family, size, color) is preserved unchanged.
 */
fun parseMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.lines()
    lines.forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()

        // ── Heading: strip leading `#`s and render the rest as bold ──
        val headingMatch = HEADING_REGEX.matchAt(line, 0)
        if (headingMatch != null) {
            val content = headingMatch.groupValues[2]
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlineMarkdown(content)
            }
            if (index < lines.lastIndex) append('\n')
            return@forEachIndexed
        }

        // ── Unordered list: `- ` or `* ` (with optional leading whitespace) ──
        val ulMatch = UL_REGEX.matchAt(line, 0)
        if (ulMatch != null) {
            val indent = ulMatch.groupValues[1]
            val content = ulMatch.groupValues[2]
            append(indent)
            append("  •  ")
            appendInlineMarkdown(content)
            if (index < lines.lastIndex) append('\n')
            return@forEachIndexed
        }

        // ── Ordered list: `1. ` (with optional leading whitespace) ──
        val olMatch = OL_REGEX.matchAt(line, 0)
        if (olMatch != null) {
            val indent = olMatch.groupValues[1]
            val number = olMatch.groupValues[2]
            val content = olMatch.groupValues[3]
            append(indent)
            append("  $number.  ")
            appendInlineMarkdown(content)
            if (index < lines.lastIndex) append('\n')
            return@forEachIndexed
        }

        // ── Normal line ──
        appendInlineMarkdown(line)
        if (index < lines.lastIndex) append('\n')
    }
}

// ─── Inline Markdown Parsing ────────────────────────────────────────────────

/**
 * Regex that matches the next inline-markdown token.
 *
 * Groups (named so we can tell which one matched):
 *  1 – bold+italic   `***…***`
 *  2 – bold           `**…**` or `__…__`
 *  3 – italic         `*…*`  or `_…_`  (single, not preceded/followed by same)
 *  4 – strikethrough  `~~…~~`
 *
 * The order matters: longer delimiters must come first so `***` is not
 * consumed as `*` + `**`.
 */
private val INLINE_REGEX = Regex(
    """\*\*\*(.+?)\*\*\*""" +            // group 1: bold+italic
    """|\*\*(.+?)\*\*""" +               // group 2: bold (**)
    """|__(.+?)__""" +                   // group 3: bold (__)
    """|\*(.+?)\*""" +                   // group 4: italic (*)
    """|(?<![a-zA-Z0-9])_(.+?)_(?![a-zA-Z0-9])""" + // group 5: italic (_) – word-boundary aware
    """|~~(.+?)~~"""                     // group 6: strikethrough
)

private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.+)""")
private val UL_REGEX = Regex("""^(\s*)[-*]\s+(.+)""")
private val OL_REGEX = Regex("""^(\s*)(\d+)\.\s+(.+)""")

/**
 * Appends [text] to the current [AnnotatedString.Builder], converting
 * inline Markdown tokens (bold, italic, strikethrough) into [SpanStyle]s.
 */
private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var cursor = 0
    for (match in INLINE_REGEX.findAll(text)) {
        // Append any plain text before this match
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }

        when {
            // ***bold+italic***
            match.groupValues[1].isNotEmpty() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(match.groupValues[1])
                }
            }
            // **bold**
            match.groupValues[2].isNotEmpty() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineMarkdown(match.groupValues[2])
                }
            }
            // __bold__
            match.groupValues[3].isNotEmpty() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineMarkdown(match.groupValues[3])
                }
            }
            // *italic*
            match.groupValues[4].isNotEmpty() -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(match.groupValues[4])
                }
            }
            // _italic_
            match.groupValues[5].isNotEmpty() -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineMarkdown(match.groupValues[5])
                }
            }
            // ~~strikethrough~~
            match.groupValues[6].isNotEmpty() -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInlineMarkdown(match.groupValues[6])
                }
            }
        }

        cursor = match.range.last + 1
    }

    // Append any remaining plain text after the last match
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}

