package me.nanova.summaryexpressive

enum class AIProvider(val displayName: String, val isBaseUrlCustomisable: Boolean) {
    GEMINI("Gemini", false),
    OPENAI("OpenAI", true),
//    GROQ("groq", false)
}