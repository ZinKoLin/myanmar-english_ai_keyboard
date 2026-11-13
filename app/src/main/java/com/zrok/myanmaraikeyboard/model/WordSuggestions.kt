package com.zrok.myanmaraikeyboard.model

/** Offline English completions and spelling alternatives. Never uploads typed text. */
object WordSuggestions {
    data class Match(val prefix: String, val words: List<String>, val isKnownWord: Boolean = false)
    private val english = """
        hello hi how are you your yours thank thanks welcome please sorry yes no okay
        good morning afternoon evening night goodbye see soon today tomorrow yesterday
        beautiful great nice happy love like want need know think understand remember
        work working home school office meeting message call phone friend family
        what where when why who which can could would should will have has had
        doing going coming waiting looking talking reading writing learning help
        time later now ready busy fine really very much more also just about
        because before after again English Myanmar translation translate send
        information important possible problem question answer anything everything
        vocabulary receive definitely keyboard grammar spelling correct suggestion dictionary
    """.trimIndent().split(Regex("\\s+")).distinct()
    private val preferred = english.map { it.lowercase(java.util.Locale.ROOT) }
    private val priority = preferred.withIndex().associate { it.value to it.index }
    @Volatile private var vocabulary: java.util.NavigableSet<String> = java.util.TreeSet(preferred)

    /** Build off the UI thread, then publish an immutable snapshot. */
    fun loadEnglishWords(words: Sequence<String>) {
        val expanded = java.util.TreeSet(preferred)
        words.map { it.trim().trim('"').lowercase(java.util.Locale.ROOT) }
            .filter { it.isNotEmpty() && it.length <= 48 && it.all { c -> c in 'a'..'z' || c == '\'' } }
            .forEach { expanded.add(it) }
        vocabulary = expanded
    }

    private fun corrections(word: String, dictionary: Set<String>): List<String> {
        if (word.length !in 3..32) return emptyList()
        val matches = linkedSetOf<String>()
        fun consider(value: String) { if (value != word && value in dictionary) matches.add(value) }
        for (i in word.indices) {
            consider(word.removeRange(i, i + 1))
            if (i < word.lastIndex) consider(word.substring(0, i) + word[i + 1] + word[i] + word.substring(i + 2))
            for (letter in 'a'..'z') consider(word.substring(0, i) + letter + word.substring(i + 1))
        }
        for (i in 0..word.length) {
            for (letter in 'a'..'z') consider(word.substring(0, i) + letter + word.substring(i))
        }
        return matches.sortedWith(compareBy<String> { priority[it] ?: Int.MAX_VALUE }.thenBy { it })
    }
    fun isWord(c: Char) = c.isLetter() || Character.getType(c) in listOf(
        Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt()
    ) || c == '\''

    /** Prefer popular online completions, but retain closer offline spelling corrections. */
    fun mergeOnline(local: Match, online: List<String>): Match {
        val prefix = local.prefix.lowercase(java.util.Locale.ROOT)
        if (prefix.isEmpty()) return local
        val dictionary = vocabulary
        val remote = online.map { it.lowercase(java.util.Locale.ROOT) }
            .filter { it in dictionary }.distinct()
        if (remote.isEmpty()) return local
        val candidates = (remote + local.words.map { it.lowercase(java.util.Locale.ROOT) }).distinct()
        val ranked = candidates.sortedWith(compareBy<String> {
            when {
                local.isKnownWord && it == prefix -> -1
                it.startsWith(prefix) -> 0
                else -> 1
            }
        }.thenBy { if (it.startsWith(prefix)) 0 else spellingDistance(prefix, it) })
        return local.copy(words = ranked.take(6).map { applyCase(it, local.prefix) })
    }

    private fun spellingDistance(a: String, b: String): Int {
        val rows = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) rows[i][0] = i
        for (j in 0..b.length) rows[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            rows[i][j] = minOf(rows[i - 1][j] + 1, rows[i][j - 1] + 1,
                rows[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                rows[i][j] = minOf(rows[i][j], rows[i - 2][j - 2] + 1)
            }
        }
        return rows[a.length][b.length]
    }

    private fun applyCase(word: String, prefix: String) = when {
        prefix.isNotEmpty() && prefix.all { it.isUpperCase() } -> word.uppercase(java.util.Locale.ROOT)
        prefix.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercase() }
        else -> word
    }

    fun suggest(before: String, after: String, myanmar: Boolean): Match {
        if (myanmar) return Match("", emptyList())
        if (after.firstOrNull()?.let(::isWord) == true) return Match("", emptyList())
        val prefix = before.takeLastWhile(::isWord)
        if (prefix.length > 48 || prefix.any { it !in 'a'..'z' && it !in 'A'..'Z' && it != '\'' }) return Match(prefix, emptyList())
        val dictionary = vocabulary
        val normalized = prefix.lowercase(java.util.Locale.ROOT)
        val known = normalized in dictionary
        val candidates = linkedSetOf<String>()
        if (known) candidates.add(normalized)
        preferred.filter { it.startsWith(normalized) }.take(3).forEach { candidates.add(it) }
        if (candidates.size < 3) corrections(normalized, dictionary).take(3).forEach { candidates.add(it) }
        if (candidates.size < 3) dictionary.tailSet(normalized).asSequence()
            .takeWhile { it.startsWith(normalized) }.take(3).forEach { candidates.add(it) }
        val words = candidates.take(3).map { applyCase(it, prefix) }
        return Match(prefix, words, known)
    }
}
