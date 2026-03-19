package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.preferences.Preference

/**
 * A flexible spacer that expands to fill available space in a LinearLayout.
 *
 * Uses `layout_weight` to distribute remaining space. Useful for pushing
 * sibling views apart (e.g., aligning buttons to opposite edges of a Row).
 *
 * Usage from Lua:
 * - `prefs.flexSpacer()` — weight 1 (default)
 * - `prefs.flexSpacer(2)` — weight 2 (takes twice the space of weight-1 siblings)
 *
 * @param context The context in which the spacer is created.
 * @param weight The layout weight for space distribution. Defaults to 1.
 */
class FlexSpacer @JvmOverloads constructor(
    context: Context,
    weight: Int = 1,
) : View(context), Preference {

    private val layoutWeight = weight.toFloat()

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        layoutParams = LinearLayout.LayoutParams(0, 0, layoutWeight)
        return this
    }
}
