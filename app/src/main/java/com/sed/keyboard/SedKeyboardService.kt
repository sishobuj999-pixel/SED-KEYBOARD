package com.sed.keyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SedKeyboardService : InputMethodService() {
    private var bangla = false
    private var symbols = false
    private var emojiPage = false
    private var shifted = false
    private var composing = ""
    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var deleting = false
    private var deleteStarted = false

    private val enRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val bnRows = listOf("কখগঘঙচছজঝঞ", "টঠডঢণতথদধন", "পফবভমযরলশষসহ")
    private val symbolsRows = listOf("1234567890", "@#$%&-*+()", "!?:;'/\"")
    private val emojiRows = listOf(
        listOf("😀","😂","🥰","😍","😎","😭","😊","😉"),
        listOf("❤️","👍","🙏","🔥","🎉","✨","💯"),
        listOf("🐱","🐶","🌸","🍕","⚽","🚗","🌍")
    )

    private val deleteRepeater = object : Runnable {
        override fun run() {
            if (!deleting) return
            deleteOnce()
            handler.postDelayed(this, 45)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("keyboard", Context.MODE_PRIVATE)
    }

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composing = ""
        symbols = false
        emojiPage = false
        shifted = false
        refresh()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopDelete()
        super.onFinishInputView(finishingInput)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dark() = prefs.getBoolean("dark", false)
    private fun transparent() = prefs.getBoolean("transparent", true)

    private fun keyboardBg(): Int {
        if (transparent()) return Color.TRANSPARENT
        return if (dark()) 0xE6000000.toInt() else 0xF0F4F7FA.toInt()
    }

    private fun buildKeyboard(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(6))
            setBackgroundColor(keyboardBg())
        }
        refresh()
        return root
    }

    private fun key(label: String, weight: Float = 1f, action: (() -> Unit)? = null): Button {
        val keyBg = if (transparent()) 0x22000000 else if (dark()) 0xff3c4043.toInt() else 0xFFF7F8FA.toInt()
        return Button(this).apply {
            text = label
            textSize = when {
                label.length > 8 -> 11f
                label.length > 2 -> 15f
                else -> 20f
            }
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            setTextColor(if (dark() || transparent()) Color.WHITE else 0xff202124.toInt())
            backgroundTintList = android.content.res.ColorStateList.valueOf(keyBg)
            elevation = 0f
            stateListAnimator = null
            setOnClickListener {
                if (prefs.getBoolean("vibration", true)) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                action?.invoke()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(56), weight).apply {
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
        }
    }

    private fun addCharacterRow(labels: String, handlerAction: (String) -> Unit) {
        val line = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        labels.forEach { ch -> line.addView(key(ch.toString()) { handlerAction(ch.toString()) }) }
        root.addView(line)
    }

    private fun refresh() {
        if (!::root.isInitialized) return
        root.removeAllViews()
        root.setBackgroundColor(keyboardBg())

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(top, LinearLayout.LayoutParams(-1, dp(42)))

        val candidateWords = if (composing.isBlank() || bangla) emptyList() else {
            val dictionary = listOf("the","there","their","this","that","these","you","your","and","have","with","for","from")
            listOf(composing) + dictionary.filter { it.startsWith(composing, true) && it != composing }.take(2)
        }

        if (candidateWords.isEmpty()) {
            top.addView(suggestion(if (bangla) "বাংলা" else "English") { bangla = !bangla; composing = ""; refresh() }, LinearLayout.LayoutParams(0, -1, 1f))
            top.addView(suggestion("⚙") { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }, LinearLayout.LayoutParams(0, -1, .45f))
            top.addView(suggestion("😊") { emojiPage = !emojiPage; symbols = false; refresh() }, LinearLayout.LayoutParams(0, -1, .45f))
        } else {
            candidateWords.forEach { word ->
                top.addView(suggestion(word) { acceptSuggestion(word) }, LinearLayout.LayoutParams(0, -1, 1f))
            }
        }

        when {
            emojiPage -> emojiRows.forEach { row ->
                val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
                row.forEach { emoji -> line.addView(key(emoji) { currentInputConnection?.commitText(emoji, 1) }) }
                root.addView(line)
            }
            symbols -> symbolsRows.forEach { addCharacterRow(it) { commit(it) } }
            else -> {
                val rows = if (bangla) bnRows else enRows
                rows.forEachIndexed { index, chars ->
                    val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
                    if (index == 2) line.addView(key("⇧", 1.2f) { shifted = !shifted; refresh() })
                    chars.forEach { ch ->
                        val value = if (!bangla && shifted) ch.uppercase() else ch.toString()
                        line.addView(key(value) { commit(value) })
                    }
                    if (index == 2) line.addView(deleteKey())
                    root.addView(line)
                }
            }
        }

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        bottom.addView(key(if (bangla) "EN" else "বাংলা", 1.25f) { bangla = !bangla; composing = ""; refresh() })
        bottom.addView(key(if (emojiPage) "ABC" else "😊", .95f) { emojiPage = !emojiPage; symbols = false; refresh() })
        bottom.addView(key(if (symbols) "ABC" else "?123", 1.1f) { symbols = !symbols; emojiPage = false; refresh() })
        bottom.addView(key(",", .72f) { commit(",") })
        bottom.addView(key("space", 3.9f) { currentInputConnection?.commitText(" ", 1); composing = "" })
        bottom.addView(key(".", .72f) { commit(".") })
        bottom.addView(key("↵", 1.0f) { performEditorAction() })
        root.addView(bottom)
    }

    private fun deleteKey(): Button {
        return key("⌫", 1.2f) { }.apply {
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        deleteStarted = false
                        deleteOnce()
                        deleting = true
                        handler.postDelayed({
                            if (deleting) {
                                deleteStarted = true
                                handler.post(deleteRepeater)
                            }
                        }, 350)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        stopDelete()
                        true
                    }
                    else -> true
                }
            }
        }
    }

    private fun stopDelete() {
        deleting = false
        deleteStarted = false
        handler.removeCallbacks(deleteRepeater)
    }

    private fun deleteOnce() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (composing.isNotEmpty()) composing = composing.dropLast(1)
    }

    private fun suggestion(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(if (dark() || transparent()) Color.WHITE else Color.DKGRAY)
        setOnClickListener { action() }
    }

    private fun acceptSuggestion(word: String) {
        if (composing.isNotEmpty()) {
            currentInputConnection?.deleteSurroundingText(composing.length, 0)
        }
        currentInputConnection?.commitText(word + " ", 1)
        composing = ""
        refresh()
    }

    private fun commit(value: String) {
        currentInputConnection?.commitText(value, 1)
        if (!bangla && value.length == 1 && value[0].isLetter()) composing += value.lowercase()
        else composing = ""
        if (shifted) shifted = false
        refresh()
    }

    private fun performEditorAction() {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val handled = when (action) {
            EditorInfo.IME_ACTION_GO -> currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_GO)
            EditorInfo.IME_ACTION_SEARCH -> currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            EditorInfo.IME_ACTION_SEND -> currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_SEND)
            EditorInfo.IME_ACTION_NEXT -> currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            EditorInfo.IME_ACTION_DONE -> currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
            else -> false
        }
        if (handled != true) {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }
}
