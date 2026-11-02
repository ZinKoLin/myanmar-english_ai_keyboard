package com.zrok.myanmaraikeyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zrok.myanmaraikeyboard.model.Tone
import com.zrok.myanmaraikeyboard.model.KeyboardLanguage
import com.zrok.myanmaraikeyboard.presentation.TranslationUiState

@Composable
fun TranslationPanel(
    state: TranslationUiState,
    onSourceChanged: (String) -> Unit,
    onKeyPress: (String) -> Unit,
    onPreviewChanged: (String) -> Unit,
    onToneChanged: (Tone) -> Unit,
    onLanguageChanged: (KeyboardLanguage) -> Unit,
    onTranslate: () -> Unit,
    onInsert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Myanmar → English", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.sourceText,
            onValueChange = onSourceChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
            label = { Text("Burmese text") },
            placeholder = { Text("မြန်မာလို ရိုက်ထည့်ပါ") },
            minLines = 3
        )
        KeyboardGrid(
            language = state.keyboardLanguage,
            onLanguageChanged = onLanguageChanged,
            onKeyPress = onKeyPress
        )
        Text("Tone", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tone.entries.forEach { tone ->
                FilterChip(
                    selected = state.tone == tone,
                    onClick = { onToneChanged(tone) },
                    label = { Text(tone.label) },
                    modifier = Modifier.semantics { contentDescription = "${tone.label} tone" }
                )
            }
        }
        Button(
            onClick = onTranslate,
            enabled = !state.isTranslating && state.sourceText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isTranslating) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp))
            Text(if (state.isTranslating) "Translating…" else "Translate")
        }
        OutlinedTextField(
            value = state.previewText,
            onValueChange = onPreviewChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            label = { Text("Editable English preview") },
            minLines = 4
        )
        Button(
            onClick = onInsert,
            enabled = state.previewText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Insert into active field") }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun KeyboardGrid(
    language: KeyboardLanguage,
    onLanguageChanged: (KeyboardLanguage) -> Unit,
    onKeyPress: (String) -> Unit
) {
    val rows = if (language == KeyboardLanguage.MYANMAR) listOf(
        listOf("၁", "၂", "၃", "၄", "၅", "၆", "၇", "၈", "၉", "၀"),
        listOf("ဆ", "တ", "န", "မ", "အ", "ပ", "က", "င", "သ", "စ", "ဟ"),
        listOf("ေ", "ျ", "ိ", "်", "ါ", "့", "ြ", "ု", "ူ", "း"),
        listOf("ဖ", "ထ", "ခ", "လ", "ဘ", "ည", "ာ", "၊", "။", "?")
    ) else listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m", ".", ",", "?")
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = language == KeyboardLanguage.MYANMAR,
                onClick = { onLanguageChanged(KeyboardLanguage.MYANMAR) },
                label = { Text("မြန်မာ") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = language == KeyboardLanguage.ENGLISH,
                onClick = { onLanguageChanged(KeyboardLanguage.ENGLISH) },
                label = { Text("English") },
                modifier = Modifier.weight(1f)
            )
        }
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { key ->
                    androidx.compose.material3.OutlinedButton(
                        onClick = { onKeyPress(key) },
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) { Text(key) }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = { onKeyPress(" ") }, modifier = Modifier.weight(2f)) { Text("Space") }
            androidx.compose.material3.OutlinedButton(onClick = { onKeyPress("BACKSPACE") }, modifier = Modifier.weight(1f)) { Text("⌫") }
            androidx.compose.material3.OutlinedButton(onClick = { onKeyPress("CLEAR") }, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
    }
}
