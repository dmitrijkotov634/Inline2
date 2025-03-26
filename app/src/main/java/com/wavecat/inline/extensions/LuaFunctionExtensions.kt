package com.wavecat.inline.extensions

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

fun varArgFunction(block: (Varargs) -> Varargs) = object : VarArgFunction() {
    override fun onInvoke(args: Varargs?): Varargs {
        args ?: return NIL
        return block(args)
    }
}

fun zeroArgFunction(
    block: () -> LuaValue,
) = object : ZeroArgFunction() {
    override fun call(): LuaValue {
        return block()
    }
}

fun oneArgFunction(
    block: (LuaValue) -> LuaValue,
) = object : OneArgFunction() {
    override fun call(arg: LuaValue?): LuaValue {
        return block(arg!!)
    }
}

fun twoArgFunction(
    block: (LuaValue, LuaValue) -> LuaValue,
) = object : TwoArgFunction() {
    override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
        return block(arg1!!, arg2!!)
    }
}

fun threeArgFunction(
    block: (LuaValue, LuaValue, LuaValue) -> LuaValue,
) = object : ThreeArgFunction() {
    override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue {
        return block(arg1!!, arg2!!, arg3!!)
    }
}