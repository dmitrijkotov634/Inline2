@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.service.modules

import android.content.SharedPreferences
import android.util.Log
import com.wavecat.inline.preferences.PreferencesItem
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.TYPE_TEXT_CHANGED
import com.wavecat.inline.service.commands.Command
import org.luaj.vm2.LuaValue

class Module(
    private val service: InlineService,
    val filepath: String,
    val isInternal: Boolean,
) {
    var category: String? = null

    fun registerPreferences(sharedPreferences: SharedPreferences, builder: LuaValue) {
        val categoryName = category ?: filepath
        var categoryPreferences = service.allPreferences[categoryName]

        Log.d("Module", "Registering preferences for category: $category")

        if (categoryPreferences == null) {
            categoryPreferences = HashSet()
            service.allPreferences[categoryName] = categoryPreferences
        }

        categoryPreferences.add(PreferencesItem(sharedPreferences, builder))
    }

    fun registerPreferences(builder: LuaValue) =
        registerPreferences(service.defaultSharedPreferences, builder)

    fun registerCommand(name: String, callable: LuaValue, description: String?) {
        Log.d("Module", "Registering command: $name")
        service.allCommands[name] = Command(
            category = category,
            callable = callable.checkfunction(),
            description = description
        )
    }

    fun registerCommand(name: String, function: LuaValue) =
        registerCommand(name, function, "")

    fun unregisterCommand(name: String) {
        Log.d("Module", "Unregistering command: $name")
        service.allCommands.remove(name)
    }

    fun registerWatcher(callable: LuaValue, mask: Int) {
        Log.d("Module", "Registering watcher with mask: $mask")
        service.allWatchers[callable.checkfunction()] = mask
    }

    fun registerWatcher(callable: LuaValue) {
        Log.d("Module", "Registering watcher with default mask")
        service.allWatchers[callable.checkfunction()] = TYPE_TEXT_CHANGED
    }

    fun unregisterWatcher(callable: LuaValue) {
        Log.d("Module", "Unregistering watcher")
        service.allWatchers.remove(callable)
    }

    fun registerCommandFinder(callable: LuaValue) {
        Log.d("Module", "Registering command finder")
        service.allCommandFinders.add(callable.checkfunction())
    }

    fun unregisterCommandFinder(callable: LuaValue) {
        Log.d("Module", "Unregistering command finder")
        service.allCommandFinders.remove(callable)
    }
}