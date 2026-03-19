@file:Suppress("ViewConstructor", "unused")

package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.preferences.castPreference
import com.wavecat.inline.utils.dp
import org.luaj.vm2.LuaTable

/**
 * A styled container with outline stroke, corner radius, and optional elevation.
 *
 * Groups child views visually with a border outline for clear separation
 * from the dialog/activity background. Children are arranged vertically by default.
 *
 * Usage from Lua:
 * ```
 * prefs.card({
 *     prefs.text("Title"):bold(),
 *     prefs.spacer(4),
 *     prefs.text("Description"),
 * })
 * ```
 *
 * @param context The context in which the card is created.
 * @param views A LuaTable containing the child views.
 */
class Card(context: Context, private val views: LuaTable) : LinearLayout(context), Preference {

    private var cardCornerRadius = 12
    private var cardBackgroundColor: Int = Color.TRANSPARENT
    private var cardStrokeColor: Int
    private var cardStrokeWidth: Int = 1
    private var cardElevation = 0f

    init {
        orientation = VERTICAL
        setPadding(16.dp, 12.dp, 16.dp, 12.dp)

        val typedValue = TypedValue()
        cardStrokeColor = if (context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOutlineVariant, typedValue, true
            )
        ) typedValue.data else 0xFFCAC4D0.toInt()

        applyBackground()
    }

    fun setCornerRadius(radius: Int): Card {
        cardCornerRadius = radius
        applyBackground()
        return this
    }

    fun setCardBackgroundColor(color: Int): Card {
        cardBackgroundColor = color
        applyBackground()
        return this
    }

    fun setStrokeColor(color: Int): Card {
        cardStrokeColor = color
        applyBackground()
        return this
    }

    fun setStrokeWidth(width: Int): Card {
        cardStrokeWidth = width
        applyBackground()
        return this
    }

    fun setCardElevation(elevation: Float): Card {
        cardElevation = elevation
        applyBackground()
        return this
    }

    private fun applyBackground() {
        background = GradientDrawable().apply {
            setColor(cardBackgroundColor)
            setStroke(cardStrokeWidth.dp, cardStrokeColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cornerRadius = cardCornerRadius.dp.toFloat()
            }
        }

        if (cardElevation > 0f) {
            elevation = cardElevation.dp.toFloat()
        }
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        removeAllViews()

        views.forEach { _, value ->
            addView(
                castPreference(context, value, VERTICAL)
                    .getView(preferences, requestFocus)
            )
        }

        return this
    }
}
