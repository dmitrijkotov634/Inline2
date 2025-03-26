package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import org.luaj.vm2.LuaTable

@SuppressLint("ViewConstructor")
class Column(context: Context, views: LuaTable) : LinearGroup(context, views) {
    init {
        orientation = VERTICAL
    }
}
