@file:Suppress("unused")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

@SuppressLint("ViewConstructor")
class TextInput(context: Context) : TextInputLayout(context), Preference {
    var sharedKey: String? = null
        private set

    var listener: LuaValue? = null
        private set

    private var textWatcher: TextWatcher? = null
    private var defaultValue: String? = null

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
        addView(TextInputEditText(context))
    }

    fun setListener(listener: LuaValue?): TextInput {
        this.listener = listener
        return this
    }

    fun setSharedKey(sharedKey: String?): TextInput {
        this.sharedKey = sharedKey
        return this
    }

    fun setDefault(defaultValue: String?): TextInput {
        this.defaultValue = defaultValue
        return this
    }

    fun setInputTypePassword(): TextInput {
        editText?.transformationMethod = PasswordTransformationMethod.getInstance()
        return this
    }

    fun setSingleLine(value: Boolean): TextInput {
        editText?.isSingleLine = value
        return this
    }

    fun setInputTypeDefault(): TextInput {
        editText?.transformationMethod = HideReturnsTransformationMethod.getInstance()
        return this
    }

    var text: String?
        get() = editText?.text.toString()
        set(text) {
            editText?.setText(text)
        }

    override fun getView(preferences: SharedPreferences?): View {
        if (textWatcher != null)
            editText?.removeTextChangedListener(textWatcher)

        sharedKey?.let {
            editText?.setText(preferences?.getString(it, defaultValue))
        }

        textWatcher = object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                sharedKey?.let {
                    preferences
                        ?.edit()
                        ?.putString(it, s.toString())
                        ?.apply()
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
        return this
    }

    override fun setWindowFocusListener(requestFocus: () -> Unit) {
        editText?.setOnClickListener { requestFocus() }
    }
}