package com.sed.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 32, 24, 24); gravity = Gravity.CENTER_HORIZONTAL }
        root.addView(TextView(this).apply { text = "SED Keyboard"; textSize = 28f; setTextColor(0xff202124.toInt()) })
        root.addView(TextView(this).apply { text = "A clean, lightweight keyboard for everyday typing."; textSize = 14f; setPadding(0, 8, 0, 28) })
        fun action(label: String, run: () -> Unit) { root.addView(Button(this).apply { text = label; setOnClickListener { run() } }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 }) }
        action("1  Enable SED Keyboard") { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        action("2  Choose SED Keyboard") { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
        root.addView(TextView(this).apply { text = "Features\n• English + বাংলা layouts\n• Word suggestions and next-word hints\n• Number and symbol pages\n• Emoji quick panel\n• Optional key vibration\n• No network permission, no ads, no analytics\n\nDesigned to stay small. Suggestions are processed on-device."; textSize = 15f; setPadding(0, 20, 0, 0) })
        setContentView(root)
    }
}
