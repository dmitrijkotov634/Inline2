@file:Suppress("unused", "ViewConstructor", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import com.google.android.material.button.MaterialButton
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.utils.dp
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Compact button implementation for preferences with minimal styling.
 *
 * Extends MaterialButton with a borderless style and reduced dimensions
 * for use in preference screens where space-efficient buttons are needed.
 * Provides Lua scripting support for click handling while maintaining
 * a minimal visual footprint.
 *
 * @see MaterialButton
 * @see Preference
 * @see LuaValue
 */
class SmallButton : MaterialButton, Preference {
    constructor(context: Context, text: String, listener: LuaValue) : super(
        context,
        null,
        com.google.android.material.R.attr.borderlessButtonStyle
    ) {
        init(text, listener)
    }

    constructor(context: Context, text: String) : super(
        context,
        null,
        com.google.android.material.R.attr.borderlessButtonStyle
    ) {
        init(text, LuaValue.NIL)
    }

    private fun init(text: String, listener: LuaValue) {
        setText(text)
        setPaddingRelative(8.dp, 8.dp, 8.dp, 8.dp)

        insetTop = 0
        insetBottom = 0
        minimumHeight = 0
        minimumWidth = 0
        minHeight = 0
        minWidth = 0
        background = null

        setListener(listener)
    }

    fun setListener(listener: LuaValue): SmallButton {
        setOnClickListener { _: View? ->
            if (listener.isfunction())
                listener.call(CoerceJavaToLua.coerce(this))
        }

        return this
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View = this
}
