package com.zrok.myanmaraikeyboard.model

enum class TranslationRefresh { Keep, Clear, Update }

/**
 * After a translation is on screen, further source edits must not keep that old result.
 * A matching source stays put. An empty source drops the preview. Any other edit refreshes it.
 */
fun translationRefresh(
    followEdits: Boolean,
    source: String,
    displayed: TranslationMemory.Key?,
    tone: Tone,
    direction: TranslationDirection
): TranslationRefresh {
    if (!followEdits) return TranslationRefresh.Keep
    if (displayed != null && displayed.text == source && displayed.tone == tone && displayed.direction == direction) {
        return TranslationRefresh.Keep
    }
    if (source.isBlank()) return TranslationRefresh.Clear
    return TranslationRefresh.Update
}
