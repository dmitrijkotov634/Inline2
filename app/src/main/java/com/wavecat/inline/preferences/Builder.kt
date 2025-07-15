package com.wavecat.inline.preferences

import android.content.Context
import android.view.View
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.preferences.views.Button
import com.wavecat.inline.preferences.views.CheckBox
import com.wavecat.inline.preferences.views.Column
import com.wavecat.inline.preferences.views.HScrollView
import com.wavecat.inline.preferences.views.Row
import com.wavecat.inline.preferences.views.SeekBar
import com.wavecat.inline.preferences.views.SmallButton
import com.wavecat.inline.preferences.views.Spacer
import com.wavecat.inline.preferences.views.Spinner
import com.wavecat.inline.preferences.views.Text
import com.wavecat.inline.preferences.views.TextInput
import com.wavecat.inline.preferences.views.VScrollView
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaTable.CALL
import org.luaj.vm2.LuaValue.varargsOf
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Creates a LuaTable that acts as a constructor for a given Java class,
 * automatically providing a Context instance as the first argument to the constructor.
 *
 * This is useful for creating Lua-accessible constructors for Android views
 * or other classes that require a Context.
 *
 * When the returned LuaTable is called as a function from Lua, it will:
 * 1. Coerce the provided `klass` (Java class) to a LuaValue.
 * 2. Access the "new" method of that coerced class (which corresponds to its constructor).
 * 3. Invoke this "new" method, passing the `context` (coerced to LuaValue)
 *    as the first argument, followed by any arguments passed from the Lua call.
 *
 * @param context The Android Context to be passed to the constructor.
 * @param klass The Java Class for which to create a Lua-callable constructor.
 * @return A LuaTable that, when called, constructs an instance of `klass`
 *         with the provided `context`.
 */
fun withContext(context: Context, klass: Class<*>): LuaTable =
    LuaTable().apply {
        val meta = LuaTable()

        meta[CALL] = varArgFunction { args ->
            CoerceJavaToLua.coerce(klass)["new"]
                .invoke(varargsOf(CoerceJavaToLua.coerce(context), args.subargs(2)))
        }

        setmetatable(meta)
    }

/**
 * A LuaTable that provides access to UI components and event listeners for building user interfaces.
 *
 * This class is designed to be used within a Lua environment to create and manage Android UI elements.
 * It pre-populates itself with common UI components (like Text, CheckBox, Button, etc.) and
 * functions to set click listeners.
 *
 * The UI components are made available as callable Lua tables. When called, they create
 * new instances of the respective Android View, passing the provided `context` and any
 * additional arguments from the Lua call to the View's constructor.
 *
 * @param context The Android [Context] required for creating View instances.
 */
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
        set("smallButton", withContext(context, SmallButton::class.java))
        set("vscroll", withContext(context, VScrollView::class.java))
        set("hscroll", withContext(context, HScrollView::class.java))

        // Listeners

        set("setOnClickListener", twoArgFunction { view, listener ->
            (view.checkuserdata(View::class.java) as View).setOnClickListener {
                listener(CoerceJavaToLua.coerce(view))
            }
            NIL
        })

        set("setOnLongClickListener", twoArgFunction { view, listener ->
            (view.checkuserdata(View::class.java) as View).setOnLongClickListener {
                listener(CoerceJavaToLua.coerce(view)).arg1().optboolean(true)
            }
            NIL
        })
    }
}
