package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import org.luaj.vm2.LuaTable

/**
 * A [LinearGroup] that arranges its children vertically.
 *
 * @param context The context.
 * @param views A [LuaTable] containing the views to be added to this layout.
 */
@SuppressLint("ViewConstructor")
class Column(
    context: Context,
    views: LuaTable,
) : LinearGroup(context, views) {
    init {
        orientation = VERTICAL
    }
}
