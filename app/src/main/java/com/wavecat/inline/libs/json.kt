@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.libs.json.Companion.Null
import com.wavecat.inline.libs.json.Companion.castValue
import com.wavecat.inline.libs.json.Companion.dumpTable
import com.wavecat.inline.libs.json.Companion.load
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.luaj.vm2.LuaBoolean
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua


/**
 * Lua library for JSON encoding and decoding.
 *
 * Provides functionality to serialize Lua tables to JSON strings/objects
 * and deserialize JSON data back to Lua tables. Supports all standard
 * JSON data types including objects, arrays, strings, numbers, booleans, and null.
 */
class json : TwoArgFunction() {

    /**
     * Initializes the Lua library with JSON functions.
     *
     * Creates and populates a Lua table with all available JSON
     * encoding and decoding functions, along with utility constants.
     *
     * @param name The name of the library (unused)
     * @param env The Lua environment to register the library in
     * @return LuaValue The created library table
     * @see LuaValue
     */
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        /**
         * Serializes a Lua table to a JSON string.
         *
         * Converts a Lua table into its JSON string representation,
         * automatically detecting arrays vs objects based on key types.
         *
         * param table The Lua table to serialize
         * @return string The JSON string representation
         * @throws error If circular references are detected
         * @see dumpTable
         */
        library["dump"] = oneArgFunction { table ->
            valueOf(dumpTable(table.checktable(), HashSet()).toString())
        }

        /**
         * Serializes a Lua table to a JSON object (JSONObject or JSONArray).
         *
         * Converts a Lua table into a native JSON object that can be
         * manipulated directly without string conversion.
         *
         * param table The Lua table to serialize
         * @return userdata JSONObject or JSONArray containing the serialized data
         * @throws error If circular references are detected
         * @see dumpTable
         * @see JSONObject
         * @see JSONArray
         */
        library["dumpObject"] = oneArgFunction { table ->
            CoerceJavaToLua.coerce(dumpTable(table.checktable(), HashSet()))
        }

        /**
         * Deserializes a JSON string to a Lua table.
         *
         * Parses a JSON string and converts it into equivalent Lua table
         * structures, preserving data types and nested structures.
         *
         * param jsonString The JSON string to parse
         * @return table The resulting Lua table
         * @throws org.json.JSONException If the JSON string is malformed
         * @see JSONTokener
         * @see load
         */
        library["load"] = oneArgFunction { jsonString ->
            load(JSONTokener(jsonString.checkjstring()).nextValue())
        }

        /**
         * Deserializes a JSON object (JSONObject or JSONArray) to a Lua table.
         *
         * Converts a native JSON object directly to Lua table structures
         * without requiring string parsing.
         *
         * param jsonObject The JSON object to convert
         * @return table The resulting Lua table
         * @see load
         * @see JSONObject
         * @see JSONArray
         */
        library["loadObject"] = oneArgFunction { jsonObject ->
            load(jsonObject.touserdata())
        }

        /**
         * Empty JSON object constant.
         *
         * Provides a reusable empty JSONObject instance for initialization
         * or as a base for building JSON structures.
         *
         * @see JSONObject
         */
        library["emptyObject"] = LuaUserdata(JSONObject())

        /**
         * JSON null value constant.
         *
         * Represents the JSON null value, distinct from Lua nil.
         * Used for explicit null values in JSON serialization.
         *
         * @see Null
         */
        library["null"] = Null

        env["json"] = library
        env["package"]["loaded"]["json"] = library

        return library
    }

    companion object {
        /**
         * Singleton representing JSON null value.
         *
         * Special LuaUserdata instance used to distinguish JSON null
         * from Lua nil values during serialization/deserialization.
         */
        private val Null: LuaValue = LuaUserdata(Any())


        /**
         * Converts a Lua value to its JSON equivalent.
         *
         * Handles type conversion from Lua types to JSON-compatible types,
         * including special handling for JSON objects, arrays, and null values.
         *
         * @param value The Lua value to convert
         * @param stack Set tracking visited values to detect circular references
         * @return Any? The JSON-compatible representation
         * @throws error If the value type cannot be serialized or circular reference detected
         * @see JSONObject.NULL
         * @see dumpTable
         */
        private fun castValue(value: LuaValue, stack: MutableSet<LuaValue>): Any? = when {
            value.isuserdata(JSONObject::class.java) || value.isuserdata(JSONArray::class.java) ->
                value.touserdata(Any::class.java)

            value == Null -> JSONObject.NULL
            value is LuaBoolean -> value.toboolean()
            value is LuaInteger -> value.toint()
            value is LuaDouble -> value.todouble()
            value is LuaString -> value.tojstring()
            value is LuaTable -> dumpTable(value, stack)

            else -> error("Unable to serialize ${value.typename()}")
        }


        /**
         * Converts a Lua table to JSONObject or JSONArray.
         *
         * Determines whether to create a JSONArray (for numeric indices)
         * or JSONObject (for string keys) based on the first key type.
         * Recursively processes nested tables while detecting circular references.
         *
         * @param value The Lua table to convert
         * @param stack Set tracking visited tables to prevent infinite recursion
         * @return Any JSONObject or JSONArray containing the converted data
         * @throws error If circular reference is detected
         * @see JSONObject
         * @see JSONArray
         *
         * @see castValue
         */
        fun dumpTable(value: LuaValue, stack: MutableSet<LuaValue>): Any {
            if (!stack.add(value))
                error("circular reference")

            val firstKey = value.next(NIL).arg1()
            return if (firstKey.isnumber()) {
                JSONArray().apply {
                    value.forEach { _, v -> put(castValue(v, stack)) }
                }
            } else {
                JSONObject().apply {
                    value.forEach { k, v -> put(k.checkjstring(), castValue(v, stack)) }
                }
            }
        }

        /**
         * Converts JSON objects to Lua tables recursively.
         *
         * Handles conversion from JSON data structures (JSONObject, JSONArray)
         * to equivalent Lua table structures, preserving nesting and data types.
         *
         * @param kobject The JSON object to convert
         * @return LuaValue The equivalent Lua table or primitive value
         * @see JSONObject
         * @see JSONArray
         * @see LuaTable
         * @see CoerceJavaToLua.coerce
         */
        fun load(kobject: Any): LuaValue = when (kobject) {
            is JSONObject -> LuaTable().apply {
                kobject.keys().forEach { key -> this[key] = load(kobject[key]) }
            }

            is JSONArray -> LuaTable().apply {
                for (i in 0 until kobject.length()) {
                    this[i + 1] = load(kobject[i])
                }
            }

            else -> CoerceJavaToLua.coerce(kobject)
        }
    }
}
