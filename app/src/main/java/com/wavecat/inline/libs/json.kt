@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
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

class json : TwoArgFunction() {
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        library["dump"] = oneArgFunction { table ->
            valueOf(dumpTable(table.checktable(), HashSet()).toString())
        }

        library["dumpObject"] = oneArgFunction { table ->
            CoerceJavaToLua.coerce(dumpTable(table.checktable(), HashSet()))
        }

        library["load"] = oneArgFunction { jsonString ->
            load(JSONTokener(jsonString.checkjstring()).nextValue())
        }

        library["loadObject"] = oneArgFunction { jsonObject ->
            load(jsonObject.touserdata())
        }

        library["emptyObject"] = LuaUserdata(JSONObject())
        library["null"] = Null

        env["json"] = library
        env["package"]["loaded"]["json"] = library

        return library
    }

    companion object {
        private val Null: LuaValue = LuaUserdata(Any())

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

        private fun dumpTable(value: LuaValue, stack: MutableSet<LuaValue>): Any {
            if (!stack.add(value))
                error("circular reference")

            val firstKey = value.next(LuaValue.NIL).arg1()
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

        private fun load(kobject: Any): LuaValue = when (kobject) {
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
