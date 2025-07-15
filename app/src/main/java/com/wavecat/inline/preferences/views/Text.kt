@file:Suppress("unused", "ViewConstructor")

package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.wavecat.inline.preferences.Preference

/**
 * Simple text view implementation for preferences display.
 *
 * Extends AppCompatTextView to provide static text display in preference
 * screens. Used for labels, descriptions, or informational text that
 * doesn't require user interaction or persistence.
 *
 * @param context The Android context
 * @param text The text content to display
 * @see AppCompatTextView
 * @see Preference
 */
class Text(context: Context, text: String) : AppCompatTextView(context), Preference {
    init {
        setText(text)
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View = this
}
