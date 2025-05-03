@file:Suppress("unused")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.core.content.edit
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.preferences.Preference
import okhttp3.internal.toLongOrDefault
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

@SuppressLint("ViewConstructor")
class TextInput(context: Context) : TextInputLayout(context), Preference {
    var sharedKey: String? = null
        private set

    var listener: LuaValue? = null
        private set

    private var textWatcher: TextWatcher? = null
    private var defaultValue: LuaValue? = null

    private var useFloat: Boolean = false
    private var useLong: Boolean = false
    private var useInt: Boolean = false

    constructor(context: Context, sharedKey: String, hint: String) : this(context, hint) {
        this.sharedKey = sharedKey
    }

    constructor(context: Context, hint: String, listener: LuaValue) : this(context, hint) {
        this.listener = listener
    }

    constructor(context: Context, hint: String?) : this(context) {
        setHint(hint)
    }

    init {
        addView(TextInputEditText(getContext()))
    }

    fun setListener(listener: LuaValue?): TextInput {
        this.listener = listener
        return this
    }

    fun setSharedKey(sharedKey: String?): TextInput {
        this.sharedKey = sharedKey
        return this
    }

    fun setDefault(defaultValue: String): TextInput {
        this.defaultValue = LuaValue.valueOf(defaultValue)
        return this
    }

    fun setDefault(defaultValue: Int): TextInput {
        this.defaultValue = LuaValue.valueOf(defaultValue)
        return this
    }

    fun setDefault(defaultValue: Double): TextInput {
        this.defaultValue = LuaValue.valueOf(defaultValue)
        return this
    }

    fun hidePassword(): TextInput {
        editText?.transformationMethod = PasswordTransformationMethod.getInstance()
        return this
    }

    fun showPassword(): TextInput {
        editText?.transformationMethod = HideReturnsTransformationMethod.getInstance()
        return this
    }

    fun setSingleLine(value: Boolean): TextInput {
        editText?.isSingleLine = value
        return this
    }

    fun setInputType(types: LuaValue): TextInput {
        var type = InputType.TYPE_NULL

        types.forEach { _, value ->
            type = type or InputType::class.java.getDeclaredField(value.checkjstring()).getInt(null)
        }

        editText?.inputType = type
        return this
    }

    fun useFloat(): TextInput {
        useFloat = true
        return this
    }

    fun useLong(): TextInput {
        useLong = true
        return this
    }

    fun useInt(): TextInput {
        useInt = true
        return this
    }

    var text: String?
        get() = editText?.text.toString()
        set(text) {
            editText?.setText(text)
        }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        if (textWatcher != null)
            editText?.removeTextChangedListener(textWatcher)

        sharedKey?.let {
            editText?.setText(
                when {
                    useLong -> preferences?.getLong(it, defaultValue?.tolong() ?: 0).toString()
                    useInt -> preferences?.getInt(it, defaultValue?.toint() ?: 0).toString()
                    useFloat -> preferences?.getFloat(it, defaultValue?.tofloat() ?: 0f).toString()
                    else -> preferences?.getString(it, defaultValue?.tojstring())
                }
            )
        }

        textWatcher = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                sharedKey?.let {
                    preferences?.edit {
                        when {
                            useLong -> putLong(it, s.toString().toLongOrDefault(0))
                            useInt -> putInt(it, s.toString().toIntOrNull() ?: 0)
                            useFloat -> putFloat(it, s.toString().toFloatOrNull() ?: 0f)
                            else -> putString(it, s.toString())
                        }
                    }
                }

                listener?.call(
                    LuaValue.valueOf(s.toString()),
                    CoerceJavaToLua.coerce(this@TextInput)
                )
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable) {}
        }

        editText?.addTextChangedListener(textWatcher)
        editText?.setOnClickListener { requestFocus() }

        return this
    }
}