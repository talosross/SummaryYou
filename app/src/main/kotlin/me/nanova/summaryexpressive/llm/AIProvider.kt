package me.nanova.summaryexpressive.llm

import me.nanova.summaryexpressive.R

enum class AIProvider(val displayName: String, val isBaseUrlCustomisable: Boolean, val icon: Int) {
    GEMINI("Gemini", false, R.drawable.gemini),
    OPENAI("OpenAI", true, R.drawable.chatgpt),
    // fixme
    GROQ("groq", false, R.drawable.chatgpt)
}