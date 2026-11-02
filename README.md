# Myanmar AI Keyboard MVP

Native Android Kotlin/Compose input method that lets a user type Burmese, choose a translation tone, request an English translation through Gemini, edit the result, and insert it into the active text field.

## Architecture

- `ime/TranslationInputMethodService.kt`: Android `InputMethodService`; owns the keyboard surface and inserts with `InputConnection.commitText`.
- `ui/`: Compose UI shared by the settings-era design language and the keyboard panel.
- `presentation/`: state holder for source text, tone, preview, loading, and error/success messages.
- `data/`: encrypted preferences plus a Gemini REST repository. The key is user-configured; no secret is compiled into the app.
- `res/font/pyidaungsu_regular.ttf`: Pyidaungsu Regular 2.5.3 bundled for Burmese key labels and Compose text.

The Burmese key rows use a Gboard-style Myanmar Unicode arrangement: number row, `ဆ တ န မ အ ပ က င သ စ ဟ`, vowel/medial row, and `ဖ ထ ခ လ ဘ ည ာ ၊ ။`. English switches to a standard QWERTY layer. This is a native implementation of the visible layout; Gboard's proprietary suggestion/correction engine is not embedded.

## Translation API contract

The default endpoint is Gemini `models/gemini-2.5-flash:generateContent`. The app sends:

```json
{"contents":[{"parts":[{"text":"Translate the following Burmese ..."}]}]}
```

The API key is sent using Gemini's `x-goog-api-key` header. The app reads the response from `candidates[0].content.parts[0].text`.

## Privacy behavior

- No text is persisted by the MVP.
- No translation request is made until the user taps **Translate**.
- The user can disable network translation in the companion app.
- The API key is stored in `EncryptedSharedPreferences` and is never logged.
- The default endpoint is Gemini; the user only needs to add their own API key.

## Run

Open `android-keyboard` in Android Studio and run the `app` module. On the device, open **Myanmar AI Keyboard**, configure the endpoint, tap **Enable keyboard**, then select the keyboard from any text field.

This workspace does not include a Gradle wrapper, and Gradle is not installed in the current environment, so a local APK build could not be executed here.
