package com.sed.keyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodService
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
    private val enRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val bnRows = listOf("কখগঘঙচছজঝঞ", "টঠডঢণতথদধন", "পফবভমযরলশষসহ")
    private val symbolsRows = listOf("1234567890", "@#$%&-*+()", "!?:;'/\"")
    private val emojiRows = listOf("😀😂🥰😍😎😭😊😉", "❤️👍🙏🔥🎉✨💯", "🐱🐶🌸🍕⚽🚗🌍")

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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dark() = prefs.getBoolean("dark", false)

    private fun buildKeyboard(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(5), dp(3), dp(5))
            setBackgroundColor(if (dark()) 0xff202124.toInt() else 0xffdce1e8.toInt())
        }
        refresh()
        return root
    }

    private fun key(label: String, weight: Float = 1f, action: (() -> Unit)? = null): Button {
        return Button(this).apply {
            text = label
            textSize = if (label.length > 2) 12f else 18f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            setTextColor(if (dark()) Color.WHITE else 0xff202124.toInt())
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (dark()) 0xff3c4043.toInt() else Color.WHITE))
            setOnClickListener {
                if (prefs.getBoolean("vibration", true)) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                action?.invoke()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(47), weight).apply {
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
        }
    }

    private fun addCharacterRow(labels: String, handler: (String) -> Unit) {
        val line = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        labels.forEach { ch -> line.addView(key(ch.toString()) { handler(ch.toString()) }) }
        root.addView(line)
    }

    private fun refresh() {
        if (!::root.isInitialized) return
        root.removeAllViews()
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(top, LinearLayout.LayoutParams(-1, dp(39)))

        val candidateWords = if (composing.isBlank()) emptyList() else {
            val matches = if (bangla) emptyList() else listOf("the", "you", "and", "that", "this", "with", "have", "আমি", "তুমি", "এবং")
                .filter { it.startsWith(composing, true) && it.length > composing.length }
                .take(2)
            listOf(composing) + matches
        }
        if (candidateWords.isEmpty()) {
            top.addView(suggestion(if (bangla) "বাংলা" else "English") { bangla = !bangla; composing = ""; refresh() }, LinearLayout.LayoutParams(0, -1, 1f))
            top.addView(suggestion("⚙") {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }, LinearLayout.LayoutParams(0, -1, .5f))
            top.addView(suggestion("😊") { emojiPage = !emojiPage; symbols = false; refresh() }, LinearLayout.LayoutParams(0, -1, .5f))
        } else {
            candidateWords.forEachIndexed { index, word ->
                top.addView(suggestion(word) {
                    val suffix = word.removePrefix(composing)
                    if (suffix.isNotEmpty()) currentInputConnection?.commitText(suffix, 1)
                    currentInputConnection?.commitText(" ", 1)
                    composing = ""
                    refresh()
                }, LinearLayout.LayoutParams(0, -1, 1f))
            }
            top.addView(suggestion("✕") { composing = ""; refresh() }, LinearLayout.LayoutParams(0, -1, .5f))
        }

        when {
            emojiPage -> {
                emojiRows.forEach { row ->
                    val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
                    row.forEach { emoji ->
                        line.addView(key(emoji.toString()) { currentInputConnection?.commitText(emoji.toString(), 1) })
                    }
                    root.addView(line)
                }
            }
            symbols -> symbolsRows.forEach { addCharacterRow(it) { commit(it) } }
            else -> {
                val rows = if (bangla) bnRows else enRows
                rows.forEachIndexed { index, chars ->
                    val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
                    if (index == 2) line.addView(key("⇧", 1.25f) { shifted = !shifted; refresh() })
                    chars.forEach { ch ->
                        val value = if (!bangla && shifted) ch.uppercase() else ch.toString()
                        line.addView(key(value) { commit(value) })
                    }
                    if (index == 2) {
                        val delete = key("⌫", 1.25f) {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                            composing = composing.dropLast(1)
                            refresh()
                        }
                        delete.setOnLongClickListener {
                            currentInputConnection?.deleteSurroundingText(1000, 0)
                            composing = ""
                            refresh()
                            true
                        }
                        line.addView(delete)
                    }
                    root.addView(line)
                }
            }
        }

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        bottom.addView(key(if (bangla) "EN" else "বাংলা", 1.2f) { bangla = !bangla; composing = ""; refresh() })
        bottom.addView(key(if (emojiPage) "ABC" else "😊", .9f) { emojiPage = !emojiPage; symbols = false; refresh() })
        bottom.addView(key(if (symbols) "ABC" else "?123", 1.05f) { symbols = !symbols; emojiPage = false; refresh() })
        bottom.addView(key(",", .65f) { commit(",") })
        bottom.addView(key("English / বাংলা", 3.4f) { currentInputConnection?.commitText(" ", 1); composing = ""; refresh() })
        bottom.addView(key(".", .65f) { commit(".") })
        bottom.addView(key("↵", .85f) { performEditorAction() })
        root.addView(bottom)
    }

    private fun suggestion(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(if (dark()) Color.WHITE else Color.DKGRAY)
        setOnClickListener { action() }
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
            else -> null
        }
        if (handled != true) currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        if (handled != true) currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }
}
