<img src="branding/brain-keyboard-icon-512.png" alt="Brain Keyboard logo" width="112" />

# Brain Keyboard

မြန်မာနှင့် English စာရိုက်နိုင်ပြီး ဘာသာပြန်ခြင်း၊ English spelling suggestions၊ emoji နှင့် stickers ပါဝင်သော native Android keyboard ဖြစ်သည်။ Keyboard ကို Kotlin InputMethodService ဖြင့်၊ settings app ကို Jetpack Compose ဖြင့် တည်ဆောက်ထားသည်။

**Version:** 1.0.0 · **versionCode:** 2 · **Android:** 8.0/API 26+ · **application ID:** `com.zrok.myanmaraikeyboard`

[Release APK / AAB](https://github.com/ZinKoLin/myanmar-english_ai_keyboard/releases/latest) · [Languages](LANGUAGES.md) · [Word suggestions](WORD_SUGGESTIONS.md) · [Release notes](docs/RELEASE_1_0_0.md)

Repository သည် private ဖြစ်သောကြောင့် GitHub source နှင့် release များကို repo access ရှိသူသာ ဖွင့်နိုင်သည်။

## လုပ်ဆောင်ချက်များ

| Feature | အသုံးပြုပုံ |
| --- | --- |
| မြန်မာ / English | Gboard ပုံစံကို အခြေခံ၍ ကိုယ်ပိုင်ရေးထားသော Myanmar Unicode layout၊ Shift layer၊ long-press characters နှင့် English QWERTY |
| 🌐 ဘာသာပြောင်းခလုတ် | တစ်ချက်နှိပ်လျှင် မြန်မာ/English ပြောင်းသည်။ ဖိထားလျှင် Android keyboard picker ဖွင့်သည်။ |
| Enter | တစ်ချက်နှိပ်လျှင် နောက်တစ်ကြောင်းဆင်းသည်။ Editor action ရှိပါက ဖိထား၍ Send/Done စသည်ကို သုံးနိုင်သည်။ |
| Delete / Undo | Delete ဖိထားလျှင် ဆက်တိုက်ဖျက်သည်။ Undo သည် keyboard ၏ နောက်ဆုံးစာပြင်မှုကို ပြန်ယူရန် ဖြစ်သည်။ |
| Translate | မူရင်းဘာသာကို auto-detect လုပ်၍ ရွေးထားသော target သို့ ပြန်ဆိုသည်။ Preview ကို ပြင်ပြီး Insert/Send လုပ်နိုင်သည်။ |
| Tone | Gemini တွင် Natural / Formal / Casual ရွေးနိုင်သည်။ Azure တွင် tone မသက်ရောက်ပါ။ |
| English suggestions | Offline dictionary၊ ရနိုင်သော Android spell checker နှင့် optional Datamuse autocomplete။ Suggestion ကို နှိပ်မှ ထည့်သည်။ မြန်မာ suggestions မပါပါ။ |
| Emoji | ကော်မာခလုတ်ကို ဖိထား၍ emoji picker ဖွင့်နိုင်သည်။ |
| Sticker / GIF | Emoji picker → Stickers မှ reaction sticker ၁၂ ခု သို့မဟုတ် ကိုယ်ပိုင်ပုံ/GIF ထည့်နိုင်သည်။ |
| Sound / vibration | System keyboard click၊ အသံအကျယ်၊ Beep/Keypad နှင့် system/custom haptic ရွေးချယ်မှု ပါသည်။ |

Gboard ၏ proprietary prediction engine၊ decoder၊ APK နှင့် online sticker catalog ကို app ထဲ မထည့်ထားပါ။ Grammar feature နှင့် clipboard auto-paste မပါပါ။

## ဖုန်းတွင် စတင်အသုံးပြုရန်

1. Release မှ **APK** ကို download/install လုပ်ပါ။ **AAB** သည် Google Play upload အတွက်ဖြစ်ပြီး တိုက်ရိုက် install ဖိုင် မဟုတ်ပါ။
2. Brain Keyboard → **Keyboard ဖွင့်ရန်** မှ Android settings တွင် enable လုပ်ပြီး keyboard picker မှ ရွေးပါ။
3. Settings တွင် Gemini key ကို Save လုပ်၍ connection စမ်းပါ။ သို့မဟုတ် Azure key/region ဖြည့်ပြီး Azure switch ကို ဖွင့်ပါ။
4. **ဘာသာပြန် ဘာသာစကားရွေးရန်** မှ target နှင့် ⇄ ပြောင်းသုံးမည့် pair ကို ရွေးပြီး Save လုပ်ပါ။
5. စာရိုက်နေရာကို နှိပ်၍ Translate panel ဖွင့်ပါ။ စာထည့်ပြီး **Translate →** နှိပ်ပါ။ Preview ကို စစ်ဆေး၍ ပြင်ပြီးမှ Insert/Send လုပ်ပါ။ **Close** သည် panel ပိတ်ရန် ဖြစ်သည်။

Update တင်ရန် package name နှင့် signing certificate တူရမည်။ Debug key နှင့် release key မတူပါက Android က update ကို ငြင်းပယ်မည်။ Uninstall လုပ်လျှင် settings၊ API keys နှင့် ကိုယ်ပိုင် sticker များ ပျက်သွားမည်။

## Gemini API key ရယူရန်

1. [Google AI Studio API Keys](https://aistudio.google.com/api-keys) ကို Google account ဖြင့် ဝင်ပါ။
2. **Create API key** နှိပ်၍ project ရွေးပါ သို့မဟုတ် ဖန်တီးပါ။
3. App → **Gemini connection → API keys ပြင်ရန် → Gemini API key 1** တွင် key ထည့်ပါ။
4. **Save connection** နှိပ်၍ **Test saved connection** ဖြင့် စမ်းပါ။ အပို key ၂–၄ ခုလည်း ထည့်နိုင်သည်။

App ထဲတွင် မြန်မာလို guide နှင့် link ပါသည်။ Key များကို repo၊ screenshots၊ issues ထဲ မတင်ပါနှင့်။ Provider ၏ region access၊ quota နှင့် billing စည်းမျဉ်းများ သက်ရောက်သည်။ [Google key documentation](https://ai.google.dev/gemini-api/docs/api-key)

Azure အတွက် [AZURE_SETUP.md](AZURE_SETUP.md) ကိုကြည့်ပါ။

## Translation နည်းလမ်းနှင့် ကြာချိန်

- မူရင်းစာသည် မြန်မာ/English သာ မဟုတ်ဘဲ မည်သည့်ဘာသာမဆို ဖြစ်နိုင်သည်။ မူရင်းဘာသာကို model က ရှာဖွေပြီး target သို့ ပြန်ဆိုသည်။
- လက်ရှိ `GEMINI_ONLY = true` သည် Gemini လမ်းကြောင်းအတွက် OpenRouter fallback ကို ပိတ်ထားသည်။ Azure switch သီးခြားရှိပြီး ဖွင့်ထားလျှင် Azure ကို သုံးသည်။ OpenRouter implementation သည် source ထဲတွင် ရှိသော်လည်း ဤ release ၏ Gemini fallback တွင် မသုံးပါ။
- ပုံမှန်စာတွင် `gemini-3.5-flash-lite`၊ `gemini-3.1-flash-lite`၊ `gemini-3.6-flash` အစဉ်ကို သုံးသည်။ မွန်တွင် full Flash ကို ဦးစားပေးသည်။ အောင်မြင်ခဲ့သော model/key ကို session ထဲတွင် မှတ်၍ နောက်တစ်ကြိမ် ဦးစားပေးသည်။
- Gemini request တစ်ခုတွင် အများဆုံး **၃ ကြိမ်** စမ်းသည်။ တစ်ခုလုံး၏ budget သည် စာတိုအတွက် **၂၀ စက္ကန့်**၊ စာရှည်အတွက် **၄၀ စက္ကန့်** ဖြစ်သည်။ ဤကန့်သတ်ချက်သည် ထိုအချိန်အတွင်း အဖြေရမည်ဟူသော SLA မဟုတ်ပါ။
- Thinking level ကို ပုံမှန်စာတွင် `minimal`၊ မွန်တွင် `low` သုံးသည်။ မရနိုင်သော model များကို cooldown ထား၍ အကြိမ်တိုင်း အစမှ ပြန်စမ်းခြင်း လျှော့ထားသည်။
- Translate နှိပ်ပြီးနောက် မူရင်းစာ ပြင်လျှင် ၅၀၀ ms ခန့်နားပြီး ပြန်ဘာသာပြန်နိုင်သည်။ မူရင်းစာကို မပြန်ဖူးသေးသောအခါ စာရိုက်ရုံဖြင့် translation request မပို့ပါ။
- Network translation ကို Privacy မှ ပိတ်နိုင်သည်။ Local dictionary lookup သည် network မလိုပါ။

### မွန်နှင့် တိုင်းရင်းသားဘာသာများ

မွန် (`mnw`), ရှမ်း (`shn`), စကောကရင် (`ksw`), ကချင်/Jinghpaw (`kac`) ကို **AI စမ်းသပ်ဘာသာစကား** အဖြစ် ထည့်ထားသည်။ စာရင်းတွင် ပါခြင်းသည် မှန်ကန်မှု အာမခံခြင်း မဟုတ်ပါ။

မွန်အတွက် [MonDictDB](https://github.com/Barnista/MonDictDB) မှ Nai Sac Lun ၏ Mon–Burmese dictionary အဖြစ် attribution ပါရှိသော entry **၁၉,၀၈၀ ခု** ကို bundle ထည့်ထားသည်။ Automated translation entry များ မပါပါ။

- မြန်မာဝေါဟာရအတိအကျနှင့် ကိုက်ညီ၍ မွန်အဓိပ္ပာယ် တစ်မျိုးတည်းရှိလျှင် local lookup ဖြင့် ပြန်ပေးသည်။
- စာကြောင်းများတွင် ကိုက်ညီသည့် dictionary senses ကို Gemini အတွက် reference အဖြစ် ပေးသည်။ စကားလုံးများ အစားထိုးစပ်၍ စာကြောင်းဖန်တီးခြင်း မဟုတ်ပါ။
- ဤနည်းသည် မွန်စာကြောင်းမှန်ကန်မှုကို အာမခံမပေးနိုင်ပါ။ စမ်းသပ်ရာတွင် မြန်မာစာကျန်နေသော မွန်ရလဒ်ကို တွေ့ဖူးသောကြောင့် **မွန်ဘာသာတတ်သူဖြင့် စစ်ဆေးရန် လိုသည်**။
- သီးသန့် မွန် sentence-translation model/server မပါပါ။ MADLAD သို့မဟုတ် အခြား hosted model အလုပ်လုပ်ပြီးသားဟု မဖော်ပြထားပါ။

## Clear နှင့် privacy

**Clear** သည် မူရင်းစာ၊ preview၊ လက်ရှိပြင်ပစာရိုက်နေရာ၏ စာ၊ session translation memory၊ language-pair drafts၊ keyboard Undo နှင့် suggestion caches ကို ရှင်းသည်။ Translation/refresh request ကို cancel လုပ်ပြီး HTTP connection ဖြတ်သည်။ နောက်ကျရောက်လာသော ရလဒ်က စာဟောင်းကို ပြန်မဖြည့်နိုင်အောင် generation စစ်ထားသည်။

Clear သည် provider server ထံ ပို့ပြီးသားစာကို ပြန်ရုပ်သိမ်းခြင်း သို့မဟုတ် အခြား app ၏ ကိုယ်ပိုင် history ကို ဖျက်ခြင်း မဟုတ်ပါ။

| ဒေတာ | အသုံးပြုပုံ / သိမ်းဆည်းပုံ |
| --- | --- |
| API keys / settings | App-private EncryptedSharedPreferences တွင် သိမ်းသည်။ User API key ကို source/APK ထဲ မထည့်ပါ။ |
| Translation စာ | ရွေးထားသော Gemini သို့မဟုတ် Azure ထံ HTTPS ဖြင့် ပို့သည်။ Draft/translation cache သည် session memory ထဲတွင်သာ ရှိသည်။ |
| Online suggestions | လက်ရှိ English fragment ၂–၃၂ လုံးကိုသာ Datamuse ထံ ပို့သည်။ စာကြောင်းတစ်ခုလုံး မပို့ပါ။ Switch သည် network translation နှင့် သီးခြားဖြစ်သည်။ |
| System spell checker | Android ၏ ရွေးထားသော checker ထံ eligible word ကို တောင်းသည်။ မရလျှင် offline/online sources ကို ဆက်သုံးသည်။ |
| Custom sticker | ရွေးထည့်ထားသောပုံ/GIF ကို app-private files သို့ ကူးသိမ်းသည်။ Receiving app သို့ ထည့်ချိန် read URI permission ပေးသည်။ |
| Clipboard | အလိုအလျောက် ဖတ်ယူ/paste မလုပ်ပါ။ User က paste လုပ်နိုင်သည်။ |

Password၊ email၊ URL၊ non-text နှင့် no-suggestions/no-personalized-learning editor fields များတွင် word suggestions ကို ရှောင်ထားသည်။ ယင်းသည် user က Translate နှိပ်ခြင်းအပေါ် တားမြစ်မှု မဟုတ်ပါ။

Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`။ Sticker အတွက် Android document picker သုံးသည်။ FileProvider သည် non-exported ဖြစ်၍ sticker directory ကိုသာ မျှဝေသည်။ Android backup ပိတ်ထားသည်။

## Sticker၊ အသံနှင့် vibration

- Import: PNG / WebP / GIF / JPEG၊ တစ်ဖိုင် 16 MB အထိ၊ တစ်ဖက် 4096 px အထိ၊ ကိုယ်ပိုင် sticker ၁၀၀ အထိ။ ဖိထား၍ ကိုယ်ပိုင် sticker ဖျက်နိုင်သည်။
- GIF ကို static preview ပြနိုင်သော်လည်း မူရင်း bytes ကို မပြောင်းဘဲ သိမ်း၍ ပို့သည်။
- Receiving editor က image MIME type ကို လက်ခံမှ `InputConnection.commitContent()` ဖြင့် ထည့်နိုင်သည်။ မလက်ခံလျှင် အသိပေးသည်။
- Gboard online sticker/GIF catalog နှင့် Emoji Kitchen မပါပါ။
- System click သည် `AudioManager.playSoundEffect()` ကို သုံးသည်။ Silent mode တွင် keyboard အသံပိတ်သည်။
- Haptic သည် ဖုန်း၏ Touch vibration setting ကို လေးစားသည်။ မတုန်လျှင် app switch အပြင် Touch vibration၊ DND နှင့် hardware ပံ့ပိုးမှုကို စစ်ပါ။

## Project ဖွဲ့စည်းပုံ

```text
app/src/main/
├── java/com/zrok/myanmaraikeyboard/
│   ├── MainActivity.kt                # Compose settings, API setup, branding
│   ├── LanguageSettingsActivity.kt    # Language catalog / pair chooser
│   ├── StickerImportActivity.kt       # Document picker
│   ├── ime/                          # Active IME, emoji and sticker panels
│   ├── data/                         # Settings, HTTPS, spell checker, sticker files
│   ├── model/                        # Languages, tone, dictionary, ranking, request policy
│   ├── presentation/                 # Existing Compose translation state holder
│   └── ui/                           # Theme, key view, sound/haptic
├── assets/                           # Wordnik and Mon lexicon, license notices
└── res/                              # Font, vectors, adaptive/themed launcher icon
app/src/test/                         # JVM tests
app/src/androidTest/                  # Device-only probes
branding/                             # SVG/PNG and reproducible logo renderer
tools/                                # Dictionary extraction, signing, popup helpers
docs/                                 # Release documentation
```

Active keyboard entry point သည် `ime/TranslationInputMethodService.kt` ဖြစ်သည်။ Existing `presentation/TranslationViewModel.kt` နှင့် Compose `ui/TranslationPanel.kt` သည် active IME panel ကို တိုက်ရိုက်ထိန်းချုပ်သည့်နေရာ မဟုတ်ပါ။

## Build လုပ်ရန်

လိုအပ်ချက်: **JDK 17+**, Android SDK Platform **35**, Build Tools **35.0.0**, dependency download အတွက် network။ Android Studio bundled JBR နှင့် **Gradle 8.13** ဖြင့် စမ်းသပ်ထားသည်။ Wrapper ကို 8.13 နှင့် SHA-256 checksum ဖြင့် pin ထားသည်။

Android Studio တွင် project ဖွင့်၍ SDK location သတ်မှတ်ပါ။ CLI တွင် `ANDROID_HOME` သို့မဟုတ် local `local.properties` ၏ `sdk.dir` ဖြင့် မိမိ SDK path ဖြည့်ပါ။ Local SDK config ကို commit မလုပ်ပါနှင့်။

```powershell
# Windows PowerShell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testReleaseUnitTest :app:lintRelease
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

```bash
# macOS / Linux
chmod +x gradlew
./gradlew :app:assembleDebug
./gradlew :app:testReleaseUnitTest :app:lintRelease
./gradlew :app:assembleRelease :app:bundleRelease
```

Fresh checkout တွင် wrapper/Maven dependencies အရင် download လုပ်ရမည်။ Cache ပြည့်ပြီးမှသာ `--offline` သုံးပါ။

| Output | ဖိုင် |
| --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Unsigned release APK | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| Unsigned bundle | `app/build/outputs/bundle/release/app-release.aab` |
| Unit test report | `app/build/reports/tests/testReleaseUnitTest/` |
| Lint report | `app/build/reports/lint-results-release.html` |

### Release signing

Keystore ကို repository အပြင်ဘက်တွင် ထိန်းသိမ်းထားပါ။ Gradle config တွင် signing password ကို hardcode မလုပ်ထားပါ။ Release outputs ကို build လုပ်ပြီး Windows တွင်:

```powershell
.\tools\sign-release.ps1 -Keystore 'C:\secure\release.jks' -Alias key -SamePassword
```

Password ကို masked prompt မှ ရိုက်ရသည်။ Password နှစ်ခုမတူပါက `-SamePassword` မထည့်ပါနှင့်။ Script သည် child tools အတွက် transient environment variables သုံးပြီး အပြီးတွင် ရှင်းသည်။ JKS/password ကို repo ထဲ မကူးပါ။

APK alignment၊ v2/v3 signature နှင့် AAB JAR signature ကို စစ်၍ local ignored `dist/` သို့ ထုတ်သည်။ Binary files ကို GitHub Release assets အဖြစ် တင်နိုင်သည်။

```text
dist/Brain-Keyboard-1.0.0-release.apk
dist/Brain-Keyboard-1.0.0-release.aab
dist/SHA256SUMS.txt
```

နောက် update တွင် တူညီသော key သုံး၍ versionCode တိုးပါ။ Signing helper ၏ tool paths သည် Windows/Android Studio default installation နှင့် Build Tools 35.0.0 ကို အခြေခံထားသောကြောင့် အခြားစက်တွင် လိုအပ်သလို ပြင်ပါ။

## စမ်းသပ်မှုနှင့် ကန့်သတ်ချက်များ

- Release JVM tests **၃၀ ခု** အောင်မြင်သည်။ Ranking၊ cache isolation၊ dictionary ambiguity၊ request policy နှင့် cancelled HTTP result မပြန်သက်ရောက်ခြင်းတို့ ပါဝင်သည်။
- Release lint **error ၀ ခု**၊ warning **၃၅ ခု**၊ informational issue **၅ ခု** ဖြစ်သည်။ Target SDK/API deprecation စသည့် အချက်များကို report တွင် ကြည့်နိုင်သည်။ Warning-free build မဟုတ်ပါ။
- Development APK ကို ဖုန်းပေါ်တွင် မြန်မာ/English key၊ Shift၊ Enter၊ number/emoji long-press၊ repeat Delete၊ Clear/Undo၊ suggestions၊ sticker PNG commit နှင့် GIF import စမ်းသပ်ထားသည်။
- Touch vibration ဖွင့်ပြီး system trace တွင် sound playback နှင့် completed vibration events တွေ့ခဲ့သည်။
- Signed release ကို signature/alignment စစ်ထားသည်။ Release certificate ဖြင့် debug app ကို uninstall အစားထိုး၍ device test ပြန်မလုပ်ထားပါ။
- Language ၁၁၄ ခုလုံး ဘာသာပြန်မှန်ကန်မှု စမ်းသပ်မထားပါ။ မွန် sentence quality သည် အတည်မပြုနိုင်သေးသော ကန့်သတ်ချက် ဖြစ်သည်။

Device probe ဥပမာ:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e action revision com.zrok.myanmaraikeyboard.test/com.zrok.myanmaraikeyboard.KeyboardTranslationProbe
```

Main/test APK certificate တူရမည်။ `revision` သည် private test editor တွင် Clear/sticker ကို စစ်သည်။ `layout` သည် key interactions၊ `feedback` သည် key sound/haptic settings ကို စစ်သည်။ `providers`, `enhanced`, `mon` နှင့် default translation probe သည် network သုံးနိုင်၍ quota သက်ရောက်နိုင်သည်။ Feedback probe က app sound/haptic settings ဖွင့်ထားနိုင်သောကြောင့် ကိုယ်ပိုင်ဖုန်းတွင် မိမိ setting ကို ပြန်စစ်ပါ။

## Data၊ logo နှင့် attribution

- [Wordnik vocabulary](https://github.com/wordnik/wordlist), MIT — [license](app/src/main/assets/english_words.LICENSE)။
- [MonDictDB](https://github.com/Barnista/MonDictDB), MIT — revision `a112735f8800694a6adc8d14c4d9d3f4a4833caf`။ Word author 1/3 နှင့် Burmese definition author 1 (Nai Sac Lun) ကိုသာ ရွေးထားသည်။ [License](app/src/main/assets/mon_burmese_lexicon.LICENSE)။
- Pyidaungsu Regular သည် existing project font asset ဖြစ်သည်။
- Project အတွက် ဆွဲထားသော Brain Keyboard logo ၏ SVG/PNG exports ကို [branding/](branding/) တွင် ကြည့်နိုင်သည်။ PNG ပြန်ထုတ်ရန် `branding/render-brand.ps1` ကို သုံးပါ။

Mon dictionary ကို source မှ ပြန်ထုတ်ရန် Node.js နှင့် PowerShell သုံးပါ:

```powershell
.\tools\fetch-mon-lexicon.ps1
```

Pinned upstream data ကို ignored `artifacts/mon-research/` သို့ download လုပ်၍ `tools/build-mon-lexicon.cjs` က SQL literals ကို **ဒေတာအဖြစ်သာ parse** လုပ်သည်။ Download လုပ်ထားသော JS/SQL ကို execute မလုပ်ပါ။ TSV နှင့် MIT notice ကို `app/src/main/assets/` သို့ ရေးသည်။

GitHub source တွင် keystore၊ secrets၊ local caches၊ ကိုယ်ပိုင်ဖုန်း screenshot/log နှင့် third-party APK စမ်းသပ်ဖိုင်များ မပါဝင်ပါ။
