# Burmese popup audit — 2026-11-03

Reference: user's installed Gboard Burmese on the connected Android phone.

All 38 main-layer character keys and all 38 Shift-layer keys were held individually. Raw captures and eight contact sheets are under `popup-audit/`. The capture helper is `tools/audit-popups.ps1`. Only character keys were audited; this is not a claim of parity with every Gboard feature or symbol-language layout.

Main consonant, vowel, and bottom rows expose the corresponding shifted character on hold. The top row exposes extra Myanmar characters and digits. Shift digits expose ASCII digits; held shifted character keys show the same character preview rather than a new choice list. `BurmesePopups.kt` replaces the earlier speculative alternatives with this transcription. Default outputs were also read from the editor as Unicode sequences; extra top-row choices were visually transcribed from the captures.

Emoji: offline six-category picker using system emoji glyphs, seven columns, scrollable choices, ABC return and backspace. The picker was opened and visually inspected on the phone. Automated emoji insert/delete verification was inconclusive because the editor was unavailable in that UI hierarchy; do not report it as passed.

Validation: debug APK built successfully and installed on the phone. The final popup-anchor adjustment compiled successfully; its final screenshot was blocked by the phone display turning off. No message was sent during reference testing. Test characters remain in the unsent draft.
