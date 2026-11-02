package com.zrok.myanmaraikeyboard.ime

/**
 * Transcribed from the installed Gboard Burmese UI on 2026-11-03.
 * The 38 main keys and 38 shifted keys were held individually; screenshots live
 * in popup-audit/. Shift character keys show only a magnified copy of themselves.
 */
object BurmesePopups {
    val main = mapOf(
        "ဈ" to listOf("ဍ္ဎ", "1", "၁"),
        "ဝ" to listOf("ကျပ်", "2", "၂"),
        "ဋ" to listOf("၃", "ဋ္ဌ", "3"),
        "ုံ" to listOf("၄", "င်္", "4"),
        "ော" to listOf("၅", "ေါ", "5"),
        "ဪ" to listOf("၆", "ဩ", "6"),
        "ရ" to listOf("7", "ၒ", "ၓ", "၇"),
        "ဂ" to listOf("၈", "8", "ၔ", "ၕ"),
        "ဟ" to listOf("ဧ", "9", "ၑ", "ၐ"),
        "၏" to listOf("၀", "0"),
        "ဆ" to listOf("ဍ"), "တ" to listOf("ဏ္ဍ"),
        "န" to listOf("ဣ"), "မ" to listOf("၎င်း"),
        "အ" to listOf("ဤ"), "ပ" to listOf("၌"),
        "က" to listOf("ဥ"), "င" to listOf("၍"),
        "သ" to listOf("ဿ"), "စ" to listOf("ဏ"),
        "ေ" to listOf("ဗ"), "ျ" to listOf("ှ"),
        "ိ" to listOf("ီ"), "်" to listOf("္"),
        "ါ" to listOf("ွ"), "့" to listOf("ံ"),
        "ြ" to listOf("ဲ"), "ု" to listOf("ဒ"),
        "ူ" to listOf("ဓ"), "း" to listOf("ဏ္ဌ"),
        "ဖ" to listOf("ဇ"), "ထ" to listOf("ဌ"),
        "ခ" to listOf("ဃ"), "လ" to listOf("ဠ"),
        "ဘ" to listOf("ဎ"), "ည" to listOf("ဉ"),
        "ာ" to listOf("ဦ"), "ယ" to listOf("ဧ")
    )
    val shiftedDigits = "၁၂၃၄၅၆၇၈၉၀".mapIndexed { index, value ->
        value.toString() to listOf(((index + 1) % 10).toString())
    }.toMap()
}
