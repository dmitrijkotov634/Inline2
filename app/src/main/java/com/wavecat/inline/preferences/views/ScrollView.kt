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
