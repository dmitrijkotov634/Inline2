package com.wavecat.inline.extensions

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs

/**
 * Provides array-like access to Varargs by index.
 *
 * Operator function that allows accessing Varargs elements using
 * square bracket notation, similar to array access in Kotlin.
 *
 * @param index The 1-based index of the argument to retrieve
 * @return LuaValue The argument at the specified index
 * @receiver Varargs The variable arguments collection
 * @see Varargs.arg
 */
operator fun Varargs.get(index: Int): LuaValue = arg(index)

/**
 * Iterates over each argument in Varargs.
 *
 * Extension function that applies a lambda to each argument in the
 * Varargs collection, providing a convenient iteration mechanism.
 *
 * @param block Lambda function to execute for each argument
 * @receiver Varargs The variable arguments collection
 * @see Varargs.narg
 * @see Varargs.arg
 */
fun Varargs.forEachVararg(block: (LuaValue) -> Unit) {
    for (i in 1..narg())
        block(arg(i))
}

/**
 * Iterates over each argument in Varargs with index information.
 *
 * Extension function that applies a lambda to each argument along with
 * its 1-based index position in the Varargs collection.
 *
 * @param block Lambda function receiving the argument and its index
 * @receiver Varargs The variable arguments collection
 * @see Varargs.narg
 * @see Varargs.arg
 */
fun Varargs.forEachVarargIndexed(block: (LuaValue, Int) -> Unit) {
    for (i in 1..narg())
        block(arg(i), i)
}

/**
 * Converts Varargs to a Kotlin List of LuaValues.
 *
 * Extension function that extracts all arguments from Varargs
 * and returns them as a standard Kotlin List for easier manipulation.
 *
 * @return List<LuaValue> All arguments as a list
 * @receiver Varargs The variable arguments collection
 * @see forEachVararg
 */
fun Varargs.unpackVarargs(): List<LuaValue> = mutableListOf<LuaValue>().apply {
    this@unpackVarargs.forEachVararg {
        add(it)
    }
}

/**
 * Creates Varargs from individual LuaValue arguments.
 *
 * Utility function that constructs a Varargs object from a variable
 * number of LuaValue parameters using Kotlin's vararg syntax.
 *
 * @param luaValues Variable number of LuaValue arguments
 * @return Varargs Collection containing all provided arguments
 * @see LuaValue.varargsOf
 */
fun varargsOf(vararg luaValues: LuaValue): Varargs = LuaValue.varargsOf(luaValues)

/**
 * Converts an Array of LuaValues to Varargs.
 *
 * Extension function that packs an array of LuaValues into
 * a Varargs collection for use in Lua function calls.
 *
 * @return Varargs Collection containing all array elements
 * @receiver Array<LuaValue> The array to convert
 * @see varargsOf
 */
fun Array<LuaValue>.packVarargs() = varargsOf(*this)

/**
 * Converts a List of LuaValues to Varargs.
 *
 * Extension function that packs a list of LuaValues into
 * a Varargs collection for use in Lua function calls.
 *
 * @return Varargs Collection containing all list elements
 * @receiver List<LuaValue> The list to convert
 * @see varargsOf
 * @see List.toTypedArray
 */
fun List<LuaValue>.packVarargs() = varargsOf(*this.toTypedArray())

/**
 * Destructuring component for the first argument.
 *
 * @return LuaValue The first argument (index 1)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component1(): LuaValue = arg(1)

/**
 * Destructuring component for the second argument.
 *
 * @return LuaValue The second argument (index 2)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component2(): LuaValue = arg(2)

/**
 * Destructuring component for the third argument.
 *
 * @return LuaValue The third argument (index 3)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component3(): LuaValue = arg(3)

/**
 * Destructuring component for the fourth argument.
 *
 * @return LuaValue The fourth argument (index 4)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component4(): LuaValue = arg(4)

/**
 * Destructuring component for the fifth argument.
 *
 * @return LuaValue The fifth argument (index 5)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component5(): LuaValue = arg(5)

/**
 * Destructuring component for the sixth argument.
 *
 * @return LuaValue The sixth argument (index 6)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component6(): LuaValue = arg(6)

/**
 * Destructuring component for the seventh argument.
 *
 * @return LuaValue The seventh argument (index 7)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component7(): LuaValue = arg(7)

/**
 * Destructuring component for the eighth argument.
 *
 * @return LuaValue The eighth argument (index 8)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component8(): LuaValue = arg(8)

/**
 * Destructuring component for the ninth argument.
 *
 * @return LuaValue The ninth argument (index 9)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component9(): LuaValue = arg(9)

/**
 * Destructuring component for the tenth argument.
 *
 * @return LuaValue The tenth argument (index 10)
 * @receiver Varargs The variable arguments collection
 */
operator fun Varargs.component10(): LuaValue = arg(10)