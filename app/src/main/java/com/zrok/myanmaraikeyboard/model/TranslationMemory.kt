package com.zrok.myanmaraikeyboard.model

/** Small, session-only memory. Text is never written to disk. */
class TranslationMemory(private val limit: Int = 64) {
    data class Key(val text: String, val tone: Tone, val direction: TranslationDirection)
    private val entries = linkedMapOf<Key, String>()

    fun find(key: Key): String? = entries[key]

    fun remember(key: Key, result: String) {
        if (key.text.isBlank() || result.isBlank()) return
        entries[key] = result
        entries[Key(result, key.tone, key.direction.reversed())] = key.text
        while (entries.size > limit) entries.remove(entries.keys.first())
    }

    fun clear() = entries.clear()
}
