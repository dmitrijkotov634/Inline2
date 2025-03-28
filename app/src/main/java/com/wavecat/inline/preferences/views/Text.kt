@file:Suppress("unused", "ViewConstructor")

package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.wavecat.inline.preferences.Preference

class Text(context: Context, text: String) : AppCompatTextView(context), Preference {
    init {
        setText(text)
    }

    override fun getView(preferences: SharedPreferences?): View = this
}
