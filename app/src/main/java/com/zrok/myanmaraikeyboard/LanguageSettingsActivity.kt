package com.zrok.myanmaraikeyboard

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.zrok.myanmaraikeyboard.data.AppPreferences
import com.zrok.myanmaraikeyboard.model.SupportedLanguages
import com.zrok.myanmaraikeyboard.model.TranslationLanguage

class LanguageSettingsActivity : Activity() {
    private lateinit var preferences: AppPreferences
    private val selected = linkedSetOf<String>()
    private lateinit var sourceSpinner: Spinner
    private lateinit var targetSpinner: Spinner
    private lateinit var save: Button
    private lateinit var message: TextView
    private lateinit var choices: LinearLayout
    private lateinit var search: EditText
    private var pairLanguages = emptyList<TranslationLanguage>()
    private var changingAdapters = false
    private var sourceCode = "my"
    private var targetCode = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(applicationContext)
        selected.addAll(savedInstanceState?.getStringArrayList("selected") ?: preferences.enabledLanguageCodes)
        val pair = preferences.translationDirection
        sourceCode = savedInstanceState?.getString("source") ?: pair.source.code
        targetCode = savedInstanceState?.getString("target") ?: pair.target.code
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(dp(16) + insets.systemWindowInsetLeft,
                    dp(12) + insets.systemWindowInsetTop, dp(16) + insets.systemWindowInsetRight,
                    dp(12) + insets.systemWindowInsetBottom)
                insets
            }
        }
        fun label(text: String, size: Float = 14f) = TextView(this).apply {
            this.text = text; textSize = size; setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(label("Translation languages", 22f))
        root.addView(label("မူရင်းစာ၏ ဘာသာစကားကို အလိုအလျောက် သိရှိပေးမည်။ To တွင် ပြန်ဆိုလိုသည့်ဘာသာကို ရွေးပါ။"))
        root.addView(label("⇄ နှိပ်သောအခါ ပြန်ဆိုမည့်ဘာသာ"))
        sourceSpinner = Spinner(this)
        root.addView(sourceSpinner)
        root.addView(label("To"))
        targetSpinner = Spinner(this)
        root.addView(targetSpinner)
        root.addView(label("Built-in keys: Myanmar and English. For other scripts, paste text or hold the globe key to switch keyboards."))
        root.addView(label("မွန်: အဘိဓာန် + AI · တိုင်းရင်းသားဘာသာများကို စမ်းသပ်ပြန်ဆိုပေးသည်။ ရလဒ်စစ်ဆေးပါ။"))
        search = EditText(this).apply {
            hint = "Search language / code / မြန်မာ"
            setSingleLine(true)
        }
        root.addView(search)
        choices = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(choices) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        message = label("")
        root.addView(message)
        val buttons = LinearLayout(this)
        buttons.addView(Button(this).apply { text = "Cancel"; setOnClickListener { finish() } },
            LinearLayout.LayoutParams(0, dp(52), 1f))
        save = Button(this).apply {
            text = "Save languages"
            setOnClickListener {
                preferences.saveTranslationLanguages(selected, sourceCode, targetCode)
                Toast.makeText(this@LanguageSettingsActivity, "Translation languages saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        buttons.addView(save, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(buttons)
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (changingAdapters) return
                if (parent === sourceSpinner) sourceCode = pairLanguages.getOrNull(position)?.code.orEmpty()
                else targetCode = pairLanguages.getOrNull(position)?.code.orEmpty()
                validate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        sourceSpinner.onItemSelectedListener = listener
        targetSpinner.onItemSelectedListener = listener
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderChoices()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        setContentView(root)
        rebuildPair()
        renderChoices()
        root.requestApplyInsets()
    }

    private fun renderChoices() {
        val query = search.text.toString().trim()
        choices.removeAllViews()
        val matches = SupportedLanguages.all.filter {
            query.isBlank() || it.name.contains(query, true) || it.nativeName.contains(query, true) ||
                it.code.contains(query, true)
        }
        if (matches.isEmpty()) choices.addView(TextView(this).apply { text = "No matching languages" })
        matches.forEach { language ->
            choices.addView(CheckBox(this).apply {
                text = language.displayName
                textSize = 16f
                minHeight = dp(52)
                isChecked = language.code in selected
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(language.code) else selected.remove(language.code)
                    rebuildPair()
                }
            })
        }
    }

    private fun rebuildPair() {
        changingAdapters = true
        pairLanguages = SupportedLanguages.all.filter { it.code in selected }
        if (sourceCode !in selected) sourceCode = pairLanguages.firstOrNull()?.code.orEmpty()
        if (targetCode !in selected || targetCode == sourceCode) {
            targetCode = pairLanguages.firstOrNull { it.code != sourceCode }?.code.orEmpty()
        }
        fun adapter() = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            pairLanguages.map { it.displayName }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        sourceSpinner.adapter = adapter()
        targetSpinner.adapter = adapter()
        sourceSpinner.setSelection(pairLanguages.indexOfFirst { it.code == sourceCode }.coerceAtLeast(0))
        targetSpinner.setSelection(pairLanguages.indexOfFirst { it.code == targetCode }.coerceAtLeast(0))
        sourceSpinner.isEnabled = pairLanguages.size >= 2
        targetSpinner.isEnabled = pairLanguages.size >= 2
        changingAdapters = false
        validate()
    }

    private fun validate() {
        save.isEnabled = selected.size >= 2 && sourceCode in selected && targetCode in selected && sourceCode != targetCode
        message.text = when {
            selected.size < 2 -> "Select at least two languages."
            sourceCode == targetCode -> "From and To must be different."
            else -> "${selected.size} selected • ${SupportedLanguages.all.size} available"
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("selected", ArrayList(selected))
        outState.putString("source", sourceCode)
        outState.putString("target", targetCode)
        super.onSaveInstanceState(outState)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
