package com.zrok.myanmaraikeyboard.ime

import android.graphics.Color
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.PopupWindow
import com.zrok.myanmaraikeyboard.R
import com.zrok.myanmaraikeyboard.LanguageSettingsActivity
import com.zrok.myanmaraikeyboard.MainActivity
import com.zrok.myanmaraikeyboard.data.AppPreferences
import com.zrok.myanmaraikeyboard.data.TranslationRepository
import com.zrok.myanmaraikeyboard.model.Tone
import com.zrok.myanmaraikeyboard.model.TranslationDirection
import com.zrok.myanmaraikeyboard.model.TranslationMemory
import kotlinx.coroutines.*

class TranslationInputMethodService : InputMethodService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var myanmar = true
    private var shifted = false
    private var symbols = false
    private var emojiMode = false
    private var translating = false
    private var tone = Tone.NATURAL
    private var direction = TranslationDirection.MYANMAR_TO_ENGLISH
    private var configuredDirection: TranslationDirection? = null
    private val pairDrafts = mutableMapOf<TranslationDirection, Pair<String, String>>()
    private lateinit var directionKey: TextView
    private var translationGeneration = 0L
    private val translationMemory = TranslationMemory()
    private var displayedPair: TranslationMemory.Key? = null
    private var request: Job? = null
    private var deleteRepeat: Job? = null
    private lateinit var root: LinearLayout
    private lateinit var keys: LinearLayout
    private lateinit var panel: LinearLayout
    private lateinit var source: EditText
    private lateinit var preview: EditText
    private lateinit var status: TextView
    private lateinit var sourceTab: TextView
    private lateinit var previewTab: TextView
    private lateinit var insertButton: TextView
    private var reviewing = false
    private var editingPreview = false
    private var pendingPreVowel = false
    private var characterPopup: PopupWindow? = null
    private var undoing = false
    private data class LocalUndo(val target: EditText, val before: String, val beforeStart: Int, val beforeEnd: Int, val after: String)
    private data class ExternalUndo(val before: String, val beforeStart: Int, val beforeEnd: Int, val after: String, val afterStart: Int, val afterEnd: Int)
    private val localUndo = ArrayDeque<LocalUndo>()
    private var externalUndo: ExternalUndo? = null
    private var rowHeight = 50
    private var textSize = 20
    private var keySoundEnabled = true
    private var keyVibrationEnabled = true
    private var toneGenerator: ToneGenerator? = null
    private val alternatives: Map<String, List<String>>
        get() = if (shifted) BurmesePopups.shiftedDigits else BurmesePopups.main
    private val isDark: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val ink: Int
        get() = if (isDark) Color.rgb(235, 238, 242) else Color.rgb(30, 42, 52)
    private val surface: Int
        get() = if (isDark) Color.rgb(30, 32, 35) else Color.rgb(225, 232, 237)
    private val keyFill: Int
        get() = if (isDark) Color.rgb(48, 50, 54) else Color.rgb(249, 251, 253)
    private val specialFill: Int
        get() = if (isDark) Color.rgb(70, 73, 79) else Color.rgb(204, 218, 230)
    private val editorFill: Int
        get() = if (isDark) Color.rgb(43, 45, 49) else Color.rgb(249, 251, 253)
    private val border: Int
        get() = if (isDark) Color.rgb(100, 104, 112) else Color.rgb(180, 197, 210)
    private val accent: Int
        get() = if (isDark) Color.rgb(168, 199, 250) else Color.rgb(11, 87, 208)
    private val onAccent: Int
        get() = if (isDark) Color.rgb(6, 46, 111) else Color.WHITE

    override fun onCreateInputView(): View {
        val preferences = AppPreferences(applicationContext)
        rowHeight = preferences.keyboardRowHeight
        textSize = preferences.keyboardTextSize
        keySoundEnabled = preferences.keyboardSoundEnabled
        keyVibrationEnabled = preferences.keyboardVibrationEnabled
        if (keySoundEnabled && toneGenerator == null) toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 45)
        val configured = AppPreferences(applicationContext).translationDirection
        if (configuredDirection == null) {
            direction = configured
            configuredDirection = configured
        }
        root = column()
        root.setPadding(dp(6), dp(4), dp(6), dp(8))
        root.setBackgroundColor(surface)
        val toolbar = LinearLayout(this)
        val translateToggle = tile("Translate", true) {
            translating = !translating
            panel.visibility = if (translating) View.VISIBLE else View.GONE
            if (translating) showTranslationPage(false, false)
            else keys.visibility = View.VISIBLE
        }
        toolbar.addView(translateToggle, params(2f, 44))
        val toneKey = tile(tone.label, true) {}
        toneKey.setOnClickListener {
            tone = Tone.entries[(tone.ordinal + 1) % Tone.entries.size]
            toneKey.text = tone.label
        }
        toneKey.contentDescription = "Switch translation tone"
        toolbar.addView(tile("Clear", true) {
            if (translating) {
                request?.cancel()
                displayedPair = null
                source.text.clear()
                preview.text.clear()
                status.text = ""
                showTranslationPage(false, false)
            } else {
                val before = readExternalSnapshot()
                if (before != null) {
                    val ic = currentInputConnection
                    ic?.performContextMenuAction(android.R.id.selectAll)
                    ic?.commitText("", 1)
                    val after = readExternalSnapshot()
                    if (ic != null && after != null) externalUndo = ExternalUndo(before.text.toString(), before.selectionStart, before.selectionEnd, after.text.toString(), after.selectionStart, after.selectionEnd)
                }
            }
        }, params(1f, 44))
        toolbar.addView(tile("Undo", true) { undoLast() }, params(1f, 44))
        toolbar.addView(tile("⚙", true) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.apply { contentDescription = "Keyboard settings" }, params(0.85f, 44))
        root.addView(toolbar)
        panel = column()
        directionKey = tile(direction.label, true) {}
        directionKey.contentDescription = "Switch translation direction"
        directionKey.setOnClickListener {
            request?.cancel()
            translationGeneration++
            rememberReviewedPair()
            val validPair = displayedPair?.let {
                it.text == source.text.toString() && it.direction == direction && it.tone == tone
            } == true
            direction = direction.reversed()
            directionKey.text = direction.label
            val previousSource = source.text.toString()
            val previousPreview = preview.text.toString()
            source.setText(previousPreview)
            preview.setText(previousSource)
            displayedPair = if (validPair && previousPreview.isNotBlank())
                TranslationMemory.Key(previousPreview, tone, direction) else null
            source.hint = direction.sourceHint
            preview.hint = direction.previewHint
            status.text = "Text positions swapped • No new translation requested"
            showTranslationPage(false, false)
        }
        val translationHeader = LinearLayout(this)
        translationHeader.addView(directionKey, params(1.6f, 44))
        translationHeader.addView(toneKey, params(1f, 44))
        panel.addView(translationHeader)
        source = editor(direction.sourceHint)
        preview = editor(direction.previewHint)
        enableTapToEdit(source, false)
        enableTapToEdit(preview, true)
        source.setOnFocusChangeListener { _, focus -> if (focus) editingPreview = false }
        preview.setOnFocusChangeListener { _, focus -> if (focus) editingPreview = true }
        status = TextView(this).apply {
            textSize = 12f
            setTextColor(ink)
            setPadding(dp(10), dp(3), dp(10), dp(3))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val tabs = LinearLayout(this)
        sourceTab = tile(direction.sourceLabel, true) { showTranslationPage(false, false) }
        previewTab = tile(direction.targetLabel, true) { showTranslationPage(true, true) }
        tabs.addView(sourceTab, params(1f, 44))
        tabs.addView(previewTab, params(1f, 44))
        panel.addView(tabs)
        panel.addView(source, LinearLayout.LayoutParams(-1, dp(90)))
        panel.addView(preview, LinearLayout.LayoutParams(-1, dp(90)))
        val actions = LinearLayout(this)
        actions.addView(tile("Translate →", true) {
            if (source.text.isBlank()) { status.text = "Type text first"; return@tile }
            request?.cancel()
            val input = source.text.toString()
            val selectedTone = tone
            val selectedDirection = direction
            val generation = ++translationGeneration
            rememberReviewedPair()
            val memoryKey = TranslationMemory.Key(input, selectedTone, selectedDirection)
            translationMemory.find(memoryKey)?.let { existing ->
                preview.setText(existing)
                displayedPair = memoryKey
                status.text = "Using your existing translation"
                showTranslationPage(true, true)
                return@tile
            }
            status.text = "Translating…"
            request = scope.launch {
                TranslationRepository(AppPreferences(applicationContext)).translate(input, selectedTone, selectedDirection).fold(
                    onSuccess = {
                        if (generation != translationGeneration) return@launch
                        if (source.text.toString() == input) {
                            translationMemory.remember(memoryKey, it)
                            displayedPair = memoryKey
                            preview.setText(it)
                            status.text = "Tap the translation to edit • Insert when ready"
                            if (translating) showTranslationPage(true, true)
                        } else status.text = "Source changed. Tap Translate again."
                    },
                    onFailure = { if (generation == translationGeneration) status.text = it.message }
                )
            }
        }.apply { primaryStyle(this) }, params(1.35f, 48))
        actions.addView(tile("Edit", true) {
            if (preview.text.isNotBlank()) {
                showTranslationPage(true, false, preserveCursor = true)
                status.text = "Edit the translation, then tap Insert"
            }
        }, params(0.85f, 48))
        insertButton = tile("Insert ↗", true) {
            if (preview.text.isNotEmpty()) {
                val before = readExternalSnapshot()
                val inserted = currentInputConnection?.commitText(preview.text.toString(), 1) == true
                val after = readExternalSnapshot()
                if (before != null && after != null) externalUndo = ExternalUndo(before.text.toString(), before.selectionStart, before.selectionEnd, after.text.toString(), after.selectionStart, after.selectionEnd)
                if (inserted) {
                    clearTranslationDraft("Inserted into your text field")
                } else status.text = "Tap a text field and try again"
            }
        }
        insertButton.isEnabled = preview.text.isNotBlank()
        insertButton.alpha = if (insertButton.isEnabled) 1f else 0.45f
        preview.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                insertButton.isEnabled = !s.isNullOrBlank()
                insertButton.alpha = if (insertButton.isEnabled) 1f else 0.45f
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        actions.addView(insertButton, params(1f, 48))
        val sendButton = tile("➤", true) {
            val translated = preview.text.toString()
            if (translated.isNotBlank()) {
                val before = readExternalSnapshot()
                val ic = currentInputConnection
                val inserted = ic?.commitText(translated, 1) == true
                val after = readExternalSnapshot()
                if (before != null && after != null) {
                    externalUndo = ExternalUndo(before.text.toString(), before.selectionStart, before.selectionEnd, after.text.toString(), after.selectionStart, after.selectionEnd)
                }
                if (inserted) {
                    val configuredAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                    val action = configuredAction.takeIf { it != EditorInfo.IME_ACTION_NONE && it != EditorInfo.IME_ACTION_UNSPECIFIED }
                        ?: EditorInfo.IME_ACTION_SEND
                    val sent = ic?.performEditorAction(action) == true
                    if (!sent) ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    if (!sent) ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                    clearTranslationDraft("Inserted and sent")
                } else status.text = "Tap a text field and try again"
            }
        }
        sendButton.contentDescription = "Send translation"
        primaryStyle(sendButton)
        sendButton.isEnabled = preview.text.isNotBlank()
        sendButton.alpha = if (sendButton.isEnabled) 1f else 0.45f
        preview.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendButton.isEnabled = !s.isNullOrBlank()
                sendButton.alpha = if (sendButton.isEnabled) 1f else 0.45f
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        actions.addView(sendButton, params(0.8f, 48))
        panel.addView(actions)
        panel.addView(status)
        panel.visibility = if (translating) View.VISIBLE else View.GONE
        root.addView(panel)
        keys = column()
        root.addView(keys)
        renderKeys()
        showTranslationPage(false, false)
        return root
    }

    private fun rememberReviewedPair() {
        val key = displayedPair ?: return
        if (key.text == source.text.toString() && key.direction == direction && key.tone == tone) {
            translationMemory.remember(key, preview.text.toString())
        }
    }

    private fun clearTranslationDraft(message: String) {
        request?.cancel()
        translationGeneration++
        displayedPair = null
        source.text.clear()
        preview.text.clear()
        status.text = message
        showTranslationPage(false, false)
    }

    private fun showTranslationPage(english: Boolean, expanded: Boolean, preserveCursor: Boolean = false) {
        deleteRepeat?.cancel()
        editingPreview = english
        reviewing = expanded
        source.visibility = if (english) View.GONE else View.VISIBLE
        preview.visibility = if (english) View.VISIBLE else View.GONE
        val active = if (english) preview else source
        active.layoutParams = LinearLayout.LayoutParams(-1, dp(if (expanded) 210 else 90))
        active.maxLines = if (expanded) 10 else 3
        if (!preserveCursor) active.setSelection(active.text.length)
        keys.visibility = if (translating && expanded) View.GONE else View.VISIBLE
        sourceTab.isSelected = !english
        previewTab.isSelected = english
        sourceTab.text = direction.sourceLabel
        previewTab.text = direction.targetLabel
        sourceTab.setTextColor(if (!english) onAccent else ink)
        previewTab.setTextColor(if (english) onAccent else ink)
        if (!expanded) {
            val editingLanguage = if (english) direction.target else direction.source
            myanmar = editingLanguage.code == "my"
            active.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            shifted = false
            symbols = false
            emojiMode = false
            renderKeys()
        }
        if (!expanded) active.requestFocus() else active.clearFocus()
    }

    private fun enableTapToEdit(editor: EditText, isPreview: Boolean) {
        var reviewTouch = false
        var downX = 0f
        var downY = 0f
        editor.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                reviewTouch = reviewing
                downX = event.x
                downY = event.y
                return@setOnTouchListener false
            }
            if (event.actionMasked == MotionEvent.ACTION_MOVE &&
                (kotlin.math.abs(event.x - downX) > dp(8) || kotlin.math.abs(event.y - downY) > dp(8))) {
                reviewTouch = false
            }
            if (reviewTouch) {
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    val cursor = editor.getOffsetForPosition(event.x, event.y).coerceIn(0, editor.text.length)
                    reviewTouch = false
                    view.performClick()
                    showTranslationPage(isPreview, false, preserveCursor = true)
                    editor.setSelection(cursor)
                } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    reviewTouch = false
                }
                event.actionMasked == MotionEvent.ACTION_UP
            } else false
        }
    }

    private fun editor(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 16f
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        setTextColor(ink)
        setHintTextColor(if (isDark) Color.LTGRAY else Color.DKGRAY)
        gravity = Gravity.TOP or Gravity.START
        background = GradientDrawable().apply {
            setColor(editorFill)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), border)
        }
        minLines = 1
        maxLines = 2
        showSoftInputOnFocus = false
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }

    private fun renderKeys() {
        deleteRepeat?.cancel()
        keys.removeAllViews()
        if (emojiMode) {
            keys.addView(EmojiPanel(this, ::input, ::backspace, ::enableRepeatDelete) {
                emojiMode = false
                renderKeys()
            })
            return
        }
        val rows = when {
            symbols && !shifted -> listOf(
                "1 2 3 4 5 6 7 8 9 0",
                "@ # $ _ & - + ( ) /",
                "* \" ' : ; ! ? ၊ ။"
            )
            symbols -> listOf(
                "~ ` | • √ π ÷ × ¶ ∆",
                "£ € ¥ © ® ™ ✓ [ ]",
                "{ } < > = % ^ \\ …"
            )
            myanmar && shifted -> listOf(
                "ဍ ဏ္ဍ ဣ ၎င်း ဤ ၌ ဥ ၍ ဿ ဏ",
                "ဗ ှ ီ ္ ွ ံ ဲ ဒ ဓ ဏ္ဌ",
                "ဇ ဌ ဃ ဠ ဎ ဉ ဦ ဧ"
            )
            myanmar -> listOf(
                "ဆ တ န မ အ ပ က င သ စ",
                "ေ ျ ိ ် ါ ့ ြ ု ူ း",
                "ဖ ထ ခ လ ဘ ည ာ ယ"
            )
            shifted -> listOf("Q W E R T Y U I O P", "A S D F G H J K L", "Z X C V B N M")
            else -> listOf("q w e r t y u i o p", "a s d f g h j k l", "z x c v b n m")
        }
        // Captured from the user's installed Gboard Burmese layout, 2026-11-03.
        if (myanmar && !symbols) {
            val extras = if (shifted) "၁ ၂ ၃ ၄ ၅ ၆ ၇ ၈ ၉ ၀" else "ဈ ဝ ဋ ုံ ော ဪ ရ ဂ ဟ ၏"
            addRow(extras.split(" "), false)
        }
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this)
            if (index == 2) line.addView(tile(if (shifted) "⇧" else "↑", true) {
                shifted = !shifted; renderKeys()
            }, params(1.2f))
            row.split(" ").forEach { value ->
                val key = tile(value) { input(value) }
                if (myanmar && !symbols) {
                    alternatives[value]?.let { choices ->
                        key.contentDescription = "$value, hold for " + choices.joinToString(", ")
                        key.setOnLongClickListener {
                            showAlternatives(key, choices)
                            true
                        }
                    }
                }
                line.addView(key, params())
            }
            if (index == 2) line.addView(tile("⌫", true) { backspace() }.also(::enableRepeatDelete), params(1.2f))
            keys.addView(line)
        }
        val bottom = LinearLayout(this)
        bottom.addView(tile(if (symbols) "ABC" else "?123", true) { symbols = !symbols; shifted = false; renderKeys() }, params(1.3f))
        bottom.addView(tile(if (myanmar) "EN" else "မြန်မာ", true) {
            myanmar = !myanmar; shifted = false; symbols = false; renderKeys()
        }.apply {
            contentDescription = "Switch Myanmar / English; hold to use another keyboard"
            setOnLongClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .showInputMethodPicker()
                true
            }
        }, params(1.2f))
        bottom.addView(tile("☺", true) { emojiMode = true; renderKeys() }, params(0.8f))
        bottom.addView(tile(if (myanmar) "၊" else ",", true) { input(if (myanmar) "၊" else ",") }, params(0.8f))
        bottom.addView(tile(if (myanmar) "မြန်မာ" else "English") { input(" ") }, params(2.7f))
        bottom.addView(tile(if (myanmar) "။" else ".", true) { input(if (myanmar) "။" else ".") }, params(0.8f))
        bottom.addView(tile("↵", true) { input("\n") }, params(1.2f))
        keys.addView(bottom)
    }

    private fun addRow(values: List<String>, special: Boolean) {
        val line = LinearLayout(this)
        values.forEach { value ->
            val key = tile(value, special) { input(value) }
            alternatives[value]?.let { choices ->
                key.setOnLongClickListener { showAlternatives(key, choices); true }
            }
            line.addView(key, params())
        }
        keys.addView(line)
    }

    private fun showAlternatives(anchor: View, values: List<String>) {
        characterPopup?.dismiss()
        val grid = column().apply {
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(surface)
            elevation = dp(8).toFloat()
        }
        values.chunked(4).forEach { group ->
            val row = LinearLayout(this)
            group.forEach { value ->
                row.addView(tile(value) {
                    input(value)
                    characterPopup?.dismiss()
                }, params(1f, 54))
            }
            grid.addView(row)
        }
        val width = dp(minOf(values.size, 4) * 60 + 12)
        val height = dp(((values.size + 3) / 4) * 60 + 12)
        characterPopup = PopupWindow(grid, width, height, true).apply {
            setBackgroundDrawable(GradientDrawable().apply { setColor(surface); cornerRadius = dp(12).toFloat() })
            isOutsideTouchable = true
            elevation = dp(10).toFloat()
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val x = (location[0] - width / 2 + anchor.width / 2)
                .coerceIn(0, (resources.displayMetrics.widthPixels - width).coerceAtLeast(0))
            // Use anchor coordinates: absolute display Y is offset again inside an IME window.
            showAsDropDown(anchor, x - location[0], -anchor.height - height)
        }
    }

    private fun input(value: String) {
        val canFollowPreVowel = value.length == 1 &&
            (value[0] in '\u1000'..'\u1021' || value[0] in '\u103B'..'\u103E')
        val reorderPreVowel = canFollowPreVowel && (pendingPreVowel || value[0] in '\u103B'..'\u103E')
        if (translating) {
            val target = if (editingPreview) preview else source
            val beforeText = target.text.toString()
            val beforeStart = target.selectionStart.coerceAtLeast(0)
            val beforeEnd = target.selectionEnd.coerceAtLeast(beforeStart)
            val start = target.selectionStart.coerceAtLeast(0)
            val end = target.selectionEnd.coerceAtLeast(start)
            if (reorderPreVowel && start == end &&
                start > 0 && target.text[start - 1] == '\u1031') {
                target.text.replace(start - 1, start, value + "\u1031")
                target.setSelection(start + value.length)
            } else {
                target.text.replace(start, end, value)
                target.setSelection(start + value.length)
            }
            if (!undoing && target.text.toString() != beforeText) rememberLocalUndo(target, beforeText, beforeStart, beforeEnd, target.text.toString())
        } else {
            val connection = currentInputConnection ?: return
            val before = readExternalSnapshot()
            if (reorderPreVowel &&
                connection.getSelectedText(0).isNullOrEmpty() &&
                connection.getTextBeforeCursor(1, 0)?.toString() == "\u1031") {
                connection.beginBatchEdit()
                try {
                    connection.deleteSurroundingText(1, 0)
                    connection.commitText(value + "\u1031", 1)
                } finally { connection.endBatchEdit() }
            } else connection.commitText(value, 1)
            val after = readExternalSnapshot()
            if (!undoing && before != null && after != null && before.text.toString() != after.text.toString()) {
                externalUndo = ExternalUndo(before.text.toString(), before.selectionStart, before.selectionEnd, after.text.toString(), after.selectionStart, after.selectionEnd)
            }
        }
        pendingPreVowel = value == "\u1031" ||
            (pendingPreVowel && value.length == 1 && value[0] in '\u103B'..'\u103E')
        // Myanmar Shift is a one-character layer, including popup choices.
        if (myanmar && shifted && !symbols) {
            shifted = false
            renderKeys()
        }
    }

    private fun backspace() {
        pendingPreVowel = false
        if (translating) {
            val target = if (editingPreview) preview else source
            val beforeText = target.text.toString()
            val beforeStart = target.selectionStart.coerceAtLeast(0)
            val beforeEnd = target.selectionEnd.coerceAtLeast(beforeStart)
            val start = target.selectionStart.coerceAtLeast(0)
            val end = target.selectionEnd.coerceAtLeast(start)
            if (end > start) target.text.delete(start, end)
            else if (start > 0) {
                val previous = Character.offsetByCodePoints(target.text, start, -1)
                target.text.delete(previous, start)
            }
            if (!undoing && target.text.toString() != beforeText) rememberLocalUndo(target, beforeText, beforeStart, beforeEnd, target.text.toString())
        } else {
            val before = readExternalSnapshot()
            currentInputConnection?.let(::deleteExternalText)
            val after = readExternalSnapshot()
            if (!undoing && before != null && after != null && before.text.toString() != after.text.toString()) {
                externalUndo = ExternalUndo(before.text.toString(), before.selectionStart, before.selectionEnd, after.text.toString(), after.selectionStart, after.selectionEnd)
            }
        }
    }

    private fun deleteExternalText(connection: android.view.inputmethod.InputConnection) {
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
            return
        }
        val deletedByCodePoint = connection.deleteSurroundingTextInCodePoints(1, 0)
        if (deletedByCodePoint) return
        val deletedByUtf16 = connection.deleteSurroundingText(1, 0)
        if (deletedByUtf16) return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    private fun rememberLocalUndo(target: EditText, before: String, beforeStart: Int, beforeEnd: Int, after: String) {
        localUndo.addLast(LocalUndo(target, before, beforeStart, beforeEnd, after))
        while (localUndo.size > 20) localUndo.removeFirst()
    }

    private fun readExternalSnapshot(): android.view.inputmethod.ExtractedText? {
        val ic = currentInputConnection ?: return null
        val info = currentInputEditorInfo ?: return null
        if (info.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) return null
        return ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
            ?.takeIf { it.startOffset == 0 && it.flags == 0 }
    }

    private fun undoLast() {
        if (translating) {
            val action = localUndo.removeLastOrNull()
            if (action == null) { status.text = "Nothing to undo"; return }
            undoing = true
            action.target.setText(action.before)
            action.target.setSelection(action.beforeStart.coerceIn(0, action.target.text.length), action.beforeEnd.coerceIn(0, action.target.text.length))
            undoing = false
            status.text = "Undone"
            return
        }
        val action = externalUndo ?: run { return statusMessage("Nothing to undo") }
        val now = readExternalSnapshot()
        if (now == null || now.text.toString() != action.after) return statusMessage("Text changed; undo skipped")
        val ic = currentInputConnection ?: return statusMessage("Tap a text field first")
        ic.beginBatchEdit()
        try {
            ic.performContextMenuAction(android.R.id.selectAll)
            ic.commitText(action.before, 1)
            ic.setSelection(action.beforeStart.coerceIn(0, action.before.length), action.beforeEnd.coerceIn(0, action.before.length))
        } finally { ic.endBatchEdit() }
        externalUndo = null
        statusMessage("Undone")
    }

    private fun statusMessage(message: String) {
        if (::status.isInitialized) status.text = message
    }

    private fun enableRepeatDelete(key: View) {
        key.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    deleteRepeat?.cancel()
                    view.isPressed = true
                    view.performClick()
                    deleteRepeat = scope.launch {
                        delay(400)
                        while (isActive) {
                            backspace()
                            delay(65)
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.x < 0 || event.x >= view.width || event.y < 0 || event.y >= view.height) {
                        deleteRepeat?.cancel()
                        view.isPressed = false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    deleteRepeat?.cancel()
                    view.isPressed = false
                }
            }
            true
        }
        key.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) {
                deleteRepeat?.cancel()
            }
        })
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        deleteRepeat?.cancel()
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        deleteRepeat?.cancel()
        super.onFinishInput()
    }

    private fun tile(label: String, special: Boolean = false, action: () -> Unit) = TextView(this).apply {
        // Combining marks need a visible base on keycaps; commit only the original label.
        text = if (label.length == 1 && Character.getType(label[0]) in listOf(
            Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt()
        )) "\u25CC$label" else label
        textSize = if (label.length > 2) 13f else this@TranslationInputMethodService.textSize.toFloat()
        setSingleLine(true)
        if (label.length <= 2) {
            setAutoSizeTextTypeUniformWithConfiguration(11, this@TranslationInputMethodService.textSize, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextColor(ink)
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), keyBackground(accent))
            addState(intArrayOf(android.R.attr.state_selected), keyBackground(accent))
            addState(intArrayOf(), keyBackground(if (special) specialFill else keyFill))
        }
        isSoundEffectsEnabled = false
        contentDescription = label
        isClickable = true
        setOnClickListener {
            if (keyVibrationEnabled) performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            if (keySoundEnabled) toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 18)
            action()
        }
    }

    private fun keyBackground(fill: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(12).toFloat()
    }

    private fun primaryStyle(view: TextView) {
        view.setTextColor(onAccent)
        view.background = keyBackground(accent)
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun params(weight: Float = 1f, height: Int = 50) =
        LinearLayout.LayoutParams(0, dp(if (height == 50) rowHeight else height), weight).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onEvaluateFullscreenMode() = false
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!::source.isInitialized || !::keys.isInitialized) return
        val configured = AppPreferences(applicationContext).translationDirection
        if (configured != configuredDirection) {
            request?.cancel()
            translationGeneration++
            rememberReviewedPair()
            pairDrafts[direction] = source.text.toString() to preview.text.toString()
            direction = configured
            configuredDirection = configured
            directionKey.text = direction.label
            val draft = pairDrafts[direction]
            source.setText(draft?.first.orEmpty())
            preview.setText(draft?.second.orEmpty())
            displayedPair = null
            source.hint = direction.sourceHint
            preview.hint = direction.previewHint
            status.text = "Translation languages updated"
            showTranslationPage(false, false)
        }
        val preferences = AppPreferences(applicationContext)
        keySoundEnabled = preferences.keyboardSoundEnabled
        keyVibrationEnabled = preferences.keyboardVibrationEnabled
        if (keySoundEnabled && toneGenerator == null) toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 45)
        if (rowHeight != preferences.keyboardRowHeight || textSize != preferences.keyboardTextSize) {
            rowHeight = preferences.keyboardRowHeight
            textSize = preferences.keyboardTextSize
            renderKeys()
        }
    }
    override fun onDestroy() {
        translationMemory.clear()
        characterPopup?.dismiss()
        toneGenerator?.release()
        toneGenerator = null
        scope.cancel()
        super.onDestroy()
    }
}
