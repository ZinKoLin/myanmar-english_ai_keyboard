package com.zrok.myanmaraikeyboard.model

import org.junit.Assert.*
import org.junit.Test

class TranslationMemoryTest {
    private val key = TranslationMemory.Key("မင်္ဂလာပါ", Tone.NATURAL, TranslationDirection.MYANMAR_TO_ENGLISH)

    @Test fun repeatedAndReverseRequestsUseTheExistingPair() {
        val memory = TranslationMemory()
        memory.remember(key, "Hello")
        assertEquals("Hello", memory.find(key))
        assertEquals("မင်္ဂလာပါ", memory.find(
            TranslationMemory.Key("Hello", Tone.NATURAL, TranslationDirection.ENGLISH_TO_MYANMAR)
        ))
    }

    @Test fun changedTextOrToneRequiresANewTranslation() {
        val memory = TranslationMemory()
        memory.remember(key, "Hello")
        assertNull(memory.find(key.copy(text = "ကျေးဇူးတင်ပါတယ်")))
        assertNull(memory.find(key.copy(tone = Tone.FORMAL)))
    }

    @Test fun editedTranslationIsKeptWhenSwappingDirections() {
        val memory = TranslationMemory()
        memory.remember(key, "Hello")
        memory.remember(key, "Hi there")
        assertEquals("Hi there", memory.find(key))
        assertEquals(key.text, memory.find(key.copy(text = "Hi there", direction = key.direction.reversed())))
    }

    @Test fun clearRemovesBothDirectionsAndStorageIsBounded() {
        val memory = TranslationMemory(2)
        memory.remember(key, "Hello")
        memory.remember(key.copy(text = "ကျေးဇူး"), "Thanks")
        assertNull(memory.find(key))
        memory.clear()
        assertNull(memory.find(key.copy(text = "ကျေးဇူး")))
    }
}
