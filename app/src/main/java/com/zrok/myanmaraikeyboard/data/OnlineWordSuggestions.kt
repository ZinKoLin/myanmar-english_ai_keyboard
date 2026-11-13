package com.zrok.myanmaraikeyboard.data

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Datamuse autocomplete. Only an English word fragment is sent; cache stays in memory. */
class OnlineWordSuggestions {
    private data class Entry(val words: List<String>, val expires: Long)
    private val cache = object : LinkedHashMap<String, Entry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > 256
    }
    @Volatile private var retryAfter = 0L
    @Volatile private var generation = 0L

    @Synchronized fun cached(prefix: String): List<String>? {
        val key = prefix.lowercase(Locale.ROOT)
        val entry = cache[key] ?: return null
        if (entry.expires <= SystemClock.elapsedRealtime()) {
            cache.remove(key)
            return null
        }
        return entry.words
    }

    suspend fun fetch(prefix: String): List<String>? = withContext(Dispatchers.IO) {
        if (!eligible(prefix)) return@withContext null
        cached(prefix)?.let { return@withContext it }
        if (SystemClock.elapsedRealtime() < retryAfter) return@withContext null
        currentCoroutineContext().ensureActive()
        val key = prefix.lowercase(Locale.ROOT)
        val url = "https://api.datamuse.com/sug?s=${URLEncoder.encode(key, "UTF-8")}&max=12"
        val started = generation
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 2500
            readTimeout = 2500
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MyanmarAIKeyboard/0.1")
        }
        try {
            val code = connection.responseCode
            if (code != 200) {
                retryAfter = SystemClock.elapsedRealtime() + if (code == 429 || code in 401..403) 300_000 else 15_000
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(65_536)
                var count = 0
                while (count < buffer.size) {
                    val read = reader.read(buffer, count, buffer.size - count)
                    if (read < 0) break
                    count += read
                }
                require(count < buffer.size) { "Suggestion response too large" }
                String(buffer, 0, count)
            }
            val response = JSONArray(body)
            val words = (0 until minOf(response.length(), 12)).mapNotNull { index ->
                response.optJSONObject(index)?.optString("word")?.trim()?.lowercase(Locale.ROOT)
                    ?.takeIf { it.length in 1..48 && it.all { c -> c in 'a'..'z' || c == '\'' } }
            }.distinct()
            currentCoroutineContext().ensureActive()
            synchronized(this@OnlineWordSuggestions) {
                if (started == generation) cache[key] = Entry(words, SystemClock.elapsedRealtime() + 15 * 60_000)
            }
            words
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            retryAfter = SystemClock.elapsedRealtime() + 15_000
            null
        } finally {
            connection.disconnect()
        }
    }

    @Synchronized fun clear() { generation++; cache.clear() }

    companion object {
        fun eligible(prefix: String) = prefix.length in 2..32 && prefix.all {
            it in 'a'..'z' || it in 'A'..'Z' || it == '\''
        } && prefix.any { it.isLetter() }
    }
}
