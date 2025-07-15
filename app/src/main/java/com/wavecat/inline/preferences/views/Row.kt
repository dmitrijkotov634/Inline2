package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import org.luaj.vm2.LuaTable

/**
 * A layout that arranges its children in a single horizontal line.
 * This class is a specialized version of [LinearGroup] with a fixed horizontal orientation.
 *
 * @param context The Context the view is running in, through which it can
 *        access the current theme, resources, etc.
 * @param views A LuaTable containing the child views to be added to this layout.
 *              Each entry in the table should be a view object.
 */
@SuppressLint("ViewConstructor")
class Row(
    context: Context,
    views: LuaTable,
) : LinearGroup(context, views) {
    init {
        orientation = HORIZONTAL
    }
}
