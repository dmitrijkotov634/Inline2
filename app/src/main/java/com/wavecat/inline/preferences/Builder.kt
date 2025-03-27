package com.wavecat.inline.preferences

import android.content.Context
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.preferences.views.Button
import com.wavecat.inline.preferences.views.CheckBox
import com.wavecat.inline.preferences.views.Column
import com.wavecat.inline.preferences.views.Row
import com.wavecat.inline.preferences.views.SeekBar
import com.wavecat.inline.preferences.views.Spacer
import com.wavecat.inline.preferences.views.Spinner
import com.wavecat.inline.preferences.views.Text
import com.wavecat.inline.preferences.views.TextInput
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaTable.CALL
import org.luaj.vm2.LuaTable.varargsOf
import org.luaj.vm2.lib.jse.CoerceJavaToLua

fun withContext(context: Context, klass: Class<*>): LuaTable =
    LuaTable().apply {
        setmetatable(LuaTable().apply {
            this[CALL] = varArgFunction {
                CoerceJavaToLua.coerce(klass)["new"]
                    .invoke(varargsOf(CoerceJavaToLua.coerce(context), it.subargs(2)))
            }
        })
    }

class Builder(context: Context) : LuaTable() {
    init {
        set("text", withContext(context, Text::class.java))
        set("checkBox", withContext(context, CheckBox::class.java))
        set("column", withContext(context, Column::class.java))
        set("row", withContext(context, Row::class.java))
        set("seekBar", withContext(context, SeekBar::class.java))
        set("spinner", withContext(context, Spinner::class.java))
        set("textInput", withContext(context, TextInput::class.java))
        set("button", withContext(context, Button::class.java))
        set("spacer", withContext(context, Spacer::class.java))
    }
}
