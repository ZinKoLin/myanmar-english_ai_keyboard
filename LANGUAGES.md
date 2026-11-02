# Translation language settings

The catalog contains 110 text-language choices, transcribed from Google's general-use Gemini language list on 2026-11-03. Chinese Simplified and Traditional are separate choices. Google's legacy Hebrew code `iw` is represented by the modern locale alias `he`.

Official reference: https://firebase.google.com/docs/ai-logic/models#supported-languages

This is a dated catalog snapshot, not a live discovery API or a guarantee of equal translation quality across languages and configured model versions. Speech and image-generation language lists are intentionally not used.

Open the companion app's Translation languages button or the keyboard's settings gear. Search by English name, native name, or language code. Choose at least two languages and different From/To languages, then Save. Cancel does not write preferences. Selections survive rotation and process recreation through activity saved state until explicitly saved.

The keyboard reloads a changed default pair when its input view next opens. Drafts from the previous pair are kept in session memory when changing the configured pair. Translation memory is keyed by both languages and tone. Swapping direction exchanges existing text without requesting a translation.

Built-in typing layouts remain Myanmar and English. Other scripts can be pasted into the source editor; hold the EN / Myanmar key to open Android's keyboard picker. New text sent to Gemini still requires tapping Translate. Model and API-key settings are unchanged.

Verification: 8 local JVM tests pass (catalog identity, direction reversal, same-language rejection, cross-language cache isolation and existing translation-memory tests). Debug APK builds and installs successfully. The native language settings screen was visually inspected on the connected device; all 110 language pairs were not individually sent to the API.
