package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.utils.dp

/**
 * A thin horizontal or vertical line divider.
 *
 * Adapts orientation based on parent LinearLayout direction.
 * Uses `colorOutlineVariant` from the theme, falling back to light gray.
 *
 * Usage from Lua: `prefs.divider()`
 *
 * @param context The context in which the divider is created.
 */
class Divider(context: Context) : View(context), Preference {

    private var isVerticalParent = true

    init {
        val typedValue = TypedValue()
        val color = if (context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOutlineVariant, typedValue, true
            )
        ) typedValue.data else 0xFFE0E0E0.toInt()

        setBackgroundColor(color)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val parent = parent
        if (parent is LinearLayout)
            isVerticalParent = parent.orientation == LinearLayout.VERTICAL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isVerticalParent) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(width, 1.dp)
        } else {
            val height = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(1.dp, height)
        }
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View = this
}
