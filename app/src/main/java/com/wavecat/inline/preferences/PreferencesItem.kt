package com.wavecat.inline.preferences

import android.content.SharedPreferences
import org.luaj.vm2.LuaValue

/**
 * Data class representing a preferences configuration item.
 *
 * Encapsulates the relationship between SharedPreferences storage
 * and Lua-based preference UI builders. Used to manage preference
 * screens where the UI layout is defined by Lua scripts.
 *
 * @property sharedPreferences The SharedPreferences instance for data persistence
 * @property builder Lua function that constructs the preference UI layout
 * @see SharedPreferences
 * @see LuaValue
 */
class PreferencesItem(
    val sharedPreferences: SharedPreferences,
    val builder: LuaValue,
)
