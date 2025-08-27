package me.nanova.summaryexpressive.llm

import me.nanova.summaryexpressive.R

enum class AIProvider(val displayName: String, val isBaseUrlCustomisable: Boolean, val icon: Int, val enabled: Boolean = true) {
    OPENAI("OpenAI", true, R.drawable.chatgpt),
    GEMINI("Gemini", true, R.drawable.gemini),
    CLAUDE("Anthropic", true, R.drawable.claude),

    // TODO
    GROQ("groq", false, R.drawable.chatgpt, false)
}