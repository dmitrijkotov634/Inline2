@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue
import android.view.View
import android.widget.CompoundButton
import androidx.core.content.edit
import com.google.android.material.materialswitch.MaterialSwitch
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Material Switch for preferences with Lua scripting support.
 */
@SuppressLint("ViewConstructor")
class Switch : MaterialSwitch, Preference {

    var sharedKey: String? = null
        private set

    var listener: LuaValue? = null
        private set

    private var defaultValue = false

    constructor(context: Context, sharedKey: String?, text: String?) : super(context) {
        this.sharedKey = sharedKey
        init(text)
    }

    constructor(context: Context, text: String?, listener: LuaValue?) : super(context) {
        this.listener = listener
        init(text)
    }

    constructor(context: Context, text: String?) : super(context) {
        init(text)
    }

    private fun init(text: String?) {
        setText(text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    }

    fun setListener(listener: LuaValue?): Switch {
        this.listener = listener
        return this
    }

    fun setSharedKey(sharedKey: String?): Switch {
        this.sharedKey = sharedKey
        return this
    }

    fun setDefault(defaultValue: Boolean): Switch {
        this.defaultValue = defaultValue
        return this
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        setOnCheckedChangeListener(null)

        sharedKey?.let {
            if (preferences != null)
                isChecked = preferences.getBoolean(sharedKey, defaultValue)
        }

        setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            sharedKey?.let {
                preferences?.edit {
                    putBoolean(it, isChecked)
                }
            }

            listener?.call(
                LuaValue.valueOf(isChecked),
                CoerceJavaToLua.coerce(this@Switch)
            )
        }

        return this
    }
}
