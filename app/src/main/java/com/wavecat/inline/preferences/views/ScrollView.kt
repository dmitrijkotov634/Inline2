package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.preferences.castPreference
import org.luaj.vm2.LuaValue

/**
 * A vertical scroll view that can be used as a preference.
 *
 * @param context The context to use.
 * @param view The LuaValue representing the view to display in the scroll view.
 */
@SuppressLint("ViewConstructor")
open class VScrollView(context: Context, private val view: LuaValue) : ScrollView(context), Preference {
    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        removeAllViews()
        addView(
            castPreference(
                context = context,
                value = view,
                topOrientation = LinearLayout.HORIZONTAL
            )
                .getView(preferences, requestFocus)
        )

        return this
    }
}

/**
 * A horizontal scroll view that can be used in preferences.
 *
 * @param context The context to use.
 * @param view The view to display in the scroll view.
 */
@SuppressLint("ViewConstructor")
open class HScrollView(context: Context, private val view: LuaValue) : HorizontalScrollView(context),
    Preference {
    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        removeAllViews()
        addView(
            castPreference(
                context = context,
                value = view,
                topOrientation = LinearLayout.VERTICAL
            )
                .getView(preferences, requestFocus)
        )

        return this
    }
}
