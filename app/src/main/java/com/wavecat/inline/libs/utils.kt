@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import com.wavecat.inline.service.Query
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.utils.ArgumentTokenizer
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

class utils : TwoArgFunction() {
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        library["split"] = threeArgFunction { string: LuaValue, regex: LuaValue, limit: LuaValue ->
            val str = string.checkjstring()
            val regexStr = regex.checkjstring().toRegex()
            val limitVal = limit.optint(0)

            CoerceJavaToLua.coerce(
                str.split(regexStr, limitVal).toTypedArray()
            )
        }

        library["escape"] = oneArgFunction { string ->
            @Suppress("RegExpRedundantEscape")
            valueOf(string.checkjstring().replace("[().%+\\-\\*\\?\\[^$\\]]".toRegex(), "%$0"))
        }

        library["parseArgs"] = oneArgFunction { string ->
            CoerceJavaToLua.coerce(ArgumentTokenizer.tokenize(string.checkjstring()).toTypedArray())
        }

        library["command"] = threeArgFunction { value, count, errorValue ->
            value.checkfunction()
            count.checkint()

            twoArgFunction { input, arg2 ->
                val query = (arg2.checkuserdata(Query::class.java) as Query)

                val args: Array<Any> = ArgumentTokenizer.tokenize(query.args).toTypedArray()

                if (args.size == count.toint())
                    return@twoArgFunction value.call(input, arg2, CoerceJavaToLua.coerce(args))

                if (errorValue.isfunction())
                    return@twoArgFunction errorValue.call(input, arg2)

                query.answer("Wrong arguments")
                NIL
            }
        }

        library["hasArgs"] = twoArgFunction { value, errorValue ->
            value.checkfunction()

            twoArgFunction wrapped@{ input, arg2 ->
                val query = (arg2.checkuserdata(Query::class.java) as Query)

                if (query.args.isEmpty()) {
                    if (errorValue.isfunction())
                        return@wrapped errorValue.call(input, arg2)

                    query.answer("Empty argument")
                    return@wrapped NIL
                }

                value.call(input, arg2)
            }
        }

        env["utils"] = library
        env["package"]["loaded"]["utils"] = library

        return library
    }
}
