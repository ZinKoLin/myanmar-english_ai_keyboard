package com.zrok.myanmaraikeyboard.model

import org.junit.Assert.*
import org.junit.Test

class TranslationRequestPolicyTest {
    @Test fun interactiveRequestsHaveASingleWholeOperationBudget() {
        assertEquals(20_000L, TranslationRequestPolicy.budgetMs(600))
        assertEquals(40_000L, TranslationRequestPolicy.budgetMs(601))
        assertEquals(3, TranslationRequestPolicy.MAX_ATTEMPTS)
    }
    @Test fun monPrefersFullFlashAndRetainsItsWorkingModelSeparately() {
        assertEquals("gemini-3.6-flash", TranslationRequestPolicy.models(true, null).first())
        assertEquals("gemini-3.5-flash-lite", TranslationRequestPolicy.models(false, null).first())
        assertEquals("gemini-3.1-flash-lite", TranslationRequestPolicy.models(true, "gemini-3.1-flash-lite").first())
        assertFalse(TranslationRequestPolicy.models(false, "retired-model").contains("retired-model"))
    }
}
