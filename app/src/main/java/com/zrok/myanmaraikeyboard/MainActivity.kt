package com.zrok.myanmaraikeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zrok.myanmaraikeyboard.data.AppPreferences
import com.zrok.myanmaraikeyboard.ui.MyanmarAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = AppPreferences(applicationContext)
        setContent {
            val savedKeys = preferences.apiKeys
            var apiKey1 by remember { mutableStateOf(savedKeys.getOrNull(0).orEmpty()) }
            var apiKey2 by remember { mutableStateOf(savedKeys.getOrNull(1).orEmpty()) }
            var apiKey3 by remember { mutableStateOf(savedKeys.getOrNull(2).orEmpty()) }
            var apiKey4 by remember { mutableStateOf(savedKeys.getOrNull(3).orEmpty()) }
            var openRouterApiKey by remember { mutableStateOf(preferences.openRouterApiKey) }
            var networkEnabled by remember { mutableStateOf(preferences.networkTranslationEnabled) }
            var keyboardRowHeight by remember { mutableStateOf(preferences.keyboardRowHeight.toFloat()) }
            var keyboardTextSize by remember { mutableStateOf(preferences.keyboardTextSize.toFloat()) }
            var saveMessage by remember { mutableStateOf<String?>(null) }
            MyanmarAiTheme {
                Column(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Myanmar AI Keyboard")
                    Button(onClick = { startActivity(Intent(this@MainActivity, LanguageSettingsActivity::class.java)) }) {
                        Text("Translation languages / ဘာသာစကားများ")
                    }
                    HorizontalDivider()
                    Text("Keyboard appearance / လက်ကွက်အရွယ်")
                    Text("Key height: ${keyboardRowHeight.toInt()} dp")
                    Slider(
                        value = keyboardRowHeight,
                        onValueChange = {
                            keyboardRowHeight = it
                            preferences.keyboardRowHeight = it.toInt()
                        },
                        valueRange = 48f..64f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Key text: ${keyboardTextSize.toInt()} sp")
                    Slider(
                        value = keyboardTextSize,
                        onValueChange = {
                            keyboardTextSize = it
                            preferences.keyboardTextSize = it.toInt()
                        },
                        valueRange = 16f..24f,
                        steps = 7,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Changes apply the next time the keyboard opens.")
                    Text("Use your own Gemini API key. It is stored encrypted on this device and is never bundled with the app.")
                    Text("Gemini endpoint is managed automatically for security and compatibility.")
                    Text("Gemini API keys (up to 4, tried in order; stored encrypted)")
                    Text("Model fallback: ${preferences.geminiModels.joinToString(" → ")}")
                    OutlinedTextField(apiKey1, { apiKey1 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 1") },
                        visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(apiKey2, { apiKey2 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 2 (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(apiKey3, { apiKey3 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 3 (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(apiKey4, { apiKey4 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 4 (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(openRouterApiKey, { openRouterApiKey = it }, Modifier.fillMaxWidth(),
                        label = { Text("OpenRouter API key (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    Text("Optional fallback: ${AppPreferences.OPENROUTER_MODELS.joinToString(" → ")}")
                    Button(onClick = {
                        preferences.endpoint = AppPreferences.DEFAULT_ENDPOINT
                        preferences.apiKeys = listOf(apiKey1, apiKey2, apiKey3, apiKey4)
                        preferences.openRouterApiKey = openRouterApiKey
                        saveMessage = if (preferences.apiKeys.isEmpty()) {
                            "Please enter at least one Gemini API key first."
                        } else {
                            "Connection saved. Keys will fail over on 429/401/403."
                        }
                    }) { Text("Save connection") }
                    saveMessage?.let { Text(it) }
                    HorizontalDivider()
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Allow network translation")
                            Text("Text is sent only after you tap Translate.")
                        }
                        Switch(checked = networkEnabled, onCheckedChange = {
                            networkEnabled = it
                            preferences.networkTranslationEnabled = it
                        })
                    }
                    Button(onClick = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }) {
                        Text("Enable keyboard")
                    }
                }
            }
        }
    }
}
