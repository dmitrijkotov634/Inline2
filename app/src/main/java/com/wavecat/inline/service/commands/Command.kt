package com.wavecat.inline.service.commands

import org.luaj.vm2.LuaValue

data class Command(
    val category: String?,
    val callable: LuaValue,
    val description: String?,
)