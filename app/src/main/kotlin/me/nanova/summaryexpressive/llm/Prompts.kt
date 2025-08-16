package me.nanova.summaryexpressive.llm

import ai.koog.prompt.dsl.Prompt

enum class SummaryLength {
    SHORT, MEDIUM, LONG
}

fun createSummarizationPrompt(length: SummaryLength, language: String): Prompt {
    val lengthInstruction = when (length) {
        SummaryLength.SHORT -> "a few sentences(better within 100 words)"
        SummaryLength.MEDIUM -> "two to three paragraphs"
        SummaryLength.LONG -> "a detailed, multi-paragraph summary"
    }

    return Prompt.build("summarizer-prompt") {
        system(
            """
            You are an expert summarization assistant. Your task is to produce a clear, concise, and accurate summary of the provided text.
            The summary should be written in $language.
            The summary should be about $lengthInstruction long, and must not exceed the length of the original content.

            - If the text is an article, focus on the main arguments, key points, and conclusions.
            - If the text is a video transcript, focus on the key topics and speakers' points.
            - If the text is from a document, focus on the core information and purpose.
            
            Include the main point and any conclusion if relevant.
            Do not use any headings, introductions, or metacommentary.
            No markdown formatting or special characters.
            Better highlight the main concept or viewpoint with bulleted or numbered list.
            If you receive an error message as input, do not try to summarize it. Instead, repeat the error message back to the user verbatim.
            """.trimIndent()
            // TODO: Structure the output in well-structured markdown for readability.
        )
    }
}