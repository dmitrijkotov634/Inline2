@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import com.google.android.material.button.MaterialButton
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua


/**
 * Custom button implementation for preferences with Lua scripting support.
 *
 * Extends MaterialButton to provide integration with the preferences system
 * and Lua callback functionality for dynamic behavior. Designed for use
 * in preference screens where buttons trigger Lua-defined actions.
 *
 * @see MaterialButton
 * @see Preference
 * @see LuaValue
 */
@SuppressLint("ViewConstructor")
class Button : MaterialButton, Preference {
    constructor(context: Context, text: String, listener: LuaValue) : super(context) {
        setText(text)
        setListener(listener)
    }

    constructor(context: Context, text: String) : super(context) {
        setText(text)
    }

    fun setListener(listener: LuaValue): Button {
        setOnClickListener { _: View? ->
            if (listener.isfunction())
                listener.call(CoerceJavaToLua.coerce(this))
        }

        return this
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View = this
}
