@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.CompoundButton
import androidx.core.content.edit
import com.google.android.material.checkbox.MaterialCheckBox
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.utils.dp
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua


/**
 * Custom checkbox implementation for preferences with Lua scripting support.
 *
 * Extends MaterialCheckBox to provide integration with SharedPreferences
 * for persistent storage and Lua callback functionality for dynamic behavior.
 * Supports both preference-backed and listener-based checkbox configurations.
 *
 * @see MaterialCheckBox
 * @see Preference
 * @see SharedPreferences
 */
@SuppressLint("ViewConstructor")
class CheckBox : MaterialCheckBox, Preference {

    var sharedKey: String? = null
        private set

    var listener: LuaValue? = null
        private set

    private var defaultValue = false

    constructor(context: Context?, sharedKey: String?, text: String?) : this(context) {
        this.sharedKey = sharedKey
        setText(text)
    }

    constructor(context: Context?, text: String?, listener: LuaValue?) : this(context) {
        this.listener = listener
        setText(text)
    }

    constructor(context: Context?, text: String?) : this(context) {
        setText(text)
    }

    constructor(context: Context?) : super(context) {
        minimumHeight = 36.dp
        translationX = (-6).dp.toFloat()
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

            if (listener != null)
                listener!!.call(
                    LuaValue.valueOf(isChecked),
                    CoerceJavaToLua.coerce(this@CheckBox)
                )
        }

        return this
    }
}
