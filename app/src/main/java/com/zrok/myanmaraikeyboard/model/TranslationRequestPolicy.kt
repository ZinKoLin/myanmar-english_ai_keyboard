package com.zrok.myanmaraikeyboard.model

object TranslationRequestPolicy {
    const val MAX_ATTEMPTS = 3
    fun budgetMs(textLength: Int) = if (textLength <= 600) 20_000L else 40_000L
    fun models(mon: Boolean, previous: String?): List<String> {
        val defaults = if (mon) listOf("gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite")
            else listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash")
        return (listOfNotNull(previous?.takeIf { it in defaults }) + defaults).distinct()
    }
    fun thinkingLevel(mon: Boolean) = if (mon) "low" else "minimal"
}
