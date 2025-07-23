package me.nanova.summaryexpressive.llm

object Prompts {

    enum class ContentType {
        VIDEO_TRANSCRIPT, ARTICLE, TEXT, DOCUMENT
    }

    // --- Template components ---
    private const val CONCLUSION_TAKEAWAY = "If it includes a conclusion or key takeaway, make sure to include that in the end."

    private fun getOpenAIContentSource(type: ContentType, title: String?): String {
        val titlePart = if (!title.isNullOrBlank()) " '$title'" else ""
        return when (type) {
            ContentType.VIDEO_TRANSCRIPT -> "a transcript of the video$titlePart"
            ContentType.ARTICLE -> "the article$titlePart"
            ContentType.TEXT -> "a text"
            ContentType.DOCUMENT -> "a document"
        }
    }

    // --- OpenAI Prompts ---
    fun openAIPrompt(type: ContentType, title: String?, length: Int, language: String): String {
        val contentSource = getOpenAIContentSource(type, title)
        val contentTypeString = when (type) {
            ContentType.VIDEO_TRANSCRIPT -> "transcript"
            ContentType.ARTICLE -> "text"
            ContentType.TEXT -> "text"
            ContentType.DOCUMENT -> "document"
        }

        val summarySpec = when (length) {
            0 -> "a very short, concise summary with a maximum of 20 words of the $contentTypeString using only 3 bullet points"
            1 -> "a very short, concise summary with a maximum of 60 words of the $contentTypeString. $CONCLUSION_TAKEAWAY"
            else -> "a summary of the $contentTypeString. $CONCLUSION_TAKEAWAY"
        }

        return "You will be provided with $contentSource, and your task is to generate $summarySpec in $language."
    }

    // --- Gemini/Groq Prompts ---

    private fun getGeneralContentSource(type: ContentType, title: String?, forVideo: String): String {
        val titlePart = if (!title.isNullOrBlank()) " '$title'" else ""
        return when (type) {
            ContentType.VIDEO_TRANSCRIPT -> "$forVideo$titlePart"
            ContentType.ARTICLE -> "the article$titlePart"
            ContentType.TEXT -> "this text"
            ContentType.DOCUMENT -> "this document"
        }
    }

    fun geminiPrompt(type: ContentType, title: String?, length: Int, language: String): String {
        val contentSource = getGeneralContentSource(type, title, "the video")
        return buildGeminiGroqPrompt(contentSource, type, length, language)
    }

    fun groqPrompt(type: ContentType, title: String?, length: Int, language: String): String {
        val contentSource = getGeneralContentSource(type, title, "the video transcript")
        return buildGeminiGroqPrompt(contentSource, type, length, language)
    }

    private fun buildGeminiGroqPrompt(contentSource: String, type: ContentType, length: Int, language: String): String {
        return when (length) {
            0 -> "Act as an expert content summarizer. Extract exactly 3 key points from $contentSource. Format as 3 bullet points only, each starting with a dash, each containing 3-5 words maximum, and not forming complete sentences. Do not include any introductory text, conclusion, or explanations. No markdown formatting. Deliver only the 3 bullet points in $language."
            1 -> "Act as a professional summarizer. Condense $contentSource into a single paragraph of exactly 70 words. Include the main point and any conclusion if relevant. Do not use any headings, introductions, or metacommentary. No markdown formatting or special characters. Deliver only the 70-word summary in $language."
            else -> {
                if (type == ContentType.VIDEO_TRANSCRIPT) {
                    "Act as a content analysis specialist. Create a detailed summary of $contentSource using exactly 130 words. Cover the main topic, key points, and any conclusions in a cohesive narrative. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the 130-word summary in $language."
                } else {
                    "Act as a content analysis specialist. Create a comprehensive summary of $contentSource that captures its essential information, arguments, and conclusions. Do not include any headings, introductions, or phrases like 'In summary' or 'In conclusion'. No markdown formatting. Deliver only the summary in $language."
                }
            }
        }
    }
}