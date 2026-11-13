package com.zrok.myanmaraikeyboard.model

data class TranslationDirection(val source: TranslationLanguage, val target: TranslationLanguage) {
    init { require(source.code != target.code) { "Choose two different languages" } }
    val sourceLanguage get() = source.name
    val targetLanguage get() = target.name
    val sourceLabel get() = "မူရင်း (Auto)"
    val targetLabel get() = if (target.code == "my") "မြန်မာ preview" else "${target.badge} preview"
    val sourceHint get() = "မည်သည့်ဘာသာမဆို ရိုက်ထည့်ပါ သို့မဟုတ် paste လုပ်ပါ"
    val previewHint get() = "Your ${target.name} translation will appear here."
    val label get() = "Auto → ${target.badge} ⇄"
    fun reversed() = TranslationDirection(target, source)

    companion object {
        val MYANMAR_TO_ENGLISH = TranslationDirection(SupportedLanguages.require("my"), SupportedLanguages.require("en"))
        val ENGLISH_TO_MYANMAR = MYANMAR_TO_ENGLISH.reversed()
    }
}
