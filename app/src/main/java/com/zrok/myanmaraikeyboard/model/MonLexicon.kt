package com.zrok.myanmaraikeyboard.model

import java.text.Normalizer

/** Human-attributed MonDictDB entries. A glossary is evidence, not sentence-level translation. */
class MonLexicon(lines: Sequence<String>) {
    data class Entry(val mon: String, val burmese: String)
    private val entries = lines.mapNotNull { line ->
        val parts = line.split('\t', limit = 2)
        if (parts.size == 2 && parts.all { it.isNotBlank() })
            Entry(parts[0].replace(senseSuffix, ""), parts[1]) else null
    }.toList()
    private val terms = entries.flatMap { entry ->
        entry.burmese.replace(numberPrefix, "")
            .replace(posPrefix, "")
            .split('။', '၊', ';').map { it.trim() }
            .filter { it.length in 2..60 && !it.contains('(') && !it.contains(')') && it !in stopWords }
            .map { normalize(it) to entry }
    }.groupBy({ it.first }, { it.second })

    val size get() = entries.size

    fun exactBurmese(text: String): String? {
        val key = normalize(text).trimEnd('။', '.', ' ', '\n')
        // Only a single, unambiguous dictionary term can bypass the sentence translator.
        val matches = terms[key].orEmpty().map { it.mon }.distinct()
        return matches.singleOrNull()
    }

    fun glossary(text: String, limit: Int = 12): List<Entry> {
        val normalized = normalize(text)
        val matches = terms.entries.filter { (term, _) ->
            var start = normalized.indexOf(term)
            var found = false
            while (start >= 0 && !found) {
                val end = start + term.length
                found = end == normalized.length || Character.getType(normalized[end]) !in setOf(
                    Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt())
                if (!found) start = normalized.indexOf(term, start + 1)
            }
            found
        }
            .sortedByDescending { it.key.length }
        val selectedTerms = mutableListOf<String>()
        val selected = linkedSetOf<Entry>()
        for ((term, values) in matches) {
            if (selectedTerms.any { it.contains(term) }) continue
            selectedTerms.add(term)
            for (entry in values.take(3)) {
                selected.add(entry)
                if (selected.size >= limit) return selected.toList()
            }
        }
        return selected.toList()
    }

    companion object {
        private val senseSuffix = Regex("[—–-][၀-၉0-9]+$")
        private val numberPrefix = Regex("^[၀-၉0-9]+[။.]\\s*")
        private val posPrefix = Regex("^\\([^)]{1,6}\\)\\s*")
        private val invisible = Regex("[\\u200B-\\u200D\\uFEFF]")
        private val stopWords = setOf("သည်", "တယ်", "ပါ", "ကို", "မှာ")
        fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFC)
            .replace(invisible, "").trim()
    }
}
