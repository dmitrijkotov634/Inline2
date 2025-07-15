@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.service.modules

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.wavecat.inline.preferences.PreferencesItem
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.TYPE_TEXT_CHANGED
import com.wavecat.inline.service.commands.Command
import org.luaj.vm2.LuaValue

/**
 * Represents a module that can be loaded and managed by the [InlineService].
 *
 * Each module has its own set of commands, watchers, preferences, and command finders.
 * Modules can be internal or external, and their behavior can be customized through Lua scripting.
 *
 * @property service The [InlineService] instance that manages this module.
 * @property filepath The path to the module file.
 * @property isInternal Whether this module is an internal module.
 * @property mCommands A map of command names to [Command] objects.
 * @property mWatchers A map of Lua functions to watcher masks.
 * @property mPreferencesItems A set of [PreferencesItem] objects associated with this module.
 * @property mCommandFinders A set of Lua functions that can be used to find commands.
 * @property category The category of this module.
 */
class Module(
    private val service: InlineService,
    val filepath: String,
    val isInternal: Boolean,
) {
    val mCommands: MutableMap<String, Command> = mutableMapOf()
    val mWatchers: MutableMap<LuaValue, Int> = mutableMapOf()
    val mPreferencesItems: MutableSet<PreferencesItem> = mutableSetOf()
    val mCommandFinders: MutableSet<LuaValue> = mutableSetOf()

    var category: String? = null

    /**
     * Registers a set of preferences for this module.
     *
     * This function creates a [PreferencesItem] using the provided [sharedPreferences] and Lua [builder]
     * function. It then adds this item to the module's local preference list and the service's global
     * preference list, categorized either by the module's `category` field (if set) or its `filepath`.
     *
     * If a category for these preferences doesn't exist yet in the service's global list,
     * a new set is created for it.
     *
     * @param sharedPreferences The [SharedPreferences] instance to be used for storing these preferences.
     * @param builder A Lua function (`LuaValue`) that will be used to build the preference items.
     *                This function is typically called by the preference framework to construct the UI
     *                for these settings.
     */
    fun registerPreferences(sharedPreferences: SharedPreferences, builder: LuaValue) {
        val categoryName = category ?: filepath
        Log.d("Module", "Registering preferences for category: $categoryName") // Log categoryName

        val categoryPreferences = service.allPreferences.getOrPut(categoryName) { HashSet() }

        val item = PreferencesItem(sharedPreferences, builder)
        categoryPreferences.add(item)
        mPreferencesItems.add(item) // Assuming you've renamed mPreferencesItems
    }

    /**
     * Registers a new preferences item using the default shared preferences.
     *
     * @param builder The Lua function that builds the preferences item.
     * @see registerPreferences
     */
    fun registerPreferences(builder: LuaValue) =
        registerPreferences(service.defaultSharedPreferences, builder)

    /**
     * Registers a command with the given name, callable, and description.
     *
     * @param name The name of the command.
     * @param callable The Lua function to be executed when the command is called.
     * @param description An optional description of the command.
     */
    fun registerCommand(name: String, callable: LuaValue, description: String?) {
        Log.d("Module", "Registering command: $name")
        val command = Command(
            category = category,
            callable = callable.checkfunction(),
            description = description
        )
        mCommands[name] = command
        service.allCommands[name] = command
    }

    /**
     * Registers a command with a default empty description.
     *
     * @param name The name of the command.
     * @param function The Lua function to be executed when the command is called.
     */
    fun registerCommand(name: String, function: LuaValue) =
        registerCommand(name, function, "")

    /**
     * Unregisters a command.
     * This function removes a command from both the module's command map and the service's global command map.
     *
     * @param name The name of the command to unregister.
     */
    fun unregisterCommand(name: String) {
        Log.d("Module", "Unregistering command: $name")
        mCommands.remove(name)
        service.allCommands.remove(name)
    }

    /**
     * Registers a watcher function that will be called when certain events occur.
     *
     * @param callable The Lua function to be called.
     * @param mask A bitmask specifying the events to watch for. See [InlineService] for available event types.
     */
    fun registerWatcher(callable: LuaValue, mask: Int) {
        Log.d("Module", "Registering watcher with mask: $mask")
        callable.checkfunction()
        mWatchers[callable] = mask
        service.allWatchers[callable] = mask
    }

    /**
     * Registers a watcher function that will be called when the text in the input field changes.
     * This version uses the default mask [InlineService.TYPE_TEXT_CHANGED].
     *
     * @param callable The Lua function to be called when the text changes.
     */
    fun registerWatcher(callable: LuaValue) {
        Log.d("Module", "Registering watcher with default mask")
        registerWatcher(callable, TYPE_TEXT_CHANGED)
    }

    /**
     * Unregisters a watcher.
     *
     * @param callable The Lua function to unregister.
     */
    fun unregisterWatcher(callable: LuaValue) {
        Log.d("Module", "Unregistering watcher")
        mWatchers.remove(callable)
        service.allWatchers.remove(callable)
    }

    /**
     * Registers a Lua function as a command finder.
     *
     * @param callable The Lua function to register as a command finder.
     */
    fun registerCommandFinder(callable: LuaValue) {
        Log.d("Module", "Registering command finder")
        callable.checkfunction()
        mCommandFinders.add(callable)
        service.allCommandFinders.add(callable)
    }

    /**
     * Unregisters a command finder.
     *
     * Removes the specified Lua function from the list of command finders for this module and the global list of command finders in the service.
     *
     * @param callable The Lua function to unregister. It must be a function, otherwise an error will be thrown.
     * @throws org.luaj.vm2.LuaError if the provided [callable] is not a function.
     */
    fun unregisterCommandFinder(callable: LuaValue) {
        Log.d("Module", "Unregistering command finder")
        callable.checkfunction()
        mCommandFinders.remove(callable)
        service.allCommandFinders.remove(callable)
    }

    /**
     * Unloads the module, removing all its registered commands, watchers, command finders,
     * and preferences items from the service.
     */
    fun unload() {
        mCommands.keys.toList().forEach { commandName -> unregisterCommand(commandName) }
        mWatchers.keys.toList().forEach { luaValue -> unregisterWatcher(luaValue) }
        mCommandFinders.toList().forEach { luaValue -> unregisterCommandFinder(luaValue) }

        mPreferencesItems.forEach {
            service.allPreferences.forEach { preferences ->
                preferences.value.remove(it)
            }
        }
    }

    /**
     * Saves the module's commands to SharedPreferences for lazy loading.
     * This function stores the command names, descriptions, and categories
     * associated with this module. This information can be used later to
     * reconstruct the commands without fully loading the module.
     */
    fun saveLazyLoad() {
        service.lazyLoadSharedPreferences.edit {
            putStringSet(filepath, mCommands.keys)
            mCommands.forEach {
                putString("${it.key}DESC", it.value.description)
                putString("${it.key}CAT", it.value.category)
            }
        }
    }

    /**
     * Sets the description for the internal module.
     * This description is typically displayed in the module management interface.
     *
     * @param description The description string. Can be null to remove the description.
     * @throws IllegalArgumentException if the module is not an internal module.
     */
    fun setDescription(description: String?) {
        require(isInternal) { "setDescription works only with internal modules" }
        service.defaultSharedPreferences.edit {
            putString("DESC${filepath}", description)
        }
    }
}