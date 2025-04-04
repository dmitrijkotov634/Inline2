package com.wavecat.inline.service.modules

import android.content.SharedPreferences
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.service.commands.Command
import org.luaj.vm2.LuaValue

const val LAZYLOAD: String = "lazyload"

fun loadLazyCommands(
    allCommands: MutableMap<String, Command>,
    commands: Set<String>,
    lazyPrefs: SharedPreferences,
    load: () -> Unit,
) {
    commands.forEach { name ->
        val category = lazyPrefs.getString("${name}CAT", null)
        val description = lazyPrefs.getString("${name}DESC", null)

        allCommands[name] = Command(
            category = category,
            description = description,
            callable = varArgFunction { args ->
                load()
                allCommands[name]?.callable?.invoke(args) ?: LuaValue.NIL
            }
        )
    }
}