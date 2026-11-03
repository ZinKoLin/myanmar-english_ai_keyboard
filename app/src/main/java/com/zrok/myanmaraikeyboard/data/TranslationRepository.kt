package com.zrok.myanmaraikeyboard.data

import com.zrok.myanmaraikeyboard.model.Tone
import com.zrok.myanmaraikeyboard.model.TranslationDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TranslationRepository(private val preferences: AppPreferences) {
    suspend fun translate(
        text: String,
        tone: Tone,
        direction: TranslationDirection = TranslationDirection.MYANMAR_TO_ENGLISH
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!preferences.networkTranslationEnabled) {
            return@withContext Result.failure(TranslationException("Network translation is disabled in Privacy settings."))
        }
        if (text.isBlank()) return@withContext Result.failure(TranslationException("Type text first."))
        val apiKeys = if (preferences.geminiEnabled) preferences.apiKeys else emptyList()
        val openRouterKey = preferences.openRouterApiKey.trim()
        if (apiKeys.isEmpty() && openRouterKey.isBlank()) return@withContext Result.failure(TranslationException(
            if (!preferences.geminiEnabled) "Gemini is paused. Add an OpenRouter key or enable Gemini in settings."
            else "Add a Gemini or OpenRouter API key in the companion app."
        ))

        runCatching {
            val prompt = "Translate the following ${direction.sourceLanguage} text into ${direction.targetLanguage}. " +
                "Use a ${tone.promptValue} tone. Preserve the meaning and return only the translation, with no explanation.\n\n$text"
            val request = JSONObject().put(
                "contents", org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))
                )
            )
            var lastErrorCode = 0
            val attempts = mutableListOf<String>()
            for (model in preferences.geminiModels) {
                var moveToNextModel = false
                for ((index, key) in apiKeys.withIndex()) {
                currentCoroutineContext().ensureActive()
                val connection = (URL(preferences.endpointForModel(model)).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 20_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("x-goog-api-key", key)
                }
                try {
                    connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) attempts.add("$model: HTTP $code — ${safeError(body)}")
                    if (code == 400) {
                        lastErrorCode = code
                        // Invalid credentials can be reported as 400 by Gemini.
                        val invalidKey = body.contains("API_KEY_INVALID", ignoreCase = true) ||
                            body.contains("API key not valid", ignoreCase = true)
                        if (invalidKey && index < apiKeys.lastIndex) continue
                        moveToNextModel = true
                        break
                    }
                    if (code == 404) {
                        lastErrorCode = code
                        moveToNextModel = true
                        break
                    }
                    if (code in 401..403 || code == 429) {
                        lastErrorCode = code
                        if (index < apiKeys.lastIndex) continue
                        moveToNextModel = true
                        break
                    }
                    if (code == 503 || code == 504) {
                        lastErrorCode = code
                        moveToNextModel = true
                        break
                    }
                    if (code !in 200..299) throw IOException("Translation server returned $code.")
                    val json = JSONObject(body)
                    val candidates = json.optJSONArray("candidates")
                        ?: throw IOException("Gemini returned no candidates.")
                    val translation = candidates.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        .orEmpty()
                    return@runCatching translation.ifBlank { throw IOException("Gemini returned no translation text.") }
                } finally {
                    connection.disconnect()
                }
                }
                if (moveToNextModel) continue
            }
            if (openRouterKey.isNotBlank()) {
                var lastOpenRouterError: IOException? = null
                for (model in AppPreferences.OPENROUTER_MODELS) {
                    currentCoroutineContext().ensureActive()
                    try {
                        return@runCatching translateWithOpenRouter(prompt, openRouterKey, model)
                    } catch (error: IOException) {
                        attempts.add(error.message.orEmpty())
                        lastOpenRouterError = error
                    }
                }
                throw IOException("Translation fallback exhausted. " + attempts.distinct().joinToString("; "), lastOpenRouterError)
            }
            throw IOException("Gemini model/key fallback exhausted (last HTTP $lastErrorCode). Add an OpenRouter key for the DeepSeek free fallback, or check Gemini quota.")
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = {
                if (it is CancellationException) throw it
                Result.failure(TranslationException(it.message ?: "Translation failed."))
            }
        )
    }

    private fun translateWithOpenRouter(prompt: String, apiKey: String, model: String): String {
        val openRouterRequest = JSONObject()
            .put("model", model)
            .put("messages", org.json.JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ))
        val connection = (URL(AppPreferences.OPENROUTER_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("HTTP-Referer", "https://github.com/zrok/myanmar-ai-keyboard")
            setRequestProperty("X-Title", "Myanmar AI Keyboard")
        }
        try {
            connection.outputStream.use { it.write(openRouterRequest.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("OpenRouter $model: HTTP $code — ${safeError(body)}")
            val content = JSONObject(body).optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            return content.ifBlank { throw IOException("OpenRouter returned no translation text.") }
        } finally {
            connection.disconnect()
        }
    }

    private fun safeError(body: String): String {
        val error = runCatching { JSONObject(body).optJSONObject("error") }.getOrNull()
        var message = error?.optString("message").orEmpty()
        if (message.isBlank()) message = "No error details from provider"
        (preferences.apiKeys + preferences.openRouterApiKey).filter { it.isNotBlank() }.forEach {
            message = message.replace(it, "[hidden]")
        }
        return message.replace(Regex("AIza[\\w-]+|sk-or-[\\w-]+"), "[hidden]")
            .replace(Regex("\\s+"), " ").take(240)
    }

    class TranslationException(message: String) : Exception(message)
}
