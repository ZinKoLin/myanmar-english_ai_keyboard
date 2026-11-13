package com.zrok.myanmaraikeyboard.data

import org.junit.Assert.*
import org.junit.Test

class OnlineWordSuggestionsTest {
    @Test fun sendsOnlyBoundedEnglishWordFragments() {
        assertTrue(OnlineWordSuggestions.eligible("archtecture"))
        assertTrue(OnlineWordSuggestions.eligible("DON'T"))
        for (text in listOf("", "a", "''", "two words", "name@example.com", "123456", "မြန်မာ", "a".repeat(33))) {
            assertFalse(text, OnlineWordSuggestions.eligible(text))
        }
    }
}
