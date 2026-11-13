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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.zrok.myanmaraikeyboard.data.TranslationRepository
import com.zrok.myanmaraikeyboard.model.Tone

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
            var geminiEnabled by remember { mutableStateOf(preferences.geminiEnabled) }
            var azureEnabled by remember { mutableStateOf(preferences.azureEnabled) }
            var azureKey by remember { mutableStateOf(preferences.azureApiKey) }
            var azureRegion by remember { mutableStateOf(preferences.azureRegion) }
            var azureMessage by remember { mutableStateOf<String?>(null) }
            var showAzureKey by remember { mutableStateOf(false) }
            var keyboardRowHeight by remember { mutableStateOf(preferences.keyboardRowHeight.toFloat()) }
            var keyboardTextSize by remember { mutableStateOf(preferences.keyboardTextSize.toFloat()) }
            var keyboardSoundEnabled by remember { mutableStateOf(preferences.keyboardSoundEnabled) }
            var suggestionsEnabled by remember { mutableStateOf(preferences.suggestionsEnabled) }
            var onlineSuggestionsEnabled by remember { mutableStateOf(preferences.onlineSuggestionsEnabled) }
            var keyboardVibrationEnabled by remember { mutableStateOf(preferences.keyboardVibrationEnabled) }
            var keyboardTone by remember { mutableStateOf(preferences.keyboardTone) }
            var vibrationDuration by remember { mutableStateOf(preferences.vibrationDuration.toFloat()) }
            var feedbackMessage by remember { mutableStateOf<String?>(null) }
            var saveMessage by remember { mutableStateOf<String?>(null) }
            var showAppearance by remember { mutableStateOf(false) }
            var showConnection by remember { mutableStateOf(false) }
            var showExtraKeys by remember { mutableStateOf(false) }
            var showModels by remember { mutableStateOf(false) }
            val connectionScope = rememberCoroutineScope()
            var testingConnection by remember { mutableStateOf(false) }
            var connectionResult by remember { mutableStateOf<String?>(null) }
            MyanmarAiTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_brain_keyboard),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(androidx.compose.ui.res.stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                    }
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
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("စကားလုံး အကြံပြုချက်")
                            Text("English • System spell checker + Offline dictionary", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = suggestionsEnabled, onCheckedChange = {
                            suggestionsEnabled = it
                            preferences.suggestionsEnabled = it
                        })
                    }
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Online word suggestion")
                            Text("ရိုက်နေသည့် English word တစ်လုံးကိုသာ Datamuse သို့ ပို့ပြီး အနီးစပ်ဆုံး words ပြပေးမည်။", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = onlineSuggestionsEnabled, enabled = suggestionsEnabled, onCheckedChange = {
                            onlineSuggestionsEnabled = it
                            preferences.onlineSuggestionsEnabled = it
                        })
                    }
                    TextButton(onClick = { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.datamuse.com/api/"))) }) {
                        Text("Datamuse API နှင့် privacy အကြောင်း")
                    }
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
                        listOf("Beep", "Keypad", "Double tap", "Gboard ပုံစံ · System keyboard click").forEachIndexed { index, label ->
                            TextButton(onClick = {
                                keyboardTone = index
                                preferences.keyboardTone = index
                                keyFeedback.sound(index, volume = preferences.keyboardSoundVolume)
                            }) { Text("${if (keyboardTone == index) "●" else "○"} $label") }
                        }
                        var soundVolume by remember { mutableStateOf(preferences.keyboardSoundVolume) }
                        Text("ခလုတ်အသံအကျယ်: ${(soundVolume * 100).toInt()}%")
                        Slider(value = soundVolume, onValueChange = { soundVolume = it; preferences.keyboardSoundVolume = it },
                            onValueChangeFinished = { keyFeedback.sound(keyboardTone, volume = soundVolume) })
                        Text("System keyboard click တွင် ဖုန်းစနစ်၏ ခလုတ်အသံကို သုံးသည်။ Silent mode တွင် အသံပိတ်သည်။", style = MaterialTheme.typography.bodySmall)
                    }
                    if (keyboardVibrationEnabled) {
                        var systemHaptic by remember { mutableStateOf(preferences.systemKeyboardHaptic) }
                        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("System keyboard haptic", modifier = Modifier.weight(1f))
                            Switch(checked = systemHaptic, onCheckedChange = { systemHaptic = it; preferences.systemKeyboardHaptic = it })
                        }
                        if (!systemHaptic) {
                        Text("တုန်ခါအား: ${vibrationDuration.toInt()}")
                        Slider(value = vibrationDuration, onValueChange = {
                            vibrationDuration = it
                            preferences.vibrationDuration = it.toInt()
                        }, valueRange = 15f..80f)
                        }
                        TextButton(onClick = {
                            feedbackMessage = keyFeedback.vibrate(vibrationDuration.toInt(), window.decorView, systemHaptic)
                                ?: "တုန်ခါမှု စမ်းသပ်ပြီးပါပြီ။ မတုန်လျှင် ဖုန်း၏ Touch vibration / DND ကို စစ်ပါ။"
                        }) { Text("တုန်ခါမှု စမ်းရန်") }
                        feedbackMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }) {
                            Text("ဖုန်း၏ Sound / vibration settings ဖွင့်ရန်")
                        }
                    }
                    }
                    SettingsCard("Azure Translator") {
                        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Azure အသုံးပြုရန်", modifier = Modifier.weight(1f))
                            Switch(checked = azureEnabled, onCheckedChange = {
                                azureEnabled = it
                                preferences.azureEnabled = it
                                connectionResult = null
                            })
                        }
                        Text("မြန်မာ ↔ English • F0 Free tier: တစ်လ စာလုံးရေ ၂ သန်းအထိ။", style = MaterialTheme.typography.bodySmall)
                        Text("Azure mode တွင် ပုံမှန်ဘာသာပြန်သာ သုံးသည်။ Natural / Formal / Casual tone မသက်ရောက်ပါ။", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { showAzureKey = !showAzureKey }) { Text("Azure Key / Region ပြင်ရန် ${if (showAzureKey) "▴" else "▾"}") }
                        if (showAzureKey) {
                            OutlinedTextField(azureKey, { azureKey = it }, Modifier.fillMaxWidth(),
                                label = { Text("Azure Translator key") }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation())
                            OutlinedTextField(azureRegion, { azureRegion = it }, Modifier.fillMaxWidth(),
                                label = { Text("Region (ဥပမာ southeastasia / global)") }, singleLine = true)
                            Text("Azure portal → Translator → Keys and Endpoint မှ ကူးထည့်ပါ။", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                preferences.azureApiKey = azureKey
                                preferences.azureRegion = azureRegion
                                azureMessage = if (azureKey.isBlank()) "Azure key ထည့်ပါ။" else "Azure connection saved."
                            }) { Text("Save Azure connection") }
                            azureMessage?.let { Text(it) }
                        }
                        TextButton(onClick = {
                            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://portal.azure.com/#create/Microsoft.CognitiveServicesTextTranslation")))
                        }) { Text("Azure Translator ဖန်တီးရန် (F0 ရွေးပါ)") }
                        Button(enabled = !testingConnection && azureEnabled, onClick = {
                            testingConnection = true
                            connectionResult = "Testing Azure…"
                            connectionScope.launch {
                                try {
                                    connectionResult = TranslationRepository(preferences).translate("Hello", Tone.NATURAL,
                                        com.zrok.myanmaraikeyboard.model.TranslationDirection.ENGLISH_TO_MYANMAR).fold(
                                        onSuccess = { "Azure OK: $it" }, onFailure = { it.message ?: "Azure failed" })
                                } finally { testingConnection = false }
                            }
                        }) { Text(if (testingConnection) "Testing…" else "Test Azure connection") }
                        if (azureEnabled) connectionResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    SettingsCard("Gemini connection") {
                    Text("မွန်ဘာသာပြန်: MonDictDB အဘိဓာန်အချက်အလက် ၁၉,၀၈၀ ခုကို အထောက်အကူပြု သုံးထားသည်။ အဓိပ္ပာယ်တစ်မျိုးတည်းရှိသော စကားလုံးအတိအကျကို ဖုန်းထဲကနေ ပြန်ပေးပြီး စာကြောင်းများကို AI ဖြင့် ပြန်သည်။ မွန်ဘာသာတတ်သူဖြင့် ရလဒ်စစ်ဆေးပါ။", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Barnista/MonDictDB"))) }) {
                        Text("မွန်အဘိဓာန်အရင်းအမြစ်နှင့် license ↗")
                    }
                    Text("Gemini key ရယူရန်", style = MaterialTheme.typography.titleSmall)
                    Text("၁။ Google AI Studio ကိုဖွင့်ပြီး Google account ဖြင့် ဝင်ပါ။\n၂။ API Keys စာမျက်နှာမှာ Create API key နှိပ်ပြီး project ရွေးပါ သို့မဟုတ် အသစ်ဖန်တီးပါ။\n၃။ Key ကို Copy ကူးပြီး အောက်က Gemini API key 1 မှာ ထည့်ပါ။\n၄။ Save connection နှိပ်ပြီး Test saved connection ဖြင့် စမ်းပါ။", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://aistudio.google.com/api-keys"))) }) {
                        Text("Google AI Studio မှ API key ရယူရန် ↗")
                    }
                    TextButton(onClick = { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ai.google.dev/gemini-api/docs/api-key"))) }) {
                        Text("Google ၏ API key လမ်းညွှန် ↗")
                    }
                    Text(if (azureEnabled) "Azure mode • Gemini သုံးရန် Azure switch ကို Off လုပ်ပါ။" else "Gemini mode • Azure ကို နောက်မှ ဖွင့်စမ်းနိုင်သည်။", style = MaterialTheme.typography.bodySmall)
                    if (!AppPreferences.GEMINI_ONLY) {
                    Text(if (azureEnabled) "Azure mode သုံးနေသည်။ Gemini / OpenRouter ပြန်သုံးရန် Azure ကို Off လုပ်ပါ။" else "Gemini / OpenRouter mode", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Gemini အသုံးပြုရန်")
                            Text(if (geminiEnabled) "Gemini → OpenRouter" else "OpenRouter only", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = geminiEnabled, onCheckedChange = {
                            geminiEnabled = it
                            preferences.geminiEnabled = it
                        })
                    }
                    }
                    Button(enabled = !testingConnection && !azureEnabled, onClick = {
                        testingConnection = true
                        connectionResult = "Testing…"
                        connectionScope.launch {
                            try {
                                connectionResult = TranslationRepository(preferences).translate("Hello", Tone.NATURAL).fold(
                                    onSuccess = { "Connection OK: $it" },
                                    onFailure = { it.message ?: "Connection failed" }
                                )
                            } finally { testingConnection = false }
                        }
                    }) { Text(if (testingConnection) "Testing…" else "Test saved connection") }
                    connectionResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
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
                    if (!AppPreferences.GEMINI_ONLY) {
                    OutlinedTextField(openRouterApiKey, { openRouterApiKey = it }, Modifier.fillMaxWidth(),
                        label = { Text("OpenRouter API key (optional)") },
                        visualTransformation = PasswordVisualTransformation())
                    }
                    Button(onClick = {
                        preferences.endpoint = AppPreferences.DEFAULT_ENDPOINT
                        preferences.apiKeys = listOf(apiKey1, apiKey2, apiKey3, apiKey4)
                        saveMessage = if (preferences.apiKeys.isEmpty()) {
                            "Please enter a Gemini API key first."
                        } else {
                            "Connection saved. Request တစ်ခုလုံး ၂၀–၄၀ စက္ကန့်အတွင်း၊ အများဆုံး ၃ ကြိမ်သာ စမ်းမည်။"
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
                            Text("Text is sent for translation when you tap Translate or edit an active translation.")
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
