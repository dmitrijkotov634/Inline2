package com.wavecat.inline.service.modules

import android.content.SharedPreferences
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.service.commands.Command
import org.luaj.vm2.LuaValue

const val LAZYLOAD: String = "lazyload"

/**
 * Loads lazy stubs for commands.
 *
 * This function iterates over a set of command names and creates stub commands for each.
 * These stub commands, when invoked, will first trigger a provided `load` function
 * (presumably to load the actual command implementation) and then execute the
 * (now loaded) command.
 *
 * The category and description for each stub command are retrieved from SharedPreferences.
 *
 * @param allCommands A mutable map where the loaded stub commands will be stored.
 *                    The keys are command names and the values are Command objects.
 * @param commands A set of strings representing the names of the commands for which
 *                 lazy stubs should be created.
 * @param lazyPrefs SharedPreferences instance used to retrieve the category and description
 *                  for each command. It's expected that for each command name `name`,
 *                  there are keys `"${name}CAT"` for the category and `"${name}DESC"`
 *                  for the description.
 * @param load A lambda function that will be executed when a lazy stub command is
 *             first invoked. This function is responsible for loading the actual
 *             implementation of the command.
 */
fun loadLazyStubs(
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