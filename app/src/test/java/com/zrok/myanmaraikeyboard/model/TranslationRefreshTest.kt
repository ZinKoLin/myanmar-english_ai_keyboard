package com.zrok.myanmaraikeyboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationRefreshTest {
    private val direction = TranslationDirection.MYANMAR_TO_ENGLISH
    private val displayed = TranslationMemory.Key("မင်္ဂလာပါ", Tone.NATURAL, direction)

    @Test fun editsBeforeTheFirstTranslationLeaveTheDraftAlone() {
        assertEquals(TranslationRefresh.Keep, translationRefresh(false, "မင်္ဂလာပါခင်ဗျာ", displayed, Tone.NATURAL, direction))
    }

    @Test fun theSameSourceKeepsTheTranslationOnScreen() {
        assertEquals(TranslationRefresh.Keep, translationRefresh(true, "မင်္ဂလာပါ", displayed, Tone.NATURAL, direction))
    }

    @Test fun textAddedAfterTranslationRequestsANewResult() {
        assertEquals(TranslationRefresh.Update, translationRefresh(true, "မင်္ဂလာပါခင်ဗျာ", displayed, Tone.NATURAL, direction))
        assertEquals(TranslationRefresh.Update, translationRefresh(true, "မင်္ဂလာပါခင်ဗျာ", null, Tone.NATURAL, direction))
    }

    @Test fun aDifferentToneOrDirectionAlsoRequestsANewResult() {
        assertEquals(TranslationRefresh.Update, translationRefresh(true, displayed.text, displayed, Tone.FORMAL, direction))
        assertEquals(
            TranslationRefresh.Update,
            translationRefresh(true, displayed.text, displayed, Tone.NATURAL, direction.reversed())
        )
    }

    @Test fun clearingTheSourceDropsTheOldTranslation() {
        assertEquals(TranslationRefresh.Clear, translationRefresh(true, "   ", displayed, Tone.NATURAL, direction))
    }
}
