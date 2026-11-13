package com.zrok.myanmaraikeyboard.model

import org.junit.Assert.*
import org.junit.Test

class WordSuggestionsTest {
    @Test fun ranksPopularOnlineCompletionsAndPreservesCapitalization() {
        val local = WordSuggestions.suggest("HEL", "", false)
        val merged = WordSuggestions.mergeOnline(local, listOf("help", "hello", "help"))
        assertEquals(listOf("HELP", "HELLO"), merged.words.take(2))
        assertEquals(merged.words.distinct(), merged.words)
    }
    @Test fun keepsCloserOfflineCorrectionAndRejectsUntrustedWords() {
        val local = WordSuggestions.suggest("recieve", "", false)
        val merged = WordSuggestions.mergeOnline(local, listOf("recieve", "welcome", "<script>", "hello world"))
        assertEquals("receive", merged.words.first())
        assertFalse(merged.words.contains("recieve"))
        assertFalse(merged.words.contains("<script>"))
        assertFalse(merged.words.contains("hello world"))
    }
    @Test fun retainsKnownWordAndFallsBackWhenOnlineIsEmpty() {
        val local = WordSuggestions.suggest("hello", "", false)
        assertEquals(local, WordSuggestions.mergeOnline(local, emptyList()))
        val merged = WordSuggestions.mergeOnline(local, listOf("help", "hello", "helloworldunknown"))
        assertTrue(merged.isKnownWord)
        assertEquals("hello", merged.words.first())
    }
    @Test fun suggestsSpellingCorrectionsAndTranspositions() {
        assertTrue("hello" in WordSuggestions.suggest("helo", "", false).words)
        assertTrue("receive" in WordSuggestions.suggest("recieve", "", false).words)
        assertTrue("vocabulary" in WordSuggestions.suggest("vocabluary", "", false).words)
    }
    @Test fun identifiesKnownWordsWithoutFlaggingUnknownNamesAsErrors() {
        val known = WordSuggestions.suggest("vocabulary", "", false)
        assertTrue(known.isKnownWord)
        assertEquals("vocabulary", known.words.first())
        assertFalse(WordSuggestions.suggest("Zrok", "", false).isKnownWord)
    }
    @Test fun ignoresOtherScriptsInEnglishMode() {
        assertTrue(WordSuggestions.suggest("မင်္ဂလာပါ", "", false).words.isEmpty())
    }
    @Test fun completesOnlyCurrentWord() {
        val result = WordSuggestions.suggest("say hel", "", false)
        assertEquals("hel", result.prefix)
        assertTrue("hello" in result.words)
        assertTrue(result.words.size <= 3)
    }
    @Test fun disablesMyanmarSuggestions() {
        for (before in listOf("", "မင်္ဂ", "မင်္ဂလာပါ ", "hel")) {
            assertTrue(WordSuggestions.suggest(before, "", true).words.isEmpty())
        }
    }
    @Test fun avoidsReplacingMiddleOfWord() {
        assertTrue(WordSuggestions.suggest("hel", "p", false).words.isEmpty())
    }
    @Test fun preservesEnglishCapitalization() {
        assertTrue("Hello" in WordSuggestions.suggest("Hel", "", false).words)
        assertTrue("HELLO" in WordSuggestions.suggest("HEL", "", false).words)
    }
}
