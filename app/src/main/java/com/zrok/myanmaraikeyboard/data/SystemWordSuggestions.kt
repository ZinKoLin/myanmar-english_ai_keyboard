package com.zrok.myanmaraikeyboard.data

import android.content.Context
import android.os.SystemClock
import android.os.Build
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

/** Uses the selected Android spell checker, including Google's Gboard service when selected. */
class SystemWordSuggestions(private val context: Context, private val onResult: () -> Unit) :
    SpellCheckerSession.SpellCheckerSessionListener {
    data class Result(val words: List<String>, val correct: Boolean?, val expires: Long)
    private val manager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
    private var session: SpellCheckerSession? = null
    private var sequence = 0
    private val pending = mutableMapOf<Int, String>()
    private val cache = object : LinkedHashMap<String, Result>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Result>?) = size > 256
    }

    fun cached(prefix: String): Result? {
        val key = prefix.lowercase(Locale.ROOT)
        val result = cache[key] ?: return null
        if (result.expires <= SystemClock.elapsedRealtime()) { cache.remove(key); return null }
        return result
    }

    @Suppress("DEPRECATION")
    fun request(prefix: String) {
        if (!OnlineWordSuggestions.eligible(prefix) || cached(prefix) != null || pending.containsValue(prefix)) return
        try {
            if (session == null) session = if (Build.VERSION.SDK_INT >= 31) {
                val params = SpellCheckerSession.SpellCheckerSessionParams.Builder()
                    .setLocale(Locale.US)
                    .setShouldReferToSpellCheckerLanguageSettings(false)
                    .setSupportedAttributes(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY or
                        SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS)
                    .build()
                manager.newSpellCheckerSession(params, context.mainExecutor, this)
            } else manager.newSpellCheckerSession(null, Locale.US, this, false)
            val checker = session ?: return
            val id = ++sequence
            pending[id] = prefix
            checker.getSuggestions(arrayOf(TextInfo(prefix, COOKIE, id)), 6, false)
        } catch (_: RuntimeException) {
            close()
        }
    }

    private fun accept(info: SuggestionsInfo) {
        if (info.cookie != COOKIE) return
        val prefix = pending.remove(info.sequence) ?: return
        val attributes = info.suggestionsAttributes
        val correct = when {
            attributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0 -> true
            attributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0 -> false
            else -> null
        }
        val words = (0 until maxOf(0, info.suggestionsCount)).mapNotNull { index ->
            info.getSuggestionAt(index)?.lowercase(Locale.ROOT)?.takeIf { word ->
                word.length in 1..48 && word.all { it in 'a'..'z' || it == '\'' }
            }
        }.distinct().take(6)
        cache[prefix.lowercase(Locale.ROOT)] = Result(words, correct, SystemClock.elapsedRealtime() + 15 * 60_000)
        onResult()
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) { results?.forEach(::accept) }
    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        results?.forEach { sentence ->
            for (index in 0 until sentence.suggestionsCount) accept(sentence.getSuggestionsInfoAt(index))
        }
    }

    fun cancel() { pending.clear(); runCatching { session?.cancel() } }
    fun close() { cancel(); runCatching { session?.close() }; session = null; cache.clear() }

    companion object { private const val COOKIE = 23041 }
}
