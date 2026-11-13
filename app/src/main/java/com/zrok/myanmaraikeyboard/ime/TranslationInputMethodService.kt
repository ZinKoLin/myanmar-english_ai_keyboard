package com.zrok.myanmaraikeyboard.ime

import android.graphics.Color
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.os.VibrationEffect
import android.media.AudioAttributes
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.InsetDrawable
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
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.zrok.myanmaraikeyboard.R
import com.zrok.myanmaraikeyboard.LanguageSettingsActivity
import com.zrok.myanmaraikeyboard.MainActivity
import com.zrok.myanmaraikeyboard.data.AppPreferences
import com.zrok.myanmaraikeyboard.data.TranslationRepository
import com.zrok.myanmaraikeyboard.data.OnlineWordSuggestions
import com.zrok.myanmaraikeyboard.data.SystemWordSuggestions
import com.zrok.myanmaraikeyboard.model.Tone
import com.zrok.myanmaraikeyboard.model.TranslationDirection
import com.zrok.myanmaraikeyboard.model.TranslationMemory
import com.zrok.myanmaraikeyboard.model.TranslationRefresh
import com.zrok.myanmaraikeyboard.model.WordSuggestions
import com.zrok.myanmaraikeyboard.model.translationRefresh
import com.zrok.myanmaraikeyboard.ui.KeyboardKeyView
import kotlinx.coroutines.*

