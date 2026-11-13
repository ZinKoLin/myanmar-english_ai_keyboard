package com.zrok.myanmaraikeyboard.model

import org.junit.Assert.*
import org.junit.Test

class MonLexiconTest {
    private val dictionary = MonLexicon(sequenceOf(
        "ကကဠာ\tငါးသေတ္တာ။", "က\tငါး။", "မသုန်\tငါး။", "ဍာ်\tရေ။",
        "ဘာ\tစာသင်ကျောင်း။", "ကျောင်း\tကျောင်း။"
    ))
    @Test fun onlyUnambiguousDictionaryTermsReturnWithoutAI() {
        assertEquals("ကကဠာ", dictionary.exactBurmese(" ငါးသေတ္တာ။ "))
        assertNull(dictionary.exactBurmese("ငါး"))
        assertNull(dictionary.exactBurmese("ငါးသေတ္တာ ဝယ်မယ်"))
    }
    @Test fun referencesPreferWholeCompoundsAndNeverSubstituteASentence() {
        val entries = dictionary.glossary("စာသင်ကျောင်းကို သွားမယ်")
        assertEquals(listOf("ဘာ"), entries.map { it.mon })
        assertNull(dictionary.exactBurmese("စာသင်ကျောင်းကို သွားမယ်"))
    }
    @Test fun shortTermsCannotCutACombiningSequence() {
        assertTrue(dictionary.glossary("ရေးမယ်").none { it.mon == "ဍာ်" })
        assertTrue(dictionary.glossary("ရေသောက်မယ်").any { it.mon == "ဍာ်" })
    }
}
