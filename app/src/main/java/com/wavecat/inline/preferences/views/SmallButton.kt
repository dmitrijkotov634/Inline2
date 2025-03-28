@file:Suppress("unused", "ViewConstructor")

package com.wavecat.inline.preferences.views

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import com.google.android.material.button.MaterialButton
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.utils.dp
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

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
        init(text, null)
    }

    private fun init(text: String, listener: LuaValue?) {
        setText(text)
        setPaddingRelative(6.dp, 6.dp, 6.dp, 6.dp)

        insetTop = 0
        insetBottom = 0
        minimumHeight = 0
        minimumWidth = 0
        minHeight = 0
        minWidth = 0
        background = null

        listener?.let {
            setOnClickListener { _: View? -> it.call(CoerceJavaToLua.coerce(this)) }
        }
    }

    fun setListener(listener: LuaValue): SmallButton {
        setOnClickListener { _: View? -> listener.call(CoerceJavaToLua.coerce(this)) }
        return this
    }

    override fun getView(preferences: SharedPreferences?): View = this
}
