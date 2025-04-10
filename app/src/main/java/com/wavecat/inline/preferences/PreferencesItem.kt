package com.wavecat.inline.preferences

import android.content.SharedPreferences
import org.luaj.vm2.LuaValue

class PreferencesItem(
    val sharedPreferences: SharedPreferences,
    val builder: LuaValue,
)
