package com.zrok.myanmaraikeyboard.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.zrok.myanmaraikeyboard.model.SupportedLanguages
import com.zrok.myanmaraikeyboard.model.TranslationDirection

class AppPreferences(context: Context) {
    internal val assets = context.applicationContext.assets
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "private_settings",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var endpoint: String
        get() = preferences.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT).orEmpty()
        set(value) = preferences.edit().putString(KEY_ENDPOINT, value.trim()).apply()

    var apiKey: String
        get() = preferences.getString(KEY_API_KEY, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_API_KEY, value).apply()

    /** Ordered Gemini keys. Values are encrypted by EncryptedSharedPreferences. */
    var apiKeys: List<String>
        get() {
            val stored = preferences.getString(KEY_API_KEYS, "").orEmpty()
                .split('\n').map { it.trim() }.filter { it.isNotBlank() }
            return if (stored.isNotEmpty()) stored.take(MAX_API_KEYS)
            else apiKey.trim().takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
        }
        set(value) {
            val keys = value.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(MAX_API_KEYS)
            preferences.edit()
                .putString(KEY_API_KEYS, keys.joinToString("\n"))
                .putString(KEY_API_KEY, keys.firstOrNull().orEmpty())
                .apply()
        }

    var openRouterApiKey: String
        get() = preferences.getString(KEY_OPENROUTER_API_KEY, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_OPENROUTER_API_KEY, value.trim()).apply()

    var networkTranslationEnabled: Boolean
        get() = preferences.getBoolean(KEY_NETWORK_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_NETWORK_ENABLED, value).apply()

    var geminiEnabled: Boolean
        get() = preferences.getBoolean("gemini_enabled", false)
        set(value) = preferences.edit().putBoolean("gemini_enabled", value).apply()

    var azureEnabled: Boolean
        get() = preferences.getBoolean("azure_enabled", false)
        set(value) = preferences.edit().putBoolean("azure_enabled", value).apply()

    var azureApiKey: String
        get() = preferences.getString("azure_api_key", "").orEmpty()
        set(value) = preferences.edit().putString("azure_api_key", value.trim()).apply()

    var azureRegion: String
        get() = preferences.getString("azure_region", "global").orEmpty()
        set(value) = preferences.edit().putString("azure_region", value.trim().lowercase(java.util.Locale.ROOT)).apply()

    var keyboardRowHeight: Int
        get() = preferences.getInt(KEY_KEYBOARD_ROW_HEIGHT, 50).coerceIn(48, 64)
        set(value) = preferences.edit().putInt(KEY_KEYBOARD_ROW_HEIGHT, value.coerceIn(48, 64)).apply()

    var previewHeight: Int
        get() = preferences.getInt("preview_height", 210).coerceIn(100, 480)
        set(value) = preferences.edit().putInt("preview_height", value.coerceIn(100, 480)).apply()

    var keyboardTextSize: Int
        get() = preferences.getInt(KEY_KEYBOARD_TEXT_SIZE, 20).coerceIn(16, 24)
        set(value) = preferences.edit().putInt(KEY_KEYBOARD_TEXT_SIZE, value.coerceIn(16, 24)).apply()

    var keyboardSoundEnabled: Boolean
        get() = preferences.getBoolean(KEY_KEYBOARD_SOUND, true)
        set(value) = preferences.edit().putBoolean(KEY_KEYBOARD_SOUND, value).apply()

    var suggestionsEnabled: Boolean
        get() = preferences.getBoolean("suggestions_enabled", true)
        set(value) = preferences.edit().putBoolean("suggestions_enabled", value).apply()

    var onlineSuggestionsEnabled: Boolean
        get() = preferences.getBoolean("online_suggestions_enabled", true)
        set(value) = preferences.edit().putBoolean("online_suggestions_enabled", value).apply()

    var keyboardTone: Int
        get() = preferences.getInt("keyboard_tone", 3).coerceIn(0, 3)
        set(value) = preferences.edit().putInt("keyboard_tone", value.coerceIn(0, 3)).apply()

    var keyboardSoundVolume: Float
        get() = preferences.getFloat("keyboard_sound_volume", 0.5f).coerceIn(0f, 1f)
        set(value) = preferences.edit().putFloat("keyboard_sound_volume", value.coerceIn(0f, 1f)).apply()

    var systemKeyboardHaptic: Boolean
        get() = preferences.getBoolean("system_keyboard_haptic", true)
        set(value) = preferences.edit().putBoolean("system_keyboard_haptic", value).apply()

    var vibrationDuration: Int
        get() = preferences.getInt("vibration_duration", 35).coerceIn(15, 80)
        set(value) = preferences.edit().putInt("vibration_duration", value.coerceIn(15, 80)).apply()

    var keyboardVibrationEnabled: Boolean
        get() = preferences.getBoolean(KEY_KEYBOARD_VIBRATION, true)
        set(value) = preferences.edit().putBoolean(KEY_KEYBOARD_VIBRATION, value).apply()

    val geminiModels: List<String>
        get() = DEFAULT_GEMINI_MODELS

    fun endpointForModel(model: String): String {
        val current = endpoint
        val marker = "/models/"
        val markerStart = current.indexOf(marker)
        val actionStart = current.indexOf(":generateContent", markerStart + marker.length)
        return if (markerStart >= 0 && actionStart > markerStart) {
            current.substring(0, markerStart + marker.length) + model + current.substring(actionStart)
        } else current
    }

    val enabledLanguageCodes: Set<String>
        get() = preferences.getStringSet("translation_languages", setOf("my", "en"))
            .orEmpty().filter { SupportedLanguages.find(it) != null }.toSet().let {
                if (it.size >= 2) it else setOf("my", "en")
            }

    val translationDirection: TranslationDirection
        get() {
            val codes = enabledLanguageCodes.sorted()
            val source = preferences.getString("source_language", "my").orEmpty()
                .takeIf { it in codes } ?: codes.first()
            val target = preferences.getString("target_language", "en").orEmpty()
                .takeIf { it in codes && it != source } ?: codes.first { it != source }
            return TranslationDirection(SupportedLanguages.require(source), SupportedLanguages.require(target))
        }

    fun saveTranslationLanguages(codes: Set<String>, source: String, target: String) {
        require(codes.size >= 2 && codes.all { SupportedLanguages.find(it) != null })
        require(source in codes && target in codes && source != target)
        preferences.edit()
            .putStringSet("translation_languages", codes.toSet())
            .putString("source_language", source)
            .putString("target_language", target)
            .apply()
    }

    companion object {
        const val GEMINI_ONLY = true
        const val DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_KEYS = "api_keys"
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_NETWORK_ENABLED = "network_enabled"
        private const val KEY_KEYBOARD_ROW_HEIGHT = "keyboard_row_height"
        private const val KEY_KEYBOARD_TEXT_SIZE = "keyboard_text_size"
        private const val KEY_KEYBOARD_SOUND = "keyboard_sound"
        private const val KEY_KEYBOARD_VIBRATION = "keyboard_vibration"
        const val MAX_API_KEYS = 4
        const val OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        val OPENROUTER_MODELS = listOf(
            "google/gemma-4-31b-it:free",
            "deepseek/deepseek-v4-flash:free",
            "qwen/qwen-2-7b-instruct:free"
        )
        val DEFAULT_GEMINI_MODELS = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.6-flash"
        )
    }
}
