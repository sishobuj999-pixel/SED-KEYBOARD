package com.sed.keyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
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
    private lateinit var suggestions: LinearLayout

    private val englishRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val banglaRows = listOf("ৌৈাীূবহগদ", "োে্িুপটচজ", "কতডরনসলম")
    private val symbolsRows = listOf("1234567890", "@#\$%&-*+()", "!?:;'/\"")
    private val emojiRows = listOf("😀😂🥰😍😊😭😎🤔", "👍🙏❤️🔥🎉✨💯🤝", "🌸🌙☀️🍎⚽🚗🐱🐶")
    private val dictionary = listOf(
        "the", "there", "their", "then", "this", "that", "thanks", "think",
        "you", "your", "you're", "yes", "yeah", "and", "are", "about",
        "আমি", "আমার", "আমাকে", "তুমি", "তোমার", "আপনি", "আপনার", "বাংলা",
        "ভালো", "ধন্যবাদ", "হবে", "কেন", "কিছু", "ঠিক", "আছে", "এবং"
    )

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("keyboard", Context.MODE_PRIVATE)
    }

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composing = ""
        shifted = false
        refresh()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun dark() = prefs.getBoolean("dark", false)

    private fun buildKeyboard(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
            setBackgroundColor(if (dark()) 0xff202124.toInt() else 0xffdce1e8.toInt())
        }
        refresh()
        return root
    }

    private fun key(label: String, weight: Float = 1f, special: Boolean = false, action: (() -> Unit)? = null): Button {
        val isDark = dark()
        return Button(this).apply {
            text = label
            textSize = if (label.length > 2) 12f else 18f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            stateListAnimator = null
            setPadding(0, 0, 0, 0)
            setTextColor(if (isDark) Color.WHITE else 0xff202124.toInt())
            val fill = when {
                special && isDark -> 0xff59616b.toInt()
                special -> 0xffb9c4d2.toInt()
                isDark -> 0xff3c4043.toInt()
                else -> Color.WHITE
            }
            background = GradientDrawable().apply {
                setColor(fill)
                cornerRadius = dp(6).toFloat()
            }
            setOnClickListener {
                if (prefs.getBoolean("vibration", false)) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                action?.invoke()
            }
            layoutParams = LinearLayout.LayoutParams(0, dp(47), weight).apply {
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
        }
    }

    private fun addCharacterRow(labels: String, transform: (Char) -> String = { it.toString() }) {
        val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        labels.forEach { char ->
            val value = transform(char)
            line.addView(key(value) { typeCharacter(value) })
        }
        root.addView(line)
    }

    private fun refresh() {
        if (!::root.isInitialized) return
        root.removeAllViews()

        suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(suggestions, LinearLayout.LayoutParams(-1, dp(38)))
        buildSuggestionBar()

        when {
            emojiPage -> buildEmojiPage()
            symbols -> buildSymbolPage()
            else -> buildLetterPage()
        }
        buildBottomRow()
    }

    private fun buildSuggestionBar() {
        val words = if (composing.isBlank()) {
            listOf(if (bangla) "বাংলা" else "English", "😊", "⚙")
        } else {
            val matches = dictionary.filter { it.startsWith(composing, ignoreCase = true) && it.length > composing.length }.take(2)
            listOf(composing) + matches + listOf("×")
        }
        words.forEachIndexed { index, word ->
            val item = TextView(this).apply {
                text = word
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(if (dark()) Color.WHITE else 0xff30343b.toInt())
                setOnClickListener {
                    when (word) {
                        "😊" -> { emojiPage = true; symbols = false; refresh() }
                        "⚙" -> openSettings()
                        "×" -> { composing = ""; refresh() }
                        else -> if (composing.isNotBlank()) {
                            val suffix = word.removePrefix(composing)
                            currentInputConnection?.commitText(suffix + " ", 1)
                            composing = ""
                            refresh()
                        } else {
                            bangla = !bangla
                            refresh()
                        }
                    }
                }
            }
            suggestions.addView(item, LinearLayout.LayoutParams(0, -1, if (index == 0) 1.5f else 1f))
        }
    }

    private fun buildLetterPage() {
        val rows = if (bangla) banglaRows else englishRows
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
            if (index == 2) line.addView(key("⇧", 1.25f, true) {
                shifted = !shifted
                refresh()
            })
            row.forEach { char ->
                val value = if (!bangla && shifted) char.uppercase() else char.toString()
                line.addView(key(value) { typeCharacter(value) })
            }
            if (index == 2) line.addView(key("⌫", 1.25f, true) { deleteCharacter() })
            root.addView(line)
        }
    }

    private fun buildSymbolPage() {
        symbolsRows.forEach { row -> addCharacterRow(row) }
    }

    private fun buildEmojiPage() {
        emojiRows.forEach { row ->
            val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
            row.forEach { character ->
                val emoji = character.toString()
                line.addView(key(emoji, 1f) {
                    currentInputConnection?.commitText(emoji, 1)
                })
            }
            root.addView(line)
        }
    }

    private fun buildBottomRow() {
        val line = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        line.addView(key(if (bangla) "EN" else "বাংলা", 1.25f, true) {
            bangla = !bangla
            symbols = false
            emojiPage = false
            composing = ""
            refresh()
        })
        line.addView(key(if (symbols || emojiPage) "ABC" else "?123", 1.15f, true) {
            symbols = false
            emojiPage = false
            refresh()
        })
        line.addView(key("😊", 1f, true) {
            emojiPage = !emojiPage
            symbols = false
            refresh()
        })
        line.addView(key(",", .65f) { typeCharacter(",") })
        val space = key(if (bangla) "বাংলা" else "English", 3.2f) {
            currentInputConnection?.commitText(" ", 1)
            composing = ""
            refresh()
        }
        space.setOnLongClickListener {
            bangla = !bangla
            composing = ""
            refresh()
            true
        }
        line.addView(space)
        line.addView(key(".", .65f) { typeCharacter(".") })
        line.addView(key("↵", 1f, true) { sendEnter() })
        root.addView(line)
    }

    private fun typeCharacter(value: String) {
        currentInputConnection?.commitText(value, 1)
        if (value.length == 1 && value[0].isLetter()) composing += value else composing = ""
        if (shifted) shifted = false
        refresh()
    }

    private fun deleteCharacter() {
        val connection = currentInputConnection ?: return
        connection.deleteSurroundingText(1, 0)
        if (composing.isNotEmpty()) composing = composing.dropLast(1)
        refresh()
    }

    private fun sendEnter() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