class TranslationInputMethodService : InputMethodService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var myanmar = true
    private var shifted = false
    private var symbols = false
    private var emojiMode = false
    private var stickerMode = false
    private var translating = false
    private var tone = Tone.NATURAL
    private var direction = TranslationDirection.MYANMAR_TO_ENGLISH
    private var configuredDirection: TranslationDirection? = null
    private val pairDrafts = mutableMapOf<TranslationDirection, Pair<String, String>>()
    private lateinit var directionKey: TextView
    private lateinit var toneKey: TextView
    private var translationGeneration = 0L
    private val translationMemory = TranslationMemory()
    private var displayedPair: TranslationMemory.Key? = null
    private var followSourceEdits = false
    private var suppressSourceWatch = 0
    private var request: Job? = null
    private var retranslate: Job? = null
    private var deleteRepeat: Job? = null
    private lateinit var root: LinearLayout
    private lateinit var keys: LinearLayout
    private lateinit var suggestionStrip: LinearLayout
    private lateinit var suggestionScroll: HorizontalScrollView
    private var suggestionsEnabled = true
    private var onlineSuggestionsEnabled = true
    private var inputVisible = false
    private val onlineWords = OnlineWordSuggestions()
    private val systemWords by lazy { SystemWordSuggestions(this, ::refreshSuggestions) }
    private var suggestionRequest: Job? = null
    private var requestedSuggestionContext: SuggestionContext? = null
    private var suggestionGeneration = 0L
    private var lastSuggestionState: Pair<SuggestionContext?, WordSuggestions.Match?>? = null
    private lateinit var panel: LinearLayout
    private lateinit var source: EditText
    private lateinit var preview: EditText
    private var fullPreviewDialog: Dialog? = null
    private lateinit var translateToggle: TextView
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
    private val feedback by lazy { com.zrok.myanmaraikeyboard.ui.KeyFeedback(this) }
    private var selectedTone = 0
    private var vibrationDuration = 35
    private var soundVolume = 0.5f
    private var systemHaptic = true
    private val alternatives: Map<String, List<String>>
        get() = if (shifted) BurmesePopups.shiftedDigits else BurmesePopups.main
    // Match the installed Gboard charcoal keyboard theme independently of the companion app.
    private val isDark: Boolean get() = true
    private val ink: Int
        get() = if (isDark) Color.rgb(227, 227, 227) else Color.rgb(30, 42, 52)
    private val surface: Int
        get() = if (isDark) Color.rgb(30, 31, 32) else Color.rgb(225, 232, 237)
    private val keyFill: Int
        get() = if (isDark) Color.rgb(55, 57, 59) else Color.rgb(249, 251, 253)
    private val specialFill: Int
        get() = if (isDark) Color.rgb(68, 71, 70) else Color.rgb(204, 218, 230)
    private val editorFill: Int
        get() = if (isDark) Color.rgb(43, 45, 49) else Color.rgb(249, 251, 253)
    private val border: Int
        get() = if (isDark) Color.rgb(100, 104, 112) else Color.rgb(180, 197, 210)
    private val accent: Int
        get() = if (isDark) Color.rgb(168, 199, 250) else Color.rgb(11, 87, 208)
    private val onAccent: Int
        get() = if (isDark) Color.rgb(6, 46, 111) else Color.WHITE

    override fun onCreate() {
        super.onCreate()
        scope.launch(Dispatchers.IO) {
            com.zrok.myanmaraikeyboard.data.MonDictionary.get(assets)
        }
        scope.launch {
            withContext(Dispatchers.Default) {
                assets.open("english_words.txt").bufferedReader().use { reader ->
                    WordSuggestions.loadEnglishWords(reader.lineSequence())
                }
            }
            if (::root.isInitialized) refreshSuggestions()
        }
    }

    override fun onCreateInputView(): View {
        val preferences = AppPreferences(applicationContext)
        rowHeight = preferences.keyboardRowHeight
        textSize = preferences.keyboardTextSize
        keySoundEnabled = preferences.keyboardSoundEnabled
        keyVibrationEnabled = preferences.keyboardVibrationEnabled
        initializeKeySound()
        val configured = AppPreferences(applicationContext).translationDirection
        if (configuredDirection == null) {
            direction = configured
            configuredDirection = configured
        }
        root = column()
        root.setPadding(dp(2), dp(4), dp(2), dp(8))
        root.setBackgroundColor(surface)
        val toolbar = LinearLayout(this)
        translateToggle = tile("Translate", true) {
            translating = !translating
            panel.visibility = if (translating) View.VISIBLE else View.GONE
            if (translating) showTranslationPage(false, false)
            else {
                request?.cancel()
                cancelPendingRefresh()
                translationGeneration++
                keys.visibility = View.VISIBLE
            }
            updateTranslationToggle()
            refreshSuggestions()
        }
        toolbar.addView(translateToggle, params(2f, 44))
        toneKey = tile(tone.label, true) {
            if (!toneKey.isEnabled) return@tile
            tone = Tone.entries[(tone.ordinal + 1) % Tone.entries.size]
            toneKey.text = tone.label
        }
        toneKey.contentDescription = "Switch translation tone"
        if (preferences.azureEnabled) {
            toneKey.text = "Azure"
            toneKey.contentDescription = "Azure standard translation; tone selection unavailable"
            toneKey.isEnabled = false
        }
        toolbar.addView(tile("Clear", true) { clearTypedText() }, params(1f, 44))
        toolbar.addView(tile("Undo", true) { undoLast() }, params(1f, 44))
        toolbar.addView(tile("⚙", true) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.apply { contentDescription = "Keyboard settings" }, params(0.85f, 44))
        root.addView(toolbar)
        panel = column()
        directionKey = tile(direction.label, true) {}
        directionKey.contentDescription = "Switch translation direction"
        directionKey.setOnClickListener {
            playKeyFeedback()
            request?.cancel()
            cancelPendingRefresh()
            translationGeneration++
            rememberReviewedPair()
            direction = direction.reversed()
            directionKey.text = direction.label
            val previousPreview = preview.text.toString()
            withoutSourceWatch {
                source.setText(previousPreview)
                preview.text.clear()
            }
            displayedPair = null
            followSourceEdits = false
            source.hint = direction.sourceHint
            preview.hint = direction.previewHint
            status.text = "Target changed • Tap Translate"
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
        previewTab = tile(direction.targetLabel, true) {
            if (translationRefresh(followSourceEdits, source.text.toString(), displayedPair, tone, direction) == TranslationRefresh.Update) {
                root.removeCallbacks(applySourceChange)
                requestTranslation(switchToPreview = true)
            } else showTranslationPage(true, true)
        }
        tabs.addView(sourceTab, params(1f, 44))
        tabs.addView(previewTab, params(1f, 44))
        tabs.addView(tile("⛶", true) { showFullPreview() }.apply {
            contentDescription = "Full screen translation preview"
        }, params(0.4f, 44))
        panel.addView(tabs)
        panel.addView(source, LinearLayout.LayoutParams(-1, dp(90)))
        panel.addView(preview, LinearLayout.LayoutParams(-1, dp(90)))
        val actions = LinearLayout(this)
        actions.addView(tile("Translate →", true) {
            requestTranslation(switchToPreview = true)
        }.apply { primaryStyle(this) }, params(1.35f, 48))
        actions.addView(tile("Edit", true) {
            if (preview.text.isNotBlank()) {
                showTranslationPage(true, false, preserveCursor = true)
                status.text = "Edit the translation, then tap Insert"
            }
        }, params(0.85f, 48))
        insertButton = tile("Insert ↗", true) { commitTranslation(send = false) }
        insertButton.contentDescription = "Insert translation"
        val sendButton = tile("➤", true) { commitTranslation(send = true) }
        sendButton.contentDescription = "Send translation"
        primaryStyle(sendButton)
        val canSend = preview.text.isNotBlank()
        insertButton.isEnabled = canSend
        sendButton.isEnabled = canSend
        insertButton.alpha = if (canSend) 1f else 0.45f
        sendButton.alpha = if (canSend) 1f else 0.45f
        preview.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val ready = !s.isNullOrBlank()
                insertButton.isEnabled = ready
                sendButton.isEnabled = ready
                insertButton.alpha = if (ready) 1f else 0.45f
                sendButton.alpha = if (ready) 1f else 0.45f
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        actions.addView(insertButton, params(1f, 48))
        actions.addView(sendButton, params(0.8f, 48))
        panel.addView(actions)
        panel.addView(status)
        panel.visibility = if (translating) View.VISIBLE else View.GONE
        root.addView(panel)
        suggestionStrip = LinearLayout(this).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        lastSuggestionState = null
        suggestionScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            isHorizontalFadingEdgeEnabled = true
            addView(suggestionStrip)
        }
        root.addView(suggestionScroll)
        keys = column()
        root.addView(keys)
        renderKeys()
        showTranslationPage(false, false)
        source.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (suppressSourceWatch == 0) rememberReviewedPair()
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshSuggestions()
                onSourceDiverged()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        preview.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshSuggestions() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        suggestionsEnabled = preferences.suggestionsEnabled
        onlineSuggestionsEnabled = preferences.onlineSuggestionsEnabled
        refreshSuggestions()
        return root
    }

    private fun rememberReviewedPair() {
        val key = displayedPair ?: return
        if (key.text == source.text.toString() && key.direction == direction && key.tone == tone) {
            translationMemory.remember(key, preview.text.toString())
        }
    }

    private fun withoutSourceWatch(block: () -> Unit) {
        suppressSourceWatch++
        try {
            block()
        } finally {
            suppressSourceWatch--
        }
    }

    private fun cancelPendingRefresh() {
        retranslate?.cancel()
        if (::root.isInitialized) root.removeCallbacks(applySourceChange)
    }

    private val applySourceChange = Runnable {
        if (suppressSourceWatch > 0 || !::source.isInitialized) return@Runnable
        when (translationRefresh(followSourceEdits, source.text.toString(), displayedPair, tone, direction)) {
            TranslationRefresh.Keep -> Unit
            TranslationRefresh.Clear -> {
                cancelPendingRefresh()
                request?.cancel()
                translationGeneration++
                followSourceEdits = false
                displayedPair = null
                preview.text.clear()
                status.text = ""
            }
            TranslationRefresh.Update -> scheduleSourceRefresh()
        }
    }

    private fun onSourceDiverged() {
        if (suppressSourceWatch > 0 || !::root.isInitialized) return
        val decision = translationRefresh(followSourceEdits, source.text.toString(), displayedPair, tone, direction)
        root.removeCallbacks(applySourceChange)
        if (decision == TranslationRefresh.Keep) {
            if (displayedPair?.text == source.text.toString()) cancelPendingRefresh()
            return
        }
        root.post(applySourceChange)
    }

    private fun scheduleSourceRefresh() {
        val current = source.text.toString()
        val key = TranslationMemory.Key(current, tone, direction)
        cancelPendingRefresh()
        request?.cancel()
        translationGeneration++
        translationMemory.find(key)?.let { existing ->
            preview.setText(existing)
            displayedPair = key
            status.text = "Using your existing translation"
            return
        }
        displayedPair = null
        preview.text.clear()
        status.text = "Source changed • updating translation"
        val scheduled = translationGeneration
        retranslate = scope.launch {
            delay(500)
            if (scheduled != translationGeneration || source.text.toString() != current) return@launch
            retranslate = null
            requestTranslation(switchToPreview = false)
        }
    }

    private fun requestTranslation(switchToPreview: Boolean) {
        cancelPendingRefresh()
        if (source.text.isBlank()) {
            status.text = "Type text first"
            return
        }
        request?.cancel()
        val input = source.text.toString()
        val selectedTone = tone
        val selectedDirection = direction
        val generation = ++translationGeneration
        rememberReviewedPair()
        followSourceEdits = true
        val memoryKey = TranslationMemory.Key(input, selectedTone, selectedDirection)
        if (displayedPair != memoryKey) {
            displayedPair = null
            preview.text.clear()
        }
        translationMemory.find(memoryKey)?.let { existing ->
            preview.setText(existing)
            displayedPair = memoryKey
            status.text = "Using your existing translation"
            if (switchToPreview) showTranslationPage(true, true)
            return
        }
        status.text = "Translating…"
        request = scope.launch {
            TranslationRepository(AppPreferences(applicationContext)).translate(input, selectedTone, selectedDirection).fold(
                onSuccess = {
                    if (generation != translationGeneration) return@launch
                    val unchanged = source.text.toString() == input && direction == selectedDirection && tone == selectedTone
                    if (unchanged) {
                        translationMemory.remember(memoryKey, it)
                        displayedPair = memoryKey
                        preview.setText(it)
                        status.text = if (switchToPreview) {
                            "Tap the translation to edit • Insert when ready"
                        } else {
                            "Translation updated"
                        }
                        if (switchToPreview && translating) showTranslationPage(true, true)
                    } else if (followSourceEdits && source.text.isNotBlank()) {
                        scheduleSourceRefresh()
                    }
                },
                onFailure = { if (generation == translationGeneration) status.text = it.message }
            )
        }
    }

    private fun clearTranslationDraft(message: String) {
        request?.cancel()
        cancelPendingRefresh()
        translationGeneration++
        followSourceEdits = false
        displayedPair = null
        withoutSourceWatch {
            source.text.clear()
            preview.text.clear()
        }
        status.text = message
        showTranslationPage(false, false)
    }

    private fun clearTypedText() {
        request?.cancel()
        request = null
        cancelPendingRefresh()
        retranslate = null
        translationGeneration++
        followSourceEdits = false
        displayedPair = null
        translationMemory.clear()
        pairDrafts.clear()
        localUndo.clear()
        externalUndo = null
        pendingPreVowel = false
        stickerMode = false
        emojiMode = false
        deleteRepeat?.cancel()
        cancelSuggestionRequests()
        onlineWords.clear()
        systemWords.close()
        characterPopup?.dismiss()
        fullPreviewDialog?.dismiss()
        withoutSourceWatch {
            source.setText("")
            preview.setText("")
        }
        status.text = ""
        currentInputConnection?.let { ic ->
            ic.beginBatchEdit()
            try {
                if (ic.performContextMenuAction(android.R.id.selectAll)) ic.commitText("", 1)
                ic.finishComposingText()
            } finally { ic.endBatchEdit() }
        }
        if (translating) showTranslationPage(false, false) else renderKeys()
        // TextWatcher/selection notifications must not restore a cleared undo or draft.
        localUndo.clear()
        externalUndo = null
    }

    private fun commitTranslation(send: Boolean) {
        val translated = preview.text.toString()
        if (translated.isBlank()) {
            status.text = "Translate first"
            return
        }
        val before = readExternalSnapshot()
        val ic = currentInputConnection
        val inserted = ic?.commitText(translated, 1) == true
        val after = readExternalSnapshot()
        if (before != null && after != null) externalUndo = ExternalUndo(before.text.toString(), before.selectionStart, before.selectionEnd, after.text.toString(), after.selectionStart, after.selectionEnd)
        if (!inserted) {
            status.text = "Tap a text field and try again"
            return
        }
        if (send) {
            val configuredAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
            val action = configuredAction.takeIf { it != EditorInfo.IME_ACTION_NONE && it != EditorInfo.IME_ACTION_UNSPECIFIED }
                ?: EditorInfo.IME_ACTION_SEND
            val sent = ic?.performEditorAction(action) == true
            if (!sent) ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            if (!sent) ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            clearTranslationDraft("Inserted and sent")
        } else clearTranslationDraft("Inserted into your text field")
    }

    private fun updateTranslationToggle() {
        translateToggle.text = if (translating) "Close" else "Translate"
        translateToggle.contentDescription = if (translating) "Close translation" else "Open translation"
        translateToggle.setTextColor(if (translating) Color.rgb(255, 218, 214) else ink)
        translateToggle.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        translateToggle.background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), keyBackground(if (translating) Color.rgb(140, 45, 44) else accent))
            addState(intArrayOf(), keyBackground(if (translating) Color.rgb(100, 35, 36) else specialFill).apply {
                if (translating) setStroke(dp(1), Color.rgb(240, 135, 130))
            })
        }
    }

    private fun showTranslationPage(english: Boolean, expanded: Boolean, preserveCursor: Boolean = false) {
        updateTranslationToggle()
        deleteRepeat?.cancel()
        editingPreview = english
        reviewing = expanded
        source.visibility = if (english) View.GONE else View.VISIBLE
        preview.visibility = if (english) View.VISIBLE else View.GONE
        val active = if (english) preview else source
        active.layoutParams = LinearLayout.LayoutParams(-1, dp(if (expanded) 210 else 90))
        active.maxLines = if (expanded) Int.MAX_VALUE else 3
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
            if (english) myanmar = editingLanguage.code == "my"
            active.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            shifted = false
            symbols = false
            emojiMode = false
            renderKeys()
        }
        if (!expanded) active.requestFocus() else active.clearFocus()
        refreshSuggestions()
    }

    private fun showFullPreview() {
        fullPreviewDialog?.dismiss()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val content = column().apply {
            setPadding(dp(16), dp(12), dp(16), dp(16))
            setBackgroundColor(surface)
        }
        val header = LinearLayout(this)
        header.addView(TextView(this).apply {
            text = direction.targetLabel
            textSize = 18f
            setTextColor(ink)
            gravity = Gravity.CENTER_VERTICAL
        }, params(2f, 48))
        header.addView(tile("⛶ Close", true) { dialog.dismiss() }.apply {
            contentDescription = "Exit full screen preview"
        }, params(1f, 48))
        content.addView(header)
        val readingText = TextView(this).apply {
            text = preview.text.toString().ifBlank { direction.previewHint }
            textSize = 20f
            setTextColor(ink)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            setTextIsSelectable(true)
            setLineSpacing(dp(5).toFloat(), 1f)
            setPadding(dp(12), dp(16), dp(12), dp(16))
        }
        content.addView(ScrollView(this).apply { addView(readingText) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this)
        actions.addView(tile("Edit", true) {
            dialog.dismiss()
            translating = true
            panel.visibility = View.VISIBLE
            showTranslationPage(true, false, preserveCursor = true)
        }, params(1f, 48))
        actions.addView(tile("Insert ↗", true) {
            dialog.dismiss()
            insertButton.performClick()
        }.apply {
            primaryStyle(this)
            isEnabled = preview.text.isNotBlank()
            alpha = if (isEnabled) 1f else 0.45f
        }, params(1f, 48))
        content.addView(actions)
        dialog.setContentView(content)
        dialog.window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
            attributes = attributes.apply { token = root.windowToken }
            addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setBackgroundDrawable(keyBackground(surface))
        }
        dialog.setOnDismissListener { fullPreviewDialog = null }
        fullPreviewDialog = dialog
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    private fun enableTapToEdit(editor: EditText, isPreview: Boolean) {
        var reviewTouch = false
        var downX = 0f
        var downY = 0f
        editor.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) refreshSuggestions()
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
        setHintTextColor(Color.rgb(132, 137, 146))
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
        refreshSuggestions()
        if (stickerMode) {
            keys.addView(StickerPanel(this, ::insertSticker, {
                startActivity(Intent(this, com.zrok.myanmaraikeyboard.StickerImportActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }, { stickerMode = false; emojiMode = true; renderKeys() },
                { stickerMode = false; emojiMode = false; renderKeys() }))
            return
        }
        if (emojiMode) {
            keys.addView(EmojiPanel(this, ::input, ::backspace, ::enableRepeatDelete, {
                stickerMode = true; emojiMode = false; renderKeys()
            }) {
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
        if (myanmar && !symbols) {
            val extras = if (shifted) "၁ ၂ ၃ ၄ ၅ ၆ ၇ ၈ ၉ ၀" else "ဈ ဝ ဋ ုံ ော ဪ ရ ဂ ဟ ၏"
            addRow(extras.split(" "))
        }
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this)
            val indentEnglish = !myanmar && !symbols && index == 1
            if (indentEnglish) line.addView(View(this), LinearLayout.LayoutParams(0, dp(typingRowHeight()), 0.5f))
            if (index == 2) line.addView(tile(if (shifted) "⇧" else "↑", true) {
                shifted = !shifted
                renderKeys()
            }.apply {
                kind = KeyboardKeyView.Kind.SHIFT
                isSelected = shifted
                contentDescription = "Shift"
                setKeyIcon(if (shifted) R.drawable.ic_key_shift_active else R.drawable.ic_key_shift, if (shifted) onAccent else ink)
            }, params(if (myanmar && !symbols) 1f else 1.5f))
            row.split(" ").forEachIndexed { keyIndex, value ->
                val key = tile(value) { input(value) }
                if (!myanmar && !symbols && index == 0) {
                    val number = ((keyIndex + 1) % 10).toString()
                    key.cornerHint = number
                    key.contentDescription = "$value; hold for $number"
                    key.setOnLongClickListener { playKeyFeedback(); input(number); true }
                }
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
            if (indentEnglish) line.addView(View(this), LinearLayout.LayoutParams(0, dp(typingRowHeight()), 0.5f))
            if (index == 2) line.addView(tile("⌫", true) { backspace() }.apply {
                kind = KeyboardKeyView.Kind.BACKSPACE
                contentDescription = "Delete"
                setKeyIcon(R.drawable.ic_key_backspace)
            }.also(::enableRepeatDelete), params(if (myanmar && !symbols) 1f else 1.5f))
            keys.addView(line)
        }
        val bottom = LinearLayout(this).apply { isBaselineAligned = false }
        bottom.addView(tile(if (symbols) "ABC" else if (myanmar) "?၁၂၃" else "?123", true) {
            symbols = !symbols; shifted = false; renderKeys()
        }.apply { kind = KeyboardKeyView.Kind.SYMBOLS }, params(1.5f))
        bottom.addView(tile(if (myanmar) "၊" else ",") { input(if (myanmar) "၊" else ",") }.apply {
            cornerHint = "☺"
            centeredHint = true
            contentDescription = "${if (myanmar) "၊" else ","}; hold for emoji"
            setOnLongClickListener { emojiMode = true; renderKeys(); true }
        }, params(1f))
        bottom.addView(tile("", true) {
            myanmar = !myanmar; shifted = false; symbols = false; renderKeys()
        }.apply {
            setKeyIcon(R.drawable.ic_language_globe)
            contentDescription = "Switch Myanmar / English; hold to use another keyboard"
            setOnLongClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .showInputMethodPicker()
                true
            }
        }, params(1f))
        bottom.addView(tile(if (myanmar) "မြန်မာ" else "English") { input(" ") }.apply { contentDescription = "Space" }, params(4f))
        bottom.addView(tile(if (myanmar) "။" else ".") { input(if (myanmar) "။" else ".") }.apply {
            setOnLongClickListener { showAlternatives(this, listOf("!", "?", ",", ".", "…", ":", ";", "\"", "'")); true }
        }, params(1f))
        val editorAction = editorActionInfo()
        bottom.addView(tile("↵", true) { input("\n") }.apply {
            kind = KeyboardKeyView.Kind.ENTER
            setKeyIcon(R.drawable.ic_key_enter, onAccent)
            contentDescription = "Enter, new line"
            if (!translating && editorAction.action != EditorInfo.IME_ACTION_NONE &&
                editorAction.action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                contentDescription = "Enter, new line; hold for ${editorAction.label}"
                setOnLongClickListener {
                    currentInputConnection?.performEditorAction(editorAction.action) == true
                }
            }
        }, params(1.5f))
        keys.addView(bottom)
        widenTypingKeys()
    }

    private data class EditorActionInfo(val action: Int, val label: String)

    private fun insertSticker(sticker: com.zrok.myanmaraikeyboard.data.StickerStore.Sticker) {
        val info = currentInputEditorInfo ?: return
        val accepts = info.contentMimeTypes.orEmpty().any { android.content.ClipDescription.compareMimeTypes(sticker.mime, it) }
        if (!accepts) {
            android.widget.Toast.makeText(this, "ဒီစာရိုက်နေရာက ပုံ / GIF မလက်ခံပါ။ ပုံလက်ခံသော chat app မှာ သုံးပါ။", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        scope.launch {
            val store = com.zrok.myanmaraikeyboard.data.StickerStore(this@TranslationInputMethodService)
            val uri = withContext(Dispatchers.IO) { runCatching { store.uri(sticker) }.getOrNull() }
            if (uri == null) {
                android.widget.Toast.makeText(this@TranslationInputMethodService, "Sticker ပုံကို ဖတ်မရပါ။", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (currentInputEditorInfo !== info) return@launch
            val content = android.view.inputmethod.InputContentInfo(uri,
                android.content.ClipDescription(sticker.label, arrayOf(sticker.mime)), null)
            val accepted = currentInputConnection?.commitContent(content,
                android.view.inputmethod.InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null) == true
            if (!accepted) android.widget.Toast.makeText(this@TranslationInputMethodService,
                "ဒီ app က sticker ကို လက်မခံပါ။", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun editorActionInfo(): EditorActionInfo {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        return when (action) {
            EditorInfo.IME_ACTION_GO -> EditorActionInfo(action, "Go")
            EditorInfo.IME_ACTION_SEARCH -> EditorActionInfo(action, "⌕")
            EditorInfo.IME_ACTION_SEND -> EditorActionInfo(action, "Send")
            EditorInfo.IME_ACTION_NEXT -> EditorActionInfo(action, "Next")
            EditorInfo.IME_ACTION_DONE -> EditorActionInfo(action, "Done")
            EditorInfo.IME_ACTION_PREVIOUS -> EditorActionInfo(action, "Prev")
            else -> EditorActionInfo(EditorInfo.IME_ACTION_NONE, "↵")
        }
    }

    private fun addRow(values: List<String>) {
        val line = LinearLayout(this)
        values.forEachIndexed { index, value ->
            val key = tile(value) { input(value) }
            key.cornerHint = ((index + 1) % 10).toString()
            alternatives[value]?.let { choices ->
                key.setOnLongClickListener { showAlternatives(key, choices); true }
            }
            line.addView(key, params())
        }
        keys.addView(line)
    }

    private fun typingRowHeight() = rowHeight + if (myanmar && !symbols) 3 else 9

    private fun TextView.setKeyIcon(drawable: Int, color: Int = ink) {
        val icon = getDrawable(drawable)!!.mutate().apply {
            setTint(color)
            setBounds(0, 0, dp(24), dp(24))
        }
        setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE)
        text = android.text.SpannableString("\uFFFC").apply {
            setSpan(android.text.style.ImageSpan(icon, android.text.style.ImageSpan.ALIGN_BOTTOM),
                0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun widenTypingKeys() {
        for (rowIndex in 0 until keys.childCount) {
            val row = keys.getChildAt(rowIndex) as? LinearLayout ?: continue
            row.isBaselineAligned = false
            row.isMotionEventSplittingEnabled = true
            for (keyIndex in 0 until row.childCount) {
                val key = row.getChildAt(keyIndex) as? KeyboardKeyView ?: continue
                val layout = key.layoutParams as LinearLayout.LayoutParams
                layout.setMargins(0, 0, 0, 0)
                layout.height = dp(typingRowHeight())
                key.layoutParams = layout
                val function = key.kind
                val fill = when (function) {
                    KeyboardKeyView.Kind.ENTER -> accent
                    KeyboardKeyView.Kind.SHIFT, KeyboardKeyView.Kind.BACKSPACE, KeyboardKeyView.Kind.SYMBOLS -> specialFill
                    else -> keyFill
                }
                val radius = if (function == KeyboardKeyView.Kind.ENTER || function == KeyboardKeyView.Kind.SYMBOLS) 28 else 6
                val background = StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_pressed), keyBackground(accent, radius))
                    addState(intArrayOf(android.R.attr.state_selected), keyBackground(accent, radius))
                    addState(intArrayOf(), keyBackground(fill, radius))
                }
                val verticalInset = if (myanmar && !symbols) 3 else 6
                key.hintTopDp = verticalInset
                key.setTextColor(if (function == KeyboardKeyView.Kind.ENTER || key.isSelected) onAccent else ink)
                key.background = InsetDrawable(background, dp(3), dp(verticalInset), dp(3), dp(verticalInset))
                key.setPadding(0, 0, 0, 0)
                if (function != KeyboardKeyView.Kind.BACKSPACE) {
                    key.setOnTouchListener { view, event ->
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) view.isPressed = true
                        // Returning false preserves click cancellation, long-press and accessibility.
                        false
                    }
                }
            }
        }
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
        refreshSuggestions()
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

    private data class SuggestionContext(val before: String, val after: String, val editor: EditText?)

    private fun suggestionContext(): SuggestionContext? {
        val info = currentInputEditorInfo ?: return null
        val kind = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        if (kind != InputType.TYPE_CLASS_TEXT || variation in listOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_URI
            ) || info.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0 ||
            info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) return null
        if (translating) {
            val editor = if (editingPreview) preview else source
            val cursor = editor.selectionStart
            if (cursor < 0 || cursor != editor.selectionEnd) return null
            return SuggestionContext(editor.text.substring(maxOf(0, cursor - 128), cursor),
                editor.text.substring(cursor, minOf(cursor + 1, editor.text.length)), editor)
        }
        val connection = currentInputConnection ?: return null
        if (!connection.getSelectedText(0).isNullOrEmpty()) return null
        return SuggestionContext(connection.getTextBeforeCursor(128, 0)?.toString() ?: return null,
            connection.getTextAfterCursor(1, 0)?.toString() ?: return null, null)
    }

    private val updateSuggestions = Runnable {
        val context = if (suggestionsEnabled && !myanmar && !symbols && !emojiMode && !(translating && reviewing)) suggestionContext() else null
        val local = context?.let { WordSuggestions.suggest(it.before, it.after, myanmar) }
        val checked = local?.let { systemWords.cached(it.prefix) }
        val verified = local?.let { match ->
            when (checked?.correct) {
                true -> match
                false -> match.copy(words = match.words.filterNot { it.equals(match.prefix, true) }, isKnownWord = false)
                null -> match
            }
        }
        val online = if (onlineSuggestionsEnabled && local != null) onlineWords.cached(local.prefix).orEmpty() else emptyList()
        val match = verified?.let { WordSuggestions.mergeOnline(it, checked?.words.orEmpty() + online) }
        renderSuggestions(context, match)
        requestBetterSuggestions(context, local)
    }

    private fun renderSuggestions(context: SuggestionContext?, match: WordSuggestions.Match?) {
        if (::suggestionStrip.isInitialized) {
            val state = context to match
            if (state == lastSuggestionState) return
            if (lastSuggestionState?.first != context) suggestionScroll.scrollTo(0, 0)
            lastSuggestionState = state
            suggestionStrip.removeAllViews()
            suggestionScroll.visibility = if (match?.words.isNullOrEmpty()) View.GONE else View.VISIBLE
            match?.words?.forEach { word ->
                suggestionStrip.addView(tile(word) {
                    if (context != null && suggestionContext() == context) {
                        val prefix = match.prefix
                        val editor = context.editor
                        if (editor != null) {
                            val cursor = editor.selectionStart
                            val before = editor.text.toString()
                            editor.text.replace(cursor - prefix.length, cursor, word)
                            editor.setSelection(cursor - prefix.length + word.length)
                            rememberLocalUndo(editor, before, cursor, cursor, editor.text.toString())
                        } else {
                            currentInputConnection?.let { connection ->
                                connection.beginBatchEdit()
                                try {
                                    connection.finishComposingText()
                                    if (prefix.isEmpty() || connection.deleteSurroundingText(prefix.length, 0)) connection.commitText(word, 1)
                                } finally { connection.endBatchEdit() }
                            }
                        }
                        pendingPreVowel = false
                        refreshSuggestions()
                    }
                }.apply {
                    textSize = 15f
                    contentDescription = "Use suggestion $word"
                    if (match.isKnownWord && word.equals(match.prefix, ignoreCase = true)) {
                        text = "✓ $word"
                        setTextColor(accent)
                        contentDescription = "Known English word $word"
                    }
                    setAutoSizeTextTypeUniformWithConfiguration(11, 17, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
                }, LinearLayout.LayoutParams(
                    ((resources.displayMetrics.widthPixels - root.paddingLeft - root.paddingRight) / 3 - dp(4)).coerceAtLeast(dp(80)), dp(44)
                ).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) })
            }
        }
    }

    private fun cancelSuggestionRequests() {
        suggestionGeneration++
        suggestionRequest?.cancel()
        suggestionRequest = null
        requestedSuggestionContext = null
        systemWords.cancel()
    }

    private fun hasInternet(): Boolean = runCatching {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)

    private fun requestBetterSuggestions(context: SuggestionContext?, match: WordSuggestions.Match?) {
        if (!inputVisible || context == null || match == null || !OnlineWordSuggestions.eligible(match.prefix)) {
            cancelSuggestionRequests()
            return
        }
        if (requestedSuggestionContext == context) return
        cancelSuggestionRequests()
        requestedSuggestionContext = context
        val generation = suggestionGeneration
        suggestionRequest = scope.launch {
            delay(150)
            if (suggestionContext() != context || !inputVisible) return@launch
            systemWords.request(match.prefix)
            delay(300)
            if (!onlineSuggestionsEnabled || suggestionContext() != context || !inputVisible || !hasInternet()) return@launch
            if (onlineWords.cached(match.prefix) != null) return@launch
            val words = onlineWords.fetch(match.prefix)
            ensureActive()
            if (words != null && generation == suggestionGeneration && inputVisible && suggestionContext() == context) refreshSuggestions()
        }
    }

    private fun refreshSuggestions() {
        if (!::root.isInitialized) return
        root.removeCallbacks(updateSuggestions)
        root.post(updateSuggestions)
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        refreshSuggestions()
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
        inputVisible = false
        cancelSuggestionRequests()
        deleteRepeat?.cancel()
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        inputVisible = false
        cancelSuggestionRequests()
        deleteRepeat?.cancel()
        fullPreviewDialog?.dismiss()
        lastSuggestionState = null
        if (::root.isInitialized) root.removeCallbacks(updateSuggestions)
        if (::suggestionScroll.isInitialized) suggestionScroll.visibility = View.GONE
        super.onFinishInput()
    }

    private fun tile(label: String, special: Boolean = false, action: () -> Unit) = KeyboardKeyView(this).apply {
        // Combining marks need a visible base on keycaps; commit only the original label.
        text = if (label.length == 1 && Character.getType(label[0]) in listOf(
            Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt()
        )) "\u25CC$label" else label
        textSize = if (label.length > 2) 13f else this@TranslationInputMethodService.textSize.toFloat()
        setSingleLine(true)
        if (label.length <= 2) {
            val maxSize = if (label == "ဪ") minOf(16, this@TranslationInputMethodService.textSize) else this@TranslationInputMethodService.textSize
            setAutoSizeTextTypeUniformWithConfiguration(if (label == "ဪ") 9 else 11, maxSize, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
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
            playKeyFeedback(label)
            action()
        }
    }

    private fun initializeKeySound() {
        val preferences = AppPreferences(applicationContext)
        selectedTone = preferences.keyboardTone
        vibrationDuration = preferences.vibrationDuration
        soundVolume = preferences.keyboardSoundVolume
        systemHaptic = preferences.systemKeyboardHaptic
    }

    private fun playKeyFeedback(label: String = "") {
        if (keyVibrationEnabled) {
            feedback.vibrate(vibrationDuration, root, systemHaptic)
        }
        if (keySoundEnabled) feedback.sound(selectedTone, label, soundVolume)
    }

    private fun keyBackground(fill: Int, radius: Int = 12) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
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
        inputVisible = true
        cancelSuggestionRequests()
        if (!::source.isInitialized || !::keys.isInitialized) return
        val configured = AppPreferences(applicationContext).translationDirection
        if (configured != configuredDirection) {
            request?.cancel()
            cancelPendingRefresh()
            translationGeneration++
            rememberReviewedPair()
            pairDrafts[direction] = source.text.toString() to preview.text.toString()
            direction = configured
            configuredDirection = configured
            directionKey.text = direction.label
            val draft = pairDrafts[direction]
            followSourceEdits = false
            displayedPair = null
            withoutSourceWatch {
                source.setText(draft?.first.orEmpty())
                preview.setText(draft?.second.orEmpty())
            }
            source.hint = direction.sourceHint
            preview.hint = direction.previewHint
            status.text = "Translation languages updated"
            showTranslationPage(false, false)
        }
        val preferences = AppPreferences(applicationContext)
        keySoundEnabled = preferences.keyboardSoundEnabled
        suggestionsEnabled = preferences.suggestionsEnabled
        onlineSuggestionsEnabled = preferences.onlineSuggestionsEnabled
        refreshSuggestions()
        toneKey.isEnabled = !preferences.azureEnabled
        toneKey.text = if (preferences.azureEnabled) "Azure" else tone.label
        keyVibrationEnabled = preferences.keyboardVibrationEnabled
        initializeKeySound()
        if (rowHeight != preferences.keyboardRowHeight || textSize != preferences.keyboardTextSize) {
            rowHeight = preferences.keyboardRowHeight
            textSize = preferences.keyboardTextSize
        }
        // The active text field can expose a different IME action, such as Go or Search.
        renderKeys()
    }

    override fun onDestroy() {
        cancelSuggestionRequests()
        systemWords.close()
        onlineWords.clear()
        fullPreviewDialog?.dismiss()
        translationMemory.clear()
        characterPopup?.dismiss()
        toneGenerator?.release()
        toneGenerator = null
        feedback.release()
        scope.cancel()
        super.onDestroy()
    }
}
