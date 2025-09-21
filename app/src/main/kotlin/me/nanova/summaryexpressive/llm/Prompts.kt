package me.nanova.summaryexpressive.llm

import ai.koog.prompt.dsl.Prompt

enum class SummaryLength {
    SHORT, MEDIUM, LONG
}

fun createSummarizationPrompt(
    length: SummaryLength,
    useOriginalLanguage: Boolean,
    language: String,
): Prompt {
    val lengthInstruction = when (length) {
        SummaryLength.SHORT -> "a few sentences(better within 100 words)"
        SummaryLength.MEDIUM -> "two to three paragraphs"
        SummaryLength.LONG -> "a detailed, multi-paragraph summary"
    }

    val languageInstruction = if (useOriginalLanguage) {
        """
        **Mandatory Procedure:**
        1.  **Identify Content Language:** First, determine the original language of the 'content' field in the user's request. This is the SOLE source for language identification. Ignore tool call details for this step.
        2.  **Summarize in Identified Language:** Generate the summary STRICTLY in the language identified in step 1.
        The summary should be written in $language.
        """
    } else "The summary should be written in $language."

    return Prompt.build("summarizer-prompt") {
        system(
            """
            You are an expert summarization assistant. Your task is to produce a clear, concise, and accurate summary of the provided text.
            $languageInstruction
            The summary should be about $lengthInstruction long, and must not exceed the length of the original content.

            - If the text is an article, focus on the main arguments, key points, and conclusions.
            - If the text is a video transcript, focus on the key topics and speakers' points.
            - If the text is from a document, focus on the core information and purpose.
            
            Include the main point and any conclusion if relevant.
            Do not use any headings, introductions, or metacommentary.
            No markdown formatting or special characters.
            Highlight the main concepts or viewpoints with bulleted or numbered list.
            If you receive an error message as input, do not try to summarize it. Instead, repeat the error message back to the user verbatim.
            """.trimIndent()
            // TODO: Structure the output in well-structured markdown for readability.
        )
    }
}