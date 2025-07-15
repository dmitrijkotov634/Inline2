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

/**
 * Converts a LuaValue to a Preference object.
 *
 * This function handles different Lua types and maps them to appropriate Preference implementations:
 * - **LuaUserdata:**
 *     - If it's already a `Preference`, it's returned directly.
 *     - If it's a `View`, it's wrapped in a custom `Preference` implementation.
 *     - Otherwise, it's treated as an error and `checkuserdata` will throw an exception.
 * - **LuaString:** Creates a `Text` preference.
 * - **LuaNumber:** Creates a `Spacer` preference with the number as its height/width.
 * - **LuaTable:** Creates a `LinearGroup` preference. The orientation of this group is
 *   set to be the opposite of `topOrientation`.
 * - **Other types:** Throws an `IllegalArgumentException`.
 *
 * @param context The Android Context.
 * @param value The LuaValue to convert.
 * @param topOrientation The orientation of the parent container, used to determine the
 *                       orientation of a `LinearGroup` created from a `LuaTable`.
 *                       Defaults to `LinearLayout.VERTICAL`.
 * @return The corresponding `Preference` object.
 * @throws IllegalArgumentException if the `value` is not a supported Lua type.
 * @throws org.luaj.vm2.LuaError if a `LuaUserdata` is not a `Preference` or `View`.
 */
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

