# English word suggestions

The keyboard combines three sources:

- The bundled Wordnik vocabulary provides immediate offline completions and edit-distance corrections.
- Android's `SpellCheckerSession` requests spelling validation and any available alternatives from the selected system checker. On the tested phone this is Gboard's `com.android.inputmethod.latin.spellcheck.AndroidSpellCheckerService`. Its word-level API identified a typo but returned no alternatives; some sessions did not respond. Offline and online sources therefore remain independent of this service. This integrates Google's spelling service, not Gboard's full prediction model or touch decoder.
- When **Online word suggestion** is enabled and the active network is validated, Datamuse's `/sug` endpoint supplies popularity-ranked completions and approximate spelling matches. Returned words are checked against the local vocabulary, deduplicated, and merged with closer local corrections. Up to six suggestions can be scrolled horizontally.

System checks start after 150 ms without a text change. Online lookup starts after 450 ms. New text, cursor changes, language changes, or hiding the keyboard invalidate pending results. Suggestions are inserted only when tapped and only if the original cursor context still matches.

Only the current English word fragment (2–32 characters) is queried. The sentence is not uploaded for suggestions. Password, email, URL, non-text, no-suggestions, and no-personalized-learning fields are excluded. The online switch is separate from the network translation switch.

The online cache is held in memory (256 entries, 15-minute expiry), with 2.5-second connection/read timeouts and a cooldown after failures or rate limits. Offline suggestions remain available when the system checker or network is unavailable.

## Provider documentation

- [Android spelling checker API](https://developer.android.com/reference/android/view/textservice/SpellCheckerSession)
- [Datamuse API, usage limits and privacy](https://www.datamuse.com/api/)
- [Wordnik vocabulary](https://github.com/wordnik/wordlist); MIT notice is bundled in `app/src/main/assets/english_words.LICENSE`.

Datamuse currently allows keyless access up to its published limit. Its published policy requires an API key starting **February 1, 2027**. Authentication support will need to follow the provider's published key contract; if access is refused, Google/system and offline suggestions continue to work.

## Verification

Unit tests cover ranking, spelling distance, case, deduplication, untrusted responses, offline fallback, and query eligibility. The `enhanced` mode of `KeyboardTranslationProbe` exercises the selected system checker, live Datamuse lookup and cache, and suggestion insertion in the actual IME draft without sending a message.

On-device provider verification: Datamuse returned `hippopotamus`, `hippopotamuses`, and `hippotamus` for `hipopatamus`; the session cache passed. Gboard's service identified the typo in one session but did not return alternatives, and other sessions were unavailable. The provider test verified the independent online fallback. The completed UI run passed `helo → hello`, `recieve → receive`, `archtecture → architecture`, `hipopatamus → hippopotamus`, the known-word marker, and insertion by tapping a suggestion. Earlier UI attempts were interrupted or timed out. Private phone captures and raw logs are intentionally excluded from Git. The current release JVM suite has 30 passing tests across the project.
