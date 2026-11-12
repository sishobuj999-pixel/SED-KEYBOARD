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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(TextView(this).apply {
            text = "SED Keyboard"
            textSize = 28f
            setTextColor(0xff202124.toInt())
        })
        root.addView(TextView(this).apply {
            text = "A lightweight keyboard for everyday typing."
            textSize = 14f
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

        root.addView(TextView(this).apply {
            text = "Keyboard preferences"
            textSize = 18f
            setTextColor(0xff202124.toInt())
            setPadding(0, 20, 0, 8)
        })

        val darkSwitch = Switch(this).apply {
            text = "Dark keyboard"
            isChecked = prefs.getBoolean("dark", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("dark", checked).apply()
            }
        }
        root.addView(darkSwitch, LinearLayout.LayoutParams(-1, -2))

        val vibrationSwitch = Switch(this).apply {
            text = "Key vibration"
            isChecked = prefs.getBoolean("vibration", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("vibration", checked).apply()
            }
        }
        root.addView(vibrationSwitch, LinearLayout.LayoutParams(-1, -2))

        root.addView(TextView(this).apply {
            text = "Included: English and Bangla character layouts, basic word completion, symbols, emoji panel, shift, delete and editor action keys. No internet permission, ads or analytics. Bangla phonetic typing, voice typing and glide typing are not included yet."
            textSize = 14f
            setTextColor(0xff444444.toInt())
            setPadding(0, 24, 0, 0)
        })
        setContentView(root)
    }
}
