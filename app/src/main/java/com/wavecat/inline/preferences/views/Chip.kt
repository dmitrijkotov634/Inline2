@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.core.content.edit
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import com.google.android.material.chip.Chip as MaterialChip

/**
 * Material Chip for preferences with Lua scripting support.
 *
 * Supports checkable mode (with SharedPreferences) and click mode (with listener).
 *
 * Usage from Lua:
 * ```
 * builder.chip("my_key", "Label")           -- checkable, persists to prefs
 * builder.chip("Label", listener)           -- click handler
 * builder.chip("Label")                     -- plain chip
 * ```
 */
@SuppressLint("ViewConstructor")
class Chip : MaterialChip, Preference {

    var sharedKey: String? = null
        private set

    var listener: LuaValue? = null
        private set

    private var defaultValue = false
    private var checkableMode = false

    constructor(context: Context, sharedKey: String?, label: String?) : super(context) {
        this.sharedKey = sharedKey
        checkableMode = true
        isCheckable = true
        init(label)
    }

    constructor(context: Context, label: String?, listener: LuaValue?) : super(context) {
        this.listener = listener
        checkableMode = false
        isCheckable = false
        init(label)
    }

    constructor(context: Context, label: String?) : super(context) {
        checkableMode = false
        isCheckable = false
        init(label)
    }

    private fun init(label: String?) {
        text = label
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun setListener(listener: LuaValue?): Chip {
        this.listener = listener
        return this
    }

    fun setSharedKey(sharedKey: String?): Chip {
        this.sharedKey = sharedKey
        return this
    }

    fun setDefault(defaultValue: Boolean): Chip {
        this.defaultValue = defaultValue
        return this
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        if (checkableMode) {
            setOnCheckedChangeListener(null)

            sharedKey?.let {
                if (preferences != null)
                    isChecked = preferences.getBoolean(sharedKey, defaultValue)
            }

            setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
                sharedKey?.let {
                    preferences?.edit {
                        putBoolean(it, checked)
                    }
                }

                listener?.call(
                    LuaValue.valueOf(checked),
                    CoerceJavaToLua.coerce(this@Chip)
                )
            }
        } else {
            setOnClickListener {
                listener?.call(CoerceJavaToLua.coerce(this@Chip))
            }
        }

        return this
    }
}
