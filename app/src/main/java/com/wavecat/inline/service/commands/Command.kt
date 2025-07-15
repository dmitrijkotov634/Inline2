package com.wavecat.inline.service.commands

import org.luaj.vm2.LuaValue

/**
 * Represents a command that can be executed.
 *
 * @property category The category of the command. Can be null if the command doesn't belong to a specific category.
 * @property callable The Lua function that will be executed when the command is invoked.
 * @property description A brief description of what the command does. Can be null if no description is provided.
 */
data class Command(
    val category: String?,
    val callable: LuaValue,
    val description: String?,
)