package com.sed.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("keyboard", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = prefs.getBoolean("dark", false)
        val bg = if (dark) 0xff202124.toInt() else 0xfff4f6f8.toInt()
        val fg = if (dark) 0xffffffff.toInt() else 0xff202124.toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(bg)
        }
        root.addView(TextView(this).apply {
            text = "SED Keyboard"
            textSize = 28f
            setTextColor(fg)
        })
        root.addView(TextView(this).apply {
            text = "Lightweight typing. Your keystrokes stay on your device."
            textSize = 14f
            setTextColor(fg)
            setPadding(0, 8, 0, 24)
        })

        fun action(label: String, run: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { run() }
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        }

        action("1  Enable SED Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        action("2  Choose SED Keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }

        val darkSwitch = Switch(this).apply {
            text = "Dark keyboard"
            setTextColor(fg)
            isChecked = prefs.getBoolean("dark", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("dark", checked).apply()
            }
        }
        root.addView(darkSwitch, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 16; bottomMargin = 12 })

        val vibrationSwitch = Switch(this).apply {
            text = "Key vibration"
            setTextColor(fg)
            isChecked = prefs.getBoolean("vibration", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("vibration", checked).apply()
            }
        }
        root.addView(vibrationSwitch, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply {
            text = "Included\n• English and Bangla character layouts\n• On-device word completion\n• Number and symbol page\n• Emoji panel\n• Dark appearance and optional vibration\n• No internet permission, ads, or analytics\n\nNot included yet: Bangla phonetic typing, voice typing, glide typing, cloud sync, or Gboard-level prediction."
            textSize = 15f
            setTextColor(fg)
            setPadding(0, 28, 0, 0)
        })
        setContentView(root)
    }
}
