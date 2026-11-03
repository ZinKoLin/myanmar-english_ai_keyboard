package com.zrok.myanmaraikeyboard.model

import org.junit.Assert.*
import org.junit.Test

class WordSuggestionsTest {
    @Test fun completesOnlyCurrentWord() {
        val result = WordSuggestions.suggest("say hel", "", false)
        assertEquals("hel", result.prefix)
        assertTrue("hello" in result.words)
        assertTrue(result.words.size <= 3)
    }
    @Test fun keepsMyanmarCombiningMarks() {
        val result = WordSuggestions.suggest("မင်္ဂ", "", true)
        assertEquals("မင်္ဂ", result.prefix)
        assertTrue("မင်္ဂလာပါ" in result.words)
    }
    @Test fun avoidsReplacingMiddleOfWord() {
        assertTrue(WordSuggestions.suggest("hel", "p", false).words.isEmpty())
    }
    @Test fun preservesEnglishCapitalization() {
        assertTrue("Hello" in WordSuggestions.suggest("Hel", "", false).words)
        assertTrue("HELLO" in WordSuggestions.suggest("HEL", "", false).words)
    }
}
