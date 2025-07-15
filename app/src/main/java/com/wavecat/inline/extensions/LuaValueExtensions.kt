package com.wavecat.inline.extensions

import org.luaj.vm2.LuaValue

/**
 * Iterates over all key-value pairs in a Lua table.
 *
 * Extension function that provides a convenient way to iterate through
 * all entries in a Lua table, similar to Lua's pairs() function.
 * Uses the table's next() method to traverse entries in order.
 *
 * @param process Lambda function called for each key-value pair
 * @receiver LuaValue The Lua table to iterate over
 * @see LuaValue.next
 * @see LuaValue.NIL
 */
fun LuaValue.forEach(process: (key: LuaValue, value: LuaValue) -> Unit) {
    var k: LuaValue = LuaValue.NIL
    while (true) {
        val n = next(k)
        k = n.arg1()
        if (k.isnil())
            break
        val v = n.arg(2)
        process(k, v)
    }
}
