package com.zrok.myanmaraikeyboard

import android.content.Context
import android.os.SystemClock
import com.zrok.myanmaraikeyboard.data.AppPreferences
import com.zrok.myanmaraikeyboard.data.TranslationRepository
import com.zrok.myanmaraikeyboard.model.*
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

object MonTranslationChecks {
    @JvmStatic fun run(context: Context, progress: java.util.function.Consumer<String>): String = runBlocking {
        val preferences = AppPreferences(context)
        val direction = TranslationDirection(SupportedLanguages.require("my"), SupportedLanguages.require("mnw"))
        val report = StringBuilder()
        fun record(line: String) { report.append(line).append('\n'); progress.accept(line + "\n") }
        val start = SystemClock.elapsedRealtime()
        val dictionary = context.assets.open("mon_burmese_lexicon.tsv").bufferedReader().use { MonLexicon(it.lineSequence()) }
        check(dictionary.size == 19080)
        check(dictionary.exactBurmese("ငါးသေတ္တာ") == "ကကဠာ")
        check(dictionary.glossary("ကျွန်တော် ရေသောက်ချင်တယ်။").isNotEmpty())
        record("Dictionary: ${dictionary.size} human-attributed entries loaded in ${SystemClock.elapsedRealtime() - start} ms")
        val repository = TranslationRepository(preferences)
        val offlineStart = SystemClock.elapsedRealtime()
        check(repository.translate("ငါးသေတ္တာ", Tone.NATURAL, direction).getOrThrow() == "ကကဠာ")
        record("Exact dictionary: ငါးသေတ္တာ => ကကဠာ in ${SystemClock.elapsedRealtime() - offlineStart} ms")
        val warmStart = SystemClock.elapsedRealtime()
        check(repository.translate("ငါးသေတ္တာ", Tone.NATURAL, direction).getOrThrow() == "ကကဠာ")
        record("Warm exact dictionary: ${SystemClock.elapsedRealtime() - warmStart} ms")
        if (preferences.apiKeys.isEmpty() || preferences.azureEnabled) {
            record("Live Mon API comparison skipped: configure Gemini without Azure first")
            return@runBlocking report.toString()
        }
        val source = "ကျွန်တော် ရေသောက်ချင်တယ်။"
        // One baseline request reproduces the previously shipped prompt/config, using the same saved key.
        val baselineStart = SystemClock.elapsedRealtime()
        val connection = (URL(preferences.endpointForModel("gemini-3.6-flash")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 4000; readTimeout = 12000; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", preferences.apiKeys.first())
        }
        try {
            val prompt = "Detect the language of the following text automatically and translate it into Mon. The input may be in any language or contain multiple languages. Do not assume a source language. Use a natural tone. Preserve the meaning and return only the translation, with no explanation.\n\n$source"
            val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code == 200) {
                val data = connection.inputStream.bufferedReader().use { it.readText() }
                val translated = JSONObject(data).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()
                record("Previous prompt: ${SystemClock.elapsedRealtime() - baselineStart} ms => $translated")
            } else record("Previous prompt: HTTP $code after ${SystemClock.elapsedRealtime() - baselineStart} ms")
        } catch (_: Exception) { record("Previous prompt: network timeout/failure after ${SystemClock.elapsedRealtime() - baselineStart} ms") }
        finally { connection.disconnect() }
        for ((target, text) in listOf(direction to source, direction to "မင်္ဂလာပါ။ နေကောင်းလား။", TranslationDirection.MYANMAR_TO_ENGLISH to source)) {
            val began = SystemClock.elapsedRealtime()
            val result = repository.translate(text, Tone.NATURAL, target)
            val elapsed = SystemClock.elapsedRealtime() - began
            check(elapsed < 23000) { "Whole request exceeded its budget: $elapsed" }
            record("New ${target.target.code}: $elapsed ms => ${result.fold({ it }, { "FAILED: ${it.message}" })}")
        }
        report.toString()
    }
}
