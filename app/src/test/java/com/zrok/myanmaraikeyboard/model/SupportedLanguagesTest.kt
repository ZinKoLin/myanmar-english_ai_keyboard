package com.zrok.myanmaraikeyboard.model

import org.junit.Assert.*
import org.junit.Test

class SupportedLanguagesTest {
    @Test fun catalogHasUniqueCodesAndIncludesPreviouslySupportedPair() {
        val codes = SupportedLanguages.all.map { it.code }
        assertTrue(codes.size > 100)
        assertEquals(codes.size, codes.distinct().size)
        assertTrue(codes.containsAll(listOf("my", "en", "th", "ja", "ar", "zh-Hans", "zh-Hant", "mnw", "shn", "ksw", "kac")))
    }

    @Test fun arbitraryPairsReverseWithoutLosingTheirLanguageIdentity() {
        val direction = TranslationDirection(SupportedLanguages.require("th"), SupportedLanguages.require("ja"))
        assertEquals("Thai", direction.sourceLanguage)
        assertEquals("Japanese", direction.targetLanguage)
        assertEquals(direction, direction.reversed().reversed())
        assertTrue(direction.label.contains("Auto"))
        assertTrue(direction.label.contains("JA"))
        assertTrue(direction.reversed().label.contains("TH"))
        assertEquals(direction.sourceLabel, direction.reversed().sourceLabel)
        assertEquals(direction.sourceHint, direction.reversed().sourceHint)
    }

    @Test fun translationMemorySeparatesLanguagePairs() {
        val memory = TranslationMemory()
        val thai = TranslationDirection(SupportedLanguages.require("en"), SupportedLanguages.require("th"))
        val japanese = TranslationDirection(SupportedLanguages.require("en"), SupportedLanguages.require("ja"))
        val key = TranslationMemory.Key("Hello", Tone.NATURAL, thai)
        memory.remember(key, "สวัสดี")
        assertNull(memory.find(key.copy(direction = japanese)))
        assertNull(memory.find(key.copy(text = "สวัสดี", direction = thai.reversed())))
    }

    @Test(expected = IllegalArgumentException::class)
    fun identicalLanguagesAreRejected() {
        TranslationDirection(SupportedLanguages.require("en"), SupportedLanguages.require("en"))
    }
}
