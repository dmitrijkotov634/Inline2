package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.preferences.castPreference
import org.luaj.vm2.LuaTable

@SuppressLint("ViewConstructor")
open class LinearGroup(context: Context, private val views: LuaTable) : LinearLayout(context), Preference {
    override fun getView(preferences: SharedPreferences?): View {
        removeAllViews()

        views.forEach { _, value ->
            addView(
                castPreference(
                    context = context,
                    value = value,
                    topOrientation = orientation
                )
                    .getView(preferences)
            )
        }

        return this
    }
}
