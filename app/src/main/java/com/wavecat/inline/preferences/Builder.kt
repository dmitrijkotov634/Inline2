package com.wavecat.inline.preferences

import android.content.Context
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
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

class Builder(context: Context) : LuaTable() {
    init {
        set("text", getCallable(context, Text::class.java))
        set("checkBox", getCallable(context, CheckBox::class.java))
        set("column", getCallable(context, Column::class.java))
        set("row", getCallable(context, Row::class.java))
        set("seekBar", getCallable(context, SeekBar::class.java))
        set("spinner", getCallable(context, Spinner::class.java))
        set("textInput", getCallable(context, TextInput::class.java))
        set("button", getCallable(context, Button::class.java))
        set("spacer", getCallable(context, Spacer::class.java))
    }

    companion object {
        private fun getCallable(context: Context, klass: Class<*>): LuaTable {
            val metatable = LuaTable()
            metatable[CALL] = object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    return CoerceJavaToLua.coerce(klass)["new"]
                        .invoke(varargsOf(CoerceJavaToLua.coerce(context), args.subargs(2)))
                }
            }
            val table = LuaTable()
            table.setmetatable(metatable)
            return table
        }
    }
}
