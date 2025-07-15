@file:Suppress("unused", "ClassName")

package com.wavecat.inline.libs

import com.wavecat.inline.extensions.forEachVararg
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.extensions.varArgFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

/**
 * Lua library for proper UTF-8 string handling.
 *
 * Provides functions for correct manipulation of UTF-8 encoded strings,
 * including character counting, substring extraction, and case checking.
 * All functions work with Unicode code points rather than raw bytes.
 */
class utf8 : TwoArgFunction() {

    /**
     * Initializes the Lua library with UTF-8 string functions.
     *
     * Creates and populates a Lua table with all available UTF-8
     * string manipulation functions and sets up the UTF-8 character pattern.
     *
     * @param name The name of the library (unused)
     * @param env The Lua environment to register the library in
     * @return LuaValue The created library table
     * @see LuaValue
     */
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        /**
         * Returns the number of characters (code points) in a UTF-8 string.
         *
         * Counts Unicode code points rather than bytes, providing accurate
         * character count for international text.
         *
         * param string The UTF-8 string to measure
         * @return number The number of characters in the string
         * @see String.length
         */
        library["len"] = oneArgFunction { string -> valueOf(string.checkjstring().length) }

        /**
         * Returns a substring of a UTF-8 string using character indices.
         *
         * Extracts a portion of the string based on character positions
         * rather than byte positions, ensuring proper UTF-8 handling.
         *
         * param string The source UTF-8 string
         * param start The starting character index (0-based)
         * param end The ending character index (optional, defaults to string length)
         * @return string The extracted substring
         * @see String.substring
         */
        library["sub"] = threeArgFunction { string, start, end ->
            val str = string.checkjstring()
            val startIndex = start.optint(0)
            val endIndex = end.optint(str.length).coerceAtMost(str.length)

            valueOf(str.substring(startIndex, endIndex))
        }

        /**
         * Creates a string from a sequence of Unicode character codes.
         *
         * Takes a variable number of integer arguments, each representing
         * a Unicode code point, and combines them into a UTF-8 string.
         *
         * param ints Variable number of Unicode code point integers
         * @return string The resulting UTF-8 string
         * @see Char
         * @see StringBuilder.append
         */
        library["char"] = varArgFunction { ints ->
            valueOf(buildString {
                ints.forEachVararg {
                    append(it.checkint())
                }
            })
        }

        /**
         * Checks if a string consists only of lowercase characters.
         *
         * Determines whether the string contains only lowercase characters
         * and is not identical to its uppercase version (to exclude
         * strings with no case distinction).
         *
         * param value The string to check
         * @return boolean True if string is entirely lowercase and has case distinction
         * @see String.lowercase
         * @see String.uppercase
         */
        library["isLower"] = oneArgFunction { value ->
            val str = value.checkjstring()
            valueOf(str == str.lowercase() && str != str.uppercase())
        }

        /**
         * Checks if a string consists only of uppercase characters.
         *
         * Determines whether the string contains only uppercase characters
         * and is not identical to its lowercase version (to exclude
         * strings with no case distinction).
         *
         * param value The string to check
         * @return boolean True if string is entirely uppercase and has case distinction
         * @see String.lowercase
         * @see String.uppercase
         */
        library["isUpper"] = oneArgFunction { value ->
            val str = value.checkjstring()
            valueOf(str == str.uppercase() && str != str.lowercase())
        }

        env["utf8"] = library

        /**
         * Sets the UTF-8 character pattern for Lua string matching.
         *
         * Defines a Lua pattern that matches a single UTF-8 character,
         * covering all valid UTF-8 byte sequences.
         *
         * Pattern explanation:
         * - [\0-\x7F] matches ASCII characters (1 byte)
         * - [\xC2-\xF4] matches UTF-8 lead bytes (2-4 bytes)
         * - [\x80-\xBF]* matches UTF-8 continuation bytes
         */
        env.checkglobals().load("utf8.charpattern = \"[\\0-\\x7F\\xC2-\\xF4][\\x80-\\xBF]*\"").call()

        env["package"]["loaded"]["utf8"] = library

        return library
    }
}
