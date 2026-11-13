# Translation language settings

The catalog contains 114 choices: the 110 text-language choices transcribed from Google's general-use Gemini language list on 2026-11-03, plus four explicitly experimental choices: Mon (`mnw`), Shan (`shn`), S'gaw Karen (`ksw`) and Jinghpaw (`kac`). Chinese Simplified and Traditional are separate choices. Google's legacy Hebrew code `iw` is represented by the modern locale alias `he`. The four additions are not a claim of inclusion in that Gemini support list.

Official reference: https://firebase.google.com/docs/ai-logic/models#supported-languages

This is a dated catalog snapshot, not a live discovery API or a guarantee of equal translation quality across languages and configured model versions. Speech and image-generation language lists are intentionally not used.

Open the companion app's Translation languages button or the keyboard's settings gear. Search by English name, native name, or language code. Choose at least two languages and different From/To languages, then Save. Cancel does not write preferences. Selections survive rotation and process recreation through activity saved state until explicitly saved.

The keyboard reloads a changed default pair when its input view next opens. Drafts from the previous pair are kept in session memory when changing the configured pair. Clear removes these drafts. Translation memory is keyed by text, language pair and tone. Swapping direction moves the previous preview into the source, clears the preview and requires tapping Translate again. The source label remains Auto because input language is detected, not forced by the configured pair.

Built-in typing layouts remain Myanmar and English. Other scripts can be pasted into the source editor; hold the globe key to open Android's keyboard picker. Translate starts a translation session; subsequent source edits can trigger a debounced update. Clear cancels pending work. See README for request budgets and provider selection.

Mon uses a bundled 19,080-entry human-attributed MonDictDB lexicon for unambiguous exact lookups and glossary-assisted Gemini prompts. It is not a dedicated sentence-translation model. Mon output has retained Burmese in testing, so native-speaker review is required. Azure does not support these experimental target choices in this app.

Verification: the release JVM suite has 30 passing tests across the project. The native language chooser was visually inspected on-device; all 114 choices were not individually sent to the API and translation quality is not certified.
