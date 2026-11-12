package com.sed.keyboard

import android.inputmethodservice.InputMethodService
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.HapticFeedbackConstants
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.content.SharedPreferences

class SedKeyboardService : InputMethodService() {
    private var bangla = false
    private var symbols = false
    private var shifted = false
    private var composing = ""
    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var suggestions: LinearLayout
    private val enRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val bnRows = listOf("কখগঘঙচছজঝঞ", "টঠডঢণতথদধন", "পফবভমযরলশষসহ")
    private val common = listOf("the", "you", "and", "to", "is", "a", "I", "it", "in", "that", "for", "আমি", "তুমি", "এবং", "না", "হবে")
    override fun onCreate() { super.onCreate(); prefs = getSharedPreferences("keyboard", Context.MODE_PRIVATE) }
    override fun onCreateInputView(): View { return buildKeyboard() }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { super.onStartInputView(info, restarting); composing = ""; refresh() }

    private fun buildKeyboard(): View {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(3, 5, 3, 5); setBackgroundColor(if (prefs.getBoolean("dark", false)) 0xff202124.toInt() else 0xffdce1e8.toInt()) }
        suggestions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; }
        root.addView(suggestions, LinearLayout.LayoutParams(-1, dp(42)))
        refresh()
        return root
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun key(label: String, weight: Float = 1f, action: (() -> Unit)? = null): Button {
        val dark = prefs.getBoolean("dark", false)
        return Button(this).apply {
            text = label; textSize = if (label.length > 2) 12f else 18f
            isAllCaps = false; minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            setPadding(0, 0, 0, 0)
            setTextColor(if (dark) Color.WHITE else 0xff202124.toInt())
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(if (dark) 0xff3c4043.toInt() else Color.WHITE))
            setOnClickListener { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); action?.invoke() }
            layoutParams = LinearLayout.LayoutParams(0, dp(48), weight).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
        }
    }
    private fun row(parent: LinearLayout, labels: String, handler: (String) -> Unit) {
        val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        labels.forEach { c -> line.addView(key(c.toString()) { handler(c.toString()) }) }
        parent.addView(line)
    }
    private fun refresh() {
        if (!::root.isInitialized) return
        root.removeAllViews()
        suggestions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        root.addView(suggestions, LinearLayout.LayoutParams(-1, dp(40)))
        val candidates = if (composing.isBlank()) listOf(if (bangla) "বাংলা" else "English", "😊", "⚙") else listOf(composing, common.firstOrNull { it.startsWith(composing, true) && it.length > composing.length } ?: "", "✕")
        candidates.filter { it.isNotEmpty() }.forEach { word ->
            val t = TextView(this).apply { text = word; textSize = 15f; gravity = Gravity.CENTER; setTextColor(if (prefs.getBoolean("dark", false)) Color.WHITE else Color.DKGRAY); setOnClickListener {
                when (word) { "😊" -> { symbols = true; refresh() }; "⚙" -> { requestShowSelf(0) }; "✕" -> { composing = ""; refresh() }; else -> if (composing.isNotBlank()) { currentInputConnection?.commitText(word.removePrefix(composing) + " ", 1); composing = ""; refresh() } }
            } }
            suggestions.addView(t, LinearLayout.LayoutParams(0, -1, 1f))
        }
        if (symbols) {
            row(root, "1234567890") { commit(it) }
            row(root, "@#$%&-*+()") { commit(it) }
            row(root, "!?:;'/\"") { commit(it) }
        } else {
            val rows = if (bangla) bnRows else enRows
            rows.forEachIndexed { index, chars ->
                val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
                if (index == 2) line.addView(key("⇧", 1.3f) { shifted = !shifted; refresh() })
                chars.forEach { ch -> line.addView(key(if (!bangla && shifted) ch.uppercase() else ch.toString()) { commit(if (!bangla && shifted) ch.uppercase() else ch.toString()) }) }
                if (index == 2) line.addView(key("⌫", 1.3f) { currentInputConnection?.deleteSurroundingText(1, 0); composing = composing.dropLast(1); refresh() })
                root.addView(line)
            }
        }
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        bottom.addView(key(if (bangla) "EN" else "বাংলা", 1.35f) { bangla = !bangla; composing = ""; refresh() })
        bottom.addView(key(if (symbols) "ABC" else "?123", 1.2f) { symbols = !symbols; refresh() })
        bottom.addView(key("😊", 1f) { symbols = !symbols; refresh() })
        bottom.addView(key(",", .75f) { commit(",") })
        bottom.addView(key("English / বাংলা", 4f) { currentInputConnection?.commitText(" ", 1); composing = ""; refresh() })
        bottom.addView(key(".", .75f) { commit(".") })
        bottom.addView(key("↵", 1f) { currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)); currentInputConnection?.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER)) })
        root.addView(bottom)
    }
    private fun commit(s: String) {
        currentInputConnection?.commitText(s, 1)
        if (s.length == 1 && s[0].isLetter()) composing += s else composing = ""
        if (shifted) shifted = false
        refresh()
    }
}
