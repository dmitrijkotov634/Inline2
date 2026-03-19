@file:Suppress("unused", "ViewConstructor")

package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.wavecat.inline.preferences.Preference
import androidx.core.graphics.toColorInt

/**
 * Text view for preferences with builder-pattern styling.
 *
 * @param context The Android context
 * @param text The text content to display
 */
class Text(context: Context, text: String) : AppCompatTextView(context), Preference {
    init {
        setText(text)
    }

    fun size(sp: Float): Text {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        return this
    }

    fun size(sp: Int): Text = size(sp.toFloat())

    fun bold(): Text {
        setTypeface(typeface, Typeface.BOLD)
        return this
    }

    fun italic(): Text {
        setTypeface(typeface, Typeface.ITALIC)
        return this
    }

    fun boldItalic(): Text {
        setTypeface(typeface, Typeface.BOLD_ITALIC)
        return this
    }

    fun color(color: Int): Text {
        setTextColor(color)
        return this
    }

    fun color(color: String): Text {
        setTextColor(color.toColorInt())
        return this
    }

    fun center(): Text {
        textAlignment = TEXT_ALIGNMENT_CENTER
        return this
    }

    fun maxLines(lines: Int): Text {
        setMaxLines(lines)
        return this
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View = this
}
