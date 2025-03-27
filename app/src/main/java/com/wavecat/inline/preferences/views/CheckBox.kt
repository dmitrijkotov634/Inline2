@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.CompoundButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

@SuppressLint("ViewConstructor")
class CheckBox : MaterialCheckBox, Preference {
    var sharedKey: String? = null
        private set

    var listener: LuaValue? = null
        private set

    private var defaultValue = false

    constructor(context: Context?, sharedKey: String?, text: String?) : super(context) {
        this.sharedKey = sharedKey
        setText(text)
    }

    constructor(context: Context?, text: String?, listener: LuaValue?) : super(context) {
        this.listener = listener
        setText(text)
    }

    constructor(context: Context?, text: String?) : super(context) {
        setText(text)
    }

    fun setListener(listener: LuaValue?): CheckBox {
        this.listener = listener
        return this
    }

    fun setSharedKey(sharedKey: String?): CheckBox {
        this.sharedKey = sharedKey
        return this
    }

    fun setDefault(defaultValue: Boolean): CheckBox {
        this.defaultValue = defaultValue
        return this
    }

    override fun getView(preferences: SharedPreferences?): View {
        setOnCheckedChangeListener(null)

        sharedKey?.let {
            if (preferences != null)
                isChecked = preferences.getBoolean(sharedKey, defaultValue)
        }

        setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            sharedKey?.let {
                preferences
                    ?.edit()
                    ?.putBoolean(it, isChecked)
                    ?.apply()
            }

            if (listener != null)
                listener!!.call(
                    LuaValue.valueOf(isChecked),
                    CoerceJavaToLua.coerce(this@CheckBox)
                )
        }

        return this
    }
}
