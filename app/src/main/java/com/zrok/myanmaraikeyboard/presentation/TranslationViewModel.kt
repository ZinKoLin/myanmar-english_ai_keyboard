package com.zrok.myanmaraikeyboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zrok.myanmaraikeyboard.data.TranslationRepository
import com.zrok.myanmaraikeyboard.model.Tone
import com.zrok.myanmaraikeyboard.model.KeyboardLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TranslationUiState(
    val sourceText: String = "",
    val previewText: String = "",
    val tone: Tone = Tone.NATURAL,
    val keyboardLanguage: KeyboardLanguage = KeyboardLanguage.MYANMAR,
    val isTranslating: Boolean = false,
    val message: String? = null
)

class TranslationViewModel(private val repository: TranslationRepository) : ViewModel() {
    private val _state = MutableStateFlow(TranslationUiState())
    val state: StateFlow<TranslationUiState> = _state.asStateFlow()

    fun setSourceText(value: String) { _state.value = _state.value.copy(sourceText = value, message = null) }
    fun appendSourceText(value: String) { setSourceText(_state.value.sourceText + value) }
    fun backspaceSourceText() { setSourceText(_state.value.sourceText.dropLast(1)) }
    fun clearSourceText() { setSourceText("") }
    fun setPreviewText(value: String) { _state.value = _state.value.copy(previewText = value, message = null) }
    fun setTone(value: Tone) { _state.value = _state.value.copy(tone = value, message = null) }
    fun setKeyboardLanguage(value: KeyboardLanguage) { _state.value = _state.value.copy(keyboardLanguage = value) }

    fun translate() {
        val current = _state.value
        if (current.isTranslating) return
        viewModelScope.launch {
            _state.value = current.copy(isTranslating = true, message = null)
            repository.translate(current.sourceText, current.tone).fold(
                onSuccess = { _state.value = _state.value.copy(previewText = it, isTranslating = false, message = "Translation ready. You can edit it before inserting.") },
                onFailure = { _state.value = _state.value.copy(isTranslating = false, message = it.message ?: "Translation failed.") }
            )
        }
    }
}
