@file:Suppress("unused", "ClassName")

package com.wavecat.inline.libs

import android.view.accessibility.AccessibilityNodeInfo
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.service.Query
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

class menu : TwoArgFunction() {
    private val menuMap = mutableMapOf<AccessibilityNodeInfo, Context>()
    private val menuWatcher = oneArgFunction { arg ->
        val accessibilityNodeInfo =
            arg.checkuserdata(AccessibilityNodeInfo::class.java) as AccessibilityNodeInfo

        val context = menuMap[accessibilityNodeInfo] ?: return@oneArgFunction NIL

        val text = accessibilityNodeInfo.text
        if (text == null || text.length != context.length) {
            menuMap.remove(accessibilityNodeInfo)

            if (context.cancelAction.isnil()) {
                context.query.answer(null)
            } else {
                context.cancelAction.call(arg, CoerceJavaToLua.coerce(context.query))
            }

            return@oneArgFunction NIL
        }

        context.parts.firstOrNull { part ->
            accessibilityNodeInfo.textSelectionStart in (part.start + 1) until part.end &&
                    accessibilityNodeInfo.textSelectionEnd in (part.start + 1) until part.end
        }?.let { part ->
            menuMap.remove(accessibilityNodeInfo)
            part.action.call(arg, CoerceJavaToLua.coerce(context.query))
        }

        NIL
    }

    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        library["create"] = threeArgFunction { arg1, arg2, arg3 ->
            val result = StringBuilder()
            val query = arg1.checkuserdata(Query::class.java) as Query
            val parts = hashSetOf<Part>()

            arg2.checktable().forEach { _, v ->
                if (v is LuaTable) {
                    val caption = v["caption"].tojstring()
                    parts.add(
                        Part(
                            start = query.startPosition + result.length,
                            end = query.startPosition + result.length + caption.length,
                            action = v["action"]
                        )
                    )
                    result.append(caption)
                } else {
                    result.append(v.tojstring())
                }
            }

            query.answer(result.toString())

            val context = Context(
                query,
                parts,
                arg3,
                query.text.length
            )

            menuMap[query.accessibilityNodeInfo] = context
            CoerceJavaToLua.coerce(context)
        }

        library["map"] = CoerceJavaToLua.coerce(menuMap)

        env["menu"] = library
        env["package"]["loaded"]["menu"] = library

        requireService().allWatchers[menuWatcher] = InlineService.TYPE_SELECTION_CHANGED

        return library
    }

    data class Context(
        val query: Query,
        val parts: Set<Part>,
        val cancelAction: LuaValue,
        val length: Int,
    )

    data class Part(
        val start: Int,
        val end: Int,
        val action: LuaValue,
    )
}


