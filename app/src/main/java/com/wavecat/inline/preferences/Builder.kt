package com.wavecat.inline.preferences

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.preferences.views.Button
import com.wavecat.inline.preferences.views.Card
import com.wavecat.inline.preferences.views.CheckBox
import com.wavecat.inline.preferences.views.Column
import com.wavecat.inline.preferences.views.Divider
import com.wavecat.inline.preferences.views.FlexSpacer
import com.wavecat.inline.preferences.views.HScrollView
import com.wavecat.inline.preferences.views.Row
import com.wavecat.inline.preferences.views.Slider
import com.wavecat.inline.preferences.views.SmallButton
import com.wavecat.inline.preferences.views.Spacer
import com.wavecat.inline.preferences.views.Spinner
import com.wavecat.inline.preferences.views.Switch
import com.wavecat.inline.preferences.views.Text
import com.wavecat.inline.preferences.views.TextInput
import com.wavecat.inline.preferences.views.VScrollView
import com.wavecat.inline.utils.dp
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaTable.CALL
import org.luaj.vm2.LuaValue.varargsOf
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Creates a LuaTable that acts as a constructor for a given Java class,
 * automatically providing a Context instance as the first argument to the constructor.
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
 * A LuaTable that provides access to UI components, layout utilities,
 * and event listeners for building user interfaces from Lua.
 *
 * @param context The Android [Context] required for creating View instances.
 */
class Builder(context: Context) : LuaTable() {
    init {
        // Widgets
        set("text", withContext(context, Text::class.java))
        set("checkBox", withContext(context, CheckBox::class.java))
        set("column", withContext(context, Column::class.java))
        set("row", withContext(context, Row::class.java))
        set("slider", withContext(context, Slider::class.java))
        set("seekBar", withContext(context, Slider::class.java))
        set("spinner", withContext(context, Spinner::class.java))
        set("textInput", withContext(context, TextInput::class.java))
        set("button", withContext(context, Button::class.java))
        set("spacer", withContext(context, Spacer::class.java))
        set("flexSpacer", withContext(context, FlexSpacer::class.java))
        set("smallButton", withContext(context, SmallButton::class.java))
        set("vscroll", withContext(context, VScrollView::class.java))
        set("hscroll", withContext(context, HScrollView::class.java))
        set("divider", withContext(context, Divider::class.java))
        set("card", withContext(context, Card::class.java))
        set("switch", withContext(context, Switch::class.java))

        // View utilities

        set("margin", varArgFunction { args ->
            val view = args.arg(1).checkuserdata(View::class.java) as View
            val lp = view.layoutParams as? LinearLayout.LayoutParams
                ?: LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            when (args.narg()) {
                2 -> {
                    val all = args.arg(2).checkint().dp
                    lp.setMargins(all, all, all, all)
                }

                3 -> {
                    val h = args.arg(2).checkint().dp
                    val v = args.arg(3).checkint().dp
                    lp.setMargins(h, v, h, v)
                }

                else -> lp.setMargins(
                    args.arg(2).checkint().dp,
                    args.arg(3).checkint().dp,
                    args.arg(4).checkint().dp,
                    args.arg(5).checkint().dp
                )
            }

            view.layoutParams = lp
            CoerceJavaToLua.coerce(view)
        })

        set("padding", varArgFunction { args ->
            val view = args.arg(1).checkuserdata(View::class.java) as View

            when (args.narg()) {
                2 -> {
                    val all = args.arg(2).checkint().dp
                    view.setPadding(all, all, all, all)
                }

                3 -> {
                    val h = args.arg(2).checkint().dp
                    val v = args.arg(3).checkint().dp
                    view.setPadding(h, v, h, v)
                }

                else -> view.setPadding(
                    args.arg(2).checkint().dp,
                    args.arg(3).checkint().dp,
                    args.arg(4).checkint().dp,
                    args.arg(5).checkint().dp
                )
            }

            CoerceJavaToLua.coerce(view)
        })

        set("visible", twoArgFunction { viewArg, visibleArg ->
            val view = viewArg.checkuserdata(View::class.java) as View
            view.visibility = if (visibleArg.checkboolean()) View.VISIBLE else View.GONE
            CoerceJavaToLua.coerce(view)
        })

        set("enabled", twoArgFunction { viewArg, enabledArg ->
            val view = viewArg.checkuserdata(View::class.java) as View
            view.isEnabled = enabledArg.checkboolean()
            view.alpha = if (view.isEnabled) 1f else 0.5f
            CoerceJavaToLua.coerce(view)
        })

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
