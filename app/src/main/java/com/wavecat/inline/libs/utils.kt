@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.service.commands.Query
import com.wavecat.inline.utils.ArgumentTokenizer
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Lua library providing various utility functions.
 *
 * A collection of helper utilities for string manipulation, argument parsing,
 * and command validation. Used primarily for processing user input and
 * validating command arguments.
 *
 * @see ArgumentTokenizer
 * @see Query
 */
class utils : TwoArgFunction() {
    /**
     * Initializes the Lua library with utility functions.
     *
     * Creates and populates a Lua table with all available utility
     * functions for string processing and command handling.
     *
     * @param name The name of the library (unused)
     * @param env The Lua environment to register the library in
     * @return LuaValue The created library table
     * @see LuaValue
     */
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        /**
         * Splits a string by regex pattern into an array of strings.
         *
         * Divides the input string using the provided regular expression,
         * optionally limiting the number of resulting parts.
         *
         * param string The string to split
         * param regex The regular expression pattern to split by
         * param limit Maximum number of parts (0 = no limit)
         * @return table Array of split string parts
         * @see String.split
         */
        library["split"] = threeArgFunction { string: LuaValue, regex: LuaValue, limit: LuaValue ->
            val str = string.checkjstring()
            val regexStr = regex.checkjstring().toRegex()
            val limitVal = limit.optint(0)

            LuaTable().apply {
                str.split(regexStr, limitVal).forEachIndexed { index, s ->
                    set(index + 1, valueOf(s))
                }
            }
        }

        /**
         * Escapes special characters in a string for Lua patterns.
         *
         * Escapes characters that have special meaning in Lua patterns
         * by prefixing them with a percent sign (%).
         *
         * param string The string to escape
         * @return string The escaped string safe for use in Lua patterns
         * @see Regex.replace
         */
        library["escape"] = oneArgFunction { string ->
            @Suppress("RegExpRedundantEscape")
            valueOf(string.checkjstring().replace("[().%+\\-\\*\\?\\[^$\\]]".toRegex(), "%$0"))
        }

        /**
         * Parses a string into an array of arguments using shell-like rules.
         *
         * Tokenizes the input string using Unix shell-like parsing rules,
         * respecting quotes and escape sequences.
         *
         * param string The string to parse
         * @return table Array of parsed arguments
         * @see ArgumentTokenizer.tokenize
         */
        library["parseArgs"] = oneArgFunction { string ->
            CoerceJavaToLua.coerce(ArgumentTokenizer.tokenize(string.checkjstring()).toTypedArray())
        }

        /**
         * Wraps a command function with argument count validation.
         *
         * Creates a wrapper function that validates the exact number of arguments
         * before calling the wrapped command. Arguments are automatically parsed
         * and passed as a table to the command function.
         *
         * param value The command function to wrap
         * param count The expected number of arguments
         * param errorValue Optional error handler function
         * @return function The wrapped command function
         * @see ArgumentTokenizer.tokenize
         * @see Query.answer
         */
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

        /**
         * Wraps a command function with argument presence validation.
         *
         * Creates a wrapper function that ensures the command has any arguments
         * before calling the wrapped command. Unlike command, this doesn't
         * parse arguments or validate their count - it only checks for presence.
         *
         * param value The command function to wrap
         * param errorValue Optional error handler function
         * @return function The wrapped command function
         * @see Query.args
         * @see Query.answer
         */
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
