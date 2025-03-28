@file:Suppress("unused")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

@SuppressLint("ViewConstructor")
class Spinner(context: Context?, set: LuaTable) : AppCompatSpinner(context!!), Preference {
    var sharedKey: String? = null
        private set

    var listener: LuaValue? = null
        private set

    private val choices: MutableList<String?> = ArrayList()
    private val adapter: ArrayAdapter<String?>

    private var userSelect = true

    constructor(context: Context, sharedKey: String?, choices: LuaTable) : this(context, choices) {
        this.sharedKey = sharedKey
    }

    constructor(context: Context, choices: LuaTable, listener: LuaValue?) : this(context, choices) {
        this.listener = listener
    }

    init {
        set.forEach { _, value ->
            choices.add(value.tojstring())
        }

        adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, choices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        setAdapter(adapter)
        setSelection(0)
    }

    fun setListener(listener: LuaValue?): Spinner {
        this.listener = listener
        return this
    }

    fun setSharedKey(sharedKey: String?): Spinner {
        this.sharedKey = sharedKey
        return this
    }

    override fun getAdapter(): ArrayAdapter<String?> {
        return adapter
    }

    override fun getView(preferences: SharedPreferences?): View {
        if (sharedKey != null && preferences != null) {
            userSelect = false
            setSelection(choices.indexOf(preferences.getString(sharedKey, "")))
        }

        onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (!userSelect) {
                    userSelect = true
                    return
                }

                sharedKey?.let {
                    preferences
                        ?.edit()
                        ?.putString(it, adapter.getItem(position))
                        ?.apply()
                }

                if (listener != null)
                    listener!!.call(
                        LuaValue.valueOf(adapter.getItem(position)),
                        CoerceJavaToLua.coerce(this@Spinner)
                    )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return this
    }
}
