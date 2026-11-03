package com.zrok.myanmaraikeyboard.model

/** Small offline starter dictionary. Never learns or uploads typed text. */
object WordSuggestions {
    data class Match(val prefix: String, val words: List<String>)
    private val english = """
        hello hi how are you your yours thank thanks welcome please sorry yes no okay
        good morning afternoon evening night goodbye see soon today tomorrow yesterday
        beautiful great nice happy love like want need know think understand remember
        work working home school office meeting message call phone friend family
        what where when why who which can could would should will have has had
        doing going coming waiting looking talking reading writing learning help
        time later now ready busy fine really very much more also just about
        because before after again English Myanmar translation translate send
        information important possible problem question answer anything everything
    """.trimIndent().split(Regex("\\s+")).distinct()
    private val burmese = """
        မင်္ဂလာပါ ကျေးဇူးတင်ပါတယ် ကျေးဇူးပါ ရပါတယ် ဟုတ်ကဲ့ ဟုတ်ပါတယ် မဟုတ်ပါဘူး
        နေကောင်းလား နေကောင်းပါတယ် ကောင်းပါတယ် ကောင်းသော အဆင်ပြေလား အဆင်ပြေပါတယ်
        ဘာလုပ်နေလဲ ဘယ်မှာလဲ ဘယ်တော့လဲ ဘာကြောင့်လဲ ဘယ်လိုလဲ ဘယ်သူလဲ
        ကျွန်တော် ကျွန်မ ကျွန်ုပ် ကျွန်တော်တို့ ကျွန်မတို့ သူတို့ သင်တို့ မိတ်ဆွေ သူငယ်ချင်း
        အခု အခုတော့ ဒီနေ့ မနက်ဖြန် မနေ့က နောက်မှ ပြီးတော့ ပြီးပါပြီ ပြီးပြီ
        မနက်ခင်း နေ့လယ် ညနေ ညအိပ် အိပ်မက် လှပသော ပျော်ရွှင်ပါစေ
        ချစ်တယ် ချစ်ပါတယ် သတိရတယ် သတိထားပါ တောင်းပန်ပါတယ် ခွင့်လွှတ်ပါ
        စားပြီးပြီလား စားပါတယ် သောက်ပါတယ် သွားမယ် လာမယ် ပြန်လာမယ် စောင့်ပါ
        အလုပ် အိမ် ကျောင်း ရုံး စာ စာသား စကား စကားပြော ဘာသာပြန် မြန်မာ အင်္ဂလိပ်
        ဖုန်း ဆက်သွယ်ရန် အချိန် အကူအညီ ကူညီပါ လိုချင်တယ် လိုအပ်ပါတယ် သိပါတယ် နားလည်ပါတယ်
        မသိပါဘူး မရသေးဘူး မဖြစ်ပါဘူး ကြည့်မယ် စမ်းကြည့်မယ် လုပ်ပေးပါ ပြောပါ
    """.trimIndent().split(Regex("\\s+")).distinct()

    fun isWord(c: Char) = c.isLetter() || Character.getType(c) in listOf(
        Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt()
    ) || c == '\''

    fun suggest(before: String, after: String, myanmar: Boolean): Match {
        if (after.firstOrNull()?.let(::isWord) == true) return Match("", emptyList())
        val prefix = before.takeLastWhile(::isWord)
        if (prefix.length > 48) return Match(prefix, emptyList())
        val dictionary = if (myanmar) burmese else english
        val words = dictionary.asSequence().filter { it.startsWith(prefix, ignoreCase = !myanmar) && it != prefix }
            .take(3).map { word ->
                if (!myanmar && prefix.isNotEmpty() && prefix.all { it.isUpperCase() }) word.uppercase(java.util.Locale.ROOT)
                else if (!myanmar && prefix.firstOrNull()?.isUpperCase() == true) word.replaceFirstChar { it.uppercase() }
                else word
            }.toList()
        return Match(prefix, words)
    }
}
