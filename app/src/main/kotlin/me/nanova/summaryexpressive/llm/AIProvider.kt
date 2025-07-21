package me.nanova.summaryexpressive.llm

enum class AIProvider(val displayName: String, val isBaseUrlCustomisable: Boolean) {
    GEMINI("Gemini", false),
    OPENAI("OpenAI", true),
    GROQ("groq", false)
}