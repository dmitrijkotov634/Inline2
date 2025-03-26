@file:Suppress("unused", "ClassName")

package com.wavecat.inline.libs

import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

class utf8 : TwoArgFunction() {
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        library["len"] = oneArgFunction { string -> valueOf(string.checkjstring().length) }
        library["sub"] = threeArgFunction { string, start, end ->
            val str = string.checkjstring()
            val startIndex = start.optint(0)
            val endIndex = end.optint(str.length).coerceAtMost(str.length)

            valueOf(str.substring(startIndex, endIndex))
        }

        env["utf8"] = library

        // The only way
        env.checkglobals().load("utf8.charpattern = \"[\\0-\\x7F\\xC2-\\xF4][\\x80-\\xBF]*\"").call()

        env["package"]["loaded"]["utf8"] = library

        return library
    }
}
