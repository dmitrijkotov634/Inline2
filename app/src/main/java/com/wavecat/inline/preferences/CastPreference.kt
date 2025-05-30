package com.wavecat.inline.preferences

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.preferences.views.LinearGroup
import com.wavecat.inline.preferences.views.Spacer
import com.wavecat.inline.preferences.views.Text
import org.luaj.vm2.LuaNumber
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

fun castPreference(
    context: Context,
    value: LuaValue,
    topOrientation: Int = LinearLayout.VERTICAL,
): Preference =
    when (value) {
        is LuaUserdata -> when {
            value.isuserdata(Preference::class.java) -> value.touserdata() as Preference

            value.isuserdata(View::class.java) -> object : Preference {
                override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View =
                    value.touserdata() as View
            }

            else -> value.checkuserdata(Preference::class.java) as Preference
        }

        is LuaString -> Text(context, value.tojstring())

        is LuaNumber -> Spacer(context, value.toint())

        is LuaTable -> LinearGroup(
            context = context,
            views = value,
        ).apply {
            orientation = if (topOrientation == LinearLayout.VERTICAL)
                LinearLayout.HORIZONTAL else
                LinearLayout.VERTICAL
        }

        else -> throw IllegalArgumentException()
    }

