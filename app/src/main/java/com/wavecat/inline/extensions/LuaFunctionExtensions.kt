package com.wavecat.inline.extensions

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * Creates a variable argument Lua function from a Kotlin lambda.
 *
 * Extension function that wraps a Kotlin lambda accepting Varargs
 * into a VarArgFunction that can be called from Lua with any number
 * of arguments.
 *
 * @param block Lambda function that processes variable arguments
 * @return VarArgFunction Lua function accepting variable arguments
 * @see VarArgFunction
 * @see Varargs
 */
fun varArgFunction(block: (Varargs) -> Varargs) = object : VarArgFunction() {
    override fun onInvoke(args: Varargs?): Varargs {
        args ?: return NIL
        return block(args)
    }
}

/**
 * Creates a zero-argument Lua function from a Kotlin lambda.
 *
 * Extension function that wraps a Kotlin lambda taking no parameters
 * into a ZeroArgFunction that can be called from Lua without arguments.
 *
 * @param block Lambda function that returns a LuaValue
 * @return ZeroArgFunction Lua function accepting no arguments
 * @see ZeroArgFunction
 * @see LuaValue
 */
fun zeroArgFunction(
    block: () -> LuaValue,
) = object : ZeroArgFunction() {
    override fun call(): LuaValue {
        return block()
    }
}

/**
 * Creates a single-argument Lua function from a Kotlin lambda.
 *
 * Extension function that wraps a Kotlin lambda accepting one LuaValue
 * into a OneArgFunction that can be called from Lua with one argument.
 *
 * @param block Lambda function that processes one LuaValue argument
 * @return OneArgFunction Lua function accepting one argument
 * @see OneArgFunction
 * @see LuaValue
 */
fun oneArgFunction(
    block: (LuaValue) -> LuaValue,
) = object : OneArgFunction() {
    override fun call(arg: LuaValue?): LuaValue {
        return block(arg!!)
    }
}

/**
 * Creates a two-argument Lua function from a Kotlin lambda.
 *
 * Extension function that wraps a Kotlin lambda accepting two LuaValues
 * into a TwoArgFunction that can be called from Lua with two arguments.
 *
 * @param block Lambda function that processes two LuaValue arguments
 * @return TwoArgFunction Lua function accepting two arguments
 * @see TwoArgFunction
 * @see LuaValue
 */
fun twoArgFunction(
    block: (LuaValue, LuaValue) -> LuaValue,
) = object : TwoArgFunction() {
    override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
        return block(arg1!!, arg2!!)
    }
}

/**
 * Creates a three-argument Lua function from a Kotlin lambda.
 *
 * Extension function that wraps a Kotlin lambda accepting three LuaValues
 * into a ThreeArgFunction that can be called from Lua with three arguments.
 *
 * @param block Lambda function that processes three LuaValue arguments
 * @return ThreeArgFunction Lua function accepting three arguments
 * @see ThreeArgFunction
 * @see LuaValue
 */
fun threeArgFunction(
    block: (LuaValue, LuaValue, LuaValue) -> LuaValue,
) = object : ThreeArgFunction() {
    override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue {
        return block(arg1!!, arg2!!, arg3!!)
    }
}