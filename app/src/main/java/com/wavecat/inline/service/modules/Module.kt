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

    fun registerPreferences(sharedPreferences: SharedPreferences, builder: LuaValue) {
        val categoryName = category ?: filepath
        var categoryPreferences = service.allPreferences[categoryName]

        Log.d("Module", "Registering preferences for category: $category")

        if (categoryPreferences == null) {
            categoryPreferences = HashSet()
            service.allPreferences[categoryName] = categoryPreferences
        }

        val item = PreferencesItem(sharedPreferences, builder)
        categoryPreferences.add(item)
        mPreferencesItems.add(item)
    }

    fun registerPreferences(builder: LuaValue) =
        registerPreferences(service.defaultSharedPreferences, builder)

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

    fun registerCommand(name: String, function: LuaValue) =
        registerCommand(name, function, "")

    fun unregisterCommand(name: String) {
        Log.d("Module", "Unregistering command: $name")
        mCommands.remove(name)
        service.allCommands.remove(name)
    }

    fun registerWatcher(callable: LuaValue, mask: Int) {
        Log.d("Module", "Registering watcher with mask: $mask")
        callable.checkfunction()
        mWatchers[callable] = mask
        service.allWatchers[callable] = mask
    }

    fun registerWatcher(callable: LuaValue) {
        Log.d("Module", "Registering watcher with default mask")
        registerWatcher(callable, TYPE_TEXT_CHANGED)
    }

    fun unregisterWatcher(callable: LuaValue) {
        Log.d("Module", "Unregistering watcher")
        mWatchers.remove(callable)
        service.allWatchers.remove(callable)
    }

    fun registerCommandFinder(callable: LuaValue) {
        Log.d("Module", "Registering command finder")
        callable.checkfunction()
        mCommandFinders.add(callable)
        service.allCommandFinders.add(callable)
    }

    fun unregisterCommandFinder(callable: LuaValue) {
        Log.d("Module", "Unregistering command finder")
        callable.checkfunction()
        mCommandFinders.remove(callable)
        service.allCommandFinders.remove(callable)
    }

    fun unload() {
        mCommands.forEach { unregisterCommand(it.key) }
        mWatchers.forEach { unregisterWatcher(it.key) }
        mCommandFinders.forEach { unregisterCommandFinder(it) }

        mPreferencesItems.forEach {
            service.allPreferences.forEach { preferences ->
                preferences.value.remove(it)
            }
        }
    }

    fun saveLazyLoad() {
        service.lazyLoadSharedPreferences.edit {
            putStringSet(filepath, mCommands.keys)
            mCommands.forEach {
                putString("${it.key}DESC", it.value.description)
                putString("${it.key}CAT", it.value.category)
            }
        }
    }

    fun setDescription(description: String?) {
        require(isInternal) { "setDescription works only with internal modules" }
        service.defaultSharedPreferences.edit {
            putString("DESC${filepath}", description)
        }
    }
}