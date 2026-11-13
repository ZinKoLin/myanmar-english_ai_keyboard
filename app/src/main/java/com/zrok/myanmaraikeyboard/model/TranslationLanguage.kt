package com.zrok.myanmaraikeyboard.model

import java.util.Locale

data class TranslationLanguage(val code: String, val name: String) {
    val nativeName: String get() = when (code) {
        "my" -> "မြန်မာ"
        "mnw" -> "ဘာသာမန် (မွန်)"
        "shn" -> "လိၵ်ႈတႆး (ရှမ်း)"
        "ksw" -> "စကောကရင်"
        "kac" -> "Jinghpaw (ကချင်)"
        else -> Locale.forLanguageTag(code).getDisplayName(Locale.forLanguageTag(code))
    }
    val experimental: Boolean get() = code in setOf("mnw", "shn", "ksw", "kac")
    val shortCode: String get() = code.substringBefore('-').uppercase(Locale.ROOT)
    val flag: String get() = when (code) {
        "my" -> "🇲🇲"; "en" -> "🇬🇧"; "th" -> "🇹🇭"; "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"; "zh-Hans" -> "🇨🇳"; "zh-Hant" -> "🇹🇼"; "fr" -> "🇫🇷"
        "de" -> "🇩🇪"; "es" -> "🇪🇸"; "pt" -> "🇵🇹"; "it" -> "🇮🇹"
        "hi" -> "🇮🇳"; "vi" -> "🇻🇳"; "id" -> "🇮🇩"; "ms" -> "🇲🇾"
        "lo" -> "🇱🇦"; "km" -> "🇰🇭"; "ru" -> "🇷🇺"; "uk" -> "🇺🇦"
        "nl" -> "🇳🇱"; "sv" -> "🇸🇪"; "tr" -> "🇹🇷"; "pl" -> "🇵🇱"
        "ne" -> "🇳🇵"; "bn" -> "🇧🇩"; "ur" -> "🇵🇰"; "si" -> "🇱🇰"
        "el" -> "🇬🇷"; "he" -> "🇮🇱"; "fa" -> "🇮🇷"; "ro" -> "🇷🇴"
        else -> "🌐"
    }
    val badge: String get() = "$flag $shortCode"
    val displayName: String get() = "$flag $name — $nativeName" + if (experimental) " · AI စမ်းသပ်" else ""
}

/** General-use text languages from Google's Firebase AI Logic model documentation,
 * plus explicitly marked experimental Myanmar ethnic language choices for LLM translation.
 * Snapshot: 2026-11-03. Chinese is exposed as two script choices.
 * https://firebase.google.com/docs/ai-logic/models#supported-languages
 */
object SupportedLanguages {
    const val SOURCE_URL = "https://firebase.google.com/docs/ai-logic/models#supported-languages"
    val all: List<TranslationLanguage> = """
af|Afrikaans
sq|Albanian
am|Amharic
ar|Arabic
hy|Armenian
as|Assamese
az|Azerbaijani
eu|Basque
be|Belarusian
bn|Bengali
bs|Bosnian
bg|Bulgarian
ca|Catalan
ceb|Cebuano
zh-Hans|Chinese (Simplified)
zh-Hant|Chinese (Traditional)
co|Corsican
hr|Croatian
cs|Czech
da|Danish
dv|Dhivehi
nl|Dutch
en|English
eo|Esperanto
et|Estonian
fil|Filipino (Tagalog)
fi|Finnish
fr|French
fy|Frisian
gl|Galician
ka|Georgian
de|German
el|Greek
gu|Gujarati
ht|Haitian Creole
ha|Hausa
haw|Hawaiian
he|Hebrew
hi|Hindi
hmn|Hmong
hu|Hungarian
is|Icelandic
ig|Igbo
id|Indonesian
ga|Irish
it|Italian
ja|Japanese
jv|Javanese
kn|Kannada
kk|Kazakh
km|Khmer
ko|Korean
kri|Krio
ku|Kurdish
ky|Kyrgyz
lo|Lao
la|Latin
lv|Latvian
lt|Lithuanian
lb|Luxembourgish
mk|Macedonian
mg|Malagasy
ms|Malay
ml|Malayalam
mt|Maltese
mi|Maori
mr|Marathi
mni-Mtei|Meiteilon (Manipuri)
mn|Mongolian
my|Myanmar (Burmese)
mnw|Mon
shn|Shan
ksw|S'gaw Karen
kac|Kachin (Jinghpaw)
ne|Nepali
no|Norwegian
ny|Nyanja (Chichewa)
or|Odia (Oriya)
ps|Pashto
fa|Persian
pl|Polish
pt|Portuguese
pa|Punjabi
ro|Romanian
ru|Russian
sm|Samoan
gd|Scots Gaelic
sr|Serbian
st|Sesotho
sn|Shona
sd|Sindhi
si|Sinhala (Sinhalese)
sk|Slovak
sl|Slovenian
so|Somali
es|Spanish
su|Sundanese
sw|Swahili
sv|Swedish
tg|Tajik
ta|Tamil
te|Telugu
th|Thai
tr|Turkish
uk|Ukrainian
ur|Urdu
ug|Uyghur
uz|Uzbek
vi|Vietnamese
cy|Welsh
xh|Xhosa
yi|Yiddish
yo|Yoruba
zu|Zulu
    """.trimIndent().lines().filter { it.contains('|') }.map {
        val (code, name) = it.trim().split('|', limit = 2)
        TranslationLanguage(code, name)
    }
    fun find(code: String): TranslationLanguage? = all.firstOrNull { it.code == code }
    fun require(code: String): TranslationLanguage = requireNotNull(find(code)) { "Unsupported language: $code" }
}
