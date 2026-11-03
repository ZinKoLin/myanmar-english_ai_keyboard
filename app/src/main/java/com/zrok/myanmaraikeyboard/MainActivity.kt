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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zrok.myanmaraikeyboard.data.AppPreferences
import com.zrok.myanmaraikeyboard.ui.MyanmarAiTheme
import com.zrok.myanmaraikeyboard.ui.KeyFeedback

class MainActivity : ComponentActivity() {
    private val keyFeedback by lazy { KeyFeedback(this) }
    override fun onDestroy() { keyFeedback.release(); super.onDestroy() }
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
            var keyboardSoundEnabled by remember { mutableStateOf(preferences.keyboardSoundEnabled) }
            var keyboardVibrationEnabled by remember { mutableStateOf(preferences.keyboardVibrationEnabled) }
            var keyboardTone by remember { mutableStateOf(preferences.keyboardTone) }
            var vibrationDuration by remember { mutableStateOf(preferences.vibrationDuration.toFloat()) }
            var feedbackMessage by remember { mutableStateOf<String?>(null) }
            var saveMessage by remember { mutableStateOf<String?>(null) }
            var showAppearance by remember { mutableStateOf(false) }
            var showConnection by remember { mutableStateOf(false) }
            var showExtraKeys by remember { mutableStateOf(false) }
            var showModels by remember { mutableStateOf(false) }
            MyanmarAiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Myanmar AI Keyboard", style = MaterialTheme.typography.headlineSmall)
                    Text("သင့်စိတ်ကြိုက် ပြင်ဆင်ပါ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Keyboard ဖွင့်ရန်")
                    }
                    SettingsCard("ဘာသာစကား") {
                    Button(onClick = { startActivity(Intent(this@MainActivity, LanguageSettingsActivity::class.java)) }) {
                        Text("ဘာသာပြန် ဘာသာစကားရွေးရန်")
                    }
                    }
                    SettingsCard("Keyboard") {
                    TextButton(onClick = { showAppearance = !showAppearance }) {
                        Text("လက်ကွက်နှင့် စာလုံးအရွယ် ${if (showAppearance) "▴" else "▾"}")
                    }
                    if (showAppearance) {
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
                    }
                    HorizontalDivider()
                    Text("Key feedback / ခလုတ်တုံ့ပြန်မှု")
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ခလုတ်အသံ", modifier = Modifier.weight(1f))
                        Switch(checked = keyboardSoundEnabled, onCheckedChange = {
                            keyboardSoundEnabled = it
                            preferences.keyboardSoundEnabled = it
                        })
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("တုန်ခါမှု", modifier = Modifier.weight(1f))
                        Switch(checked = keyboardVibrationEnabled, onCheckedChange = {
                            keyboardVibrationEnabled = it
                            preferences.keyboardVibrationEnabled = it
                        })
                    }
                    if (keyboardSoundEnabled) {
                        Text("ခလုတ်အသံ ရွေးရန်")
                        listOf("Beep", "Keypad", "Double tap").forEachIndexed { index, label ->
                            TextButton(onClick = {
                                keyboardTone = index
                                preferences.keyboardTone = index
                                keyFeedback.sound(index)
                            }) { Text("${if (keyboardTone == index) "●" else "○"} $label") }
                        }
                        Text("အသံသည် Media volume အတိုင်းထွက်သည်။ Silent mode တွင် အသံပိတ်သည်။", style = MaterialTheme.typography.bodySmall)
                    }
                    if (keyboardVibrationEnabled) {
                        Text("တုန်ခါချိန်: ${vibrationDuration.toInt()} ms")
                        Slider(value = vibrationDuration, onValueChange = {
                            vibrationDuration = it
                            preferences.vibrationDuration = it.toInt()
                        }, valueRange = 15f..80f)
                        TextButton(onClick = {
                            feedbackMessage = keyFeedback.vibrate(vibrationDuration.toInt())
                                ?: "တုန်ခါမှု စမ်းသပ်ပြီးပါပြီ။ မတုန်လျှင် ဖုန်း၏ Touch vibration / DND ကို စစ်ပါ။"
                        }) { Text("တုန်ခါမှု စမ်းရန်") }
                        feedbackMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }) {
                            Text("ဖုန်း၏ Sound / vibration settings ဖွင့်ရန်")
                        }
                    }
                    }
                    SettingsCard("AI connection") {
                    Text("API keys ကို ဒီဖုန်းမှာ encrypted သိမ်းထားသည်။", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { showConnection = !showConnection }) {
                        Text("API keys ပြင်ရန် ${if (showConnection) "▴" else "▾"}")
                    }
                    if (showConnection) {
                    OutlinedTextField(apiKey1, { apiKey1 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 1") },
                        visualTransformation = PasswordVisualTransformation())
                    TextButton(onClick = { showExtraKeys = !showExtraKeys }) {
                        Text("အပို Gemini keys ${if (showExtraKeys) "▴" else "▾"}")
                    }
                    if (showExtraKeys) {
                    OutlinedTextField(apiKey2, { apiKey2 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 2 (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(apiKey3, { apiKey3 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 3 (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(apiKey4, { apiKey4 = it }, Modifier.fillMaxWidth(),
                        label = { Text("Gemini API key 4 (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    }
                    OutlinedTextField(openRouterApiKey, { openRouterApiKey = it }, Modifier.fillMaxWidth(),
                        label = { Text("OpenRouter API key (optional)") },
                        visualTransformation = PasswordVisualTransformation())
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
                    }
                    TextButton(onClick = { showModels = !showModels }) {
                        Text("အသုံးပြုမည့် models ${if (showModels) "▴" else "▾"}")
                    }
                    if (showModels) {
                        Text("Gemini", style = MaterialTheme.typography.labelLarge)
                        preferences.geminiModels.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text("OpenRouter", style = MaterialTheme.typography.labelLarge)
                        AppPreferences.OPENROUTER_MODELS.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    }
                    SettingsCard("Privacy") {
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
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
