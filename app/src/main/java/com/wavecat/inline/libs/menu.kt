@file:Suppress("unused", "ClassName")

package com.wavecat.inline.libs

import android.view.accessibility.AccessibilityNodeInfo
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.RECEIVE_SELECTION_CHANGES
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.service.commands.Query
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Lua library for creating clickable interactive text menus.
 *
 * Allows creation of interactive menus directly within text fields,
 * where users can click on specific text portions to trigger actions.
 * Menus are built using a combination of plain text and clickable elements.
 */
class menu : TwoArgFunction() {

    /**
     * Map storing active menu contexts associated with accessibility nodes.
     *
     * Tracks all currently active menus and their associated context data,
     * keyed by the AccessibilityNodeInfo where the menu is displayed.
     */
    private val menuMap = mutableMapOf<AccessibilityNodeInfo, Context>()

    /**
     * Watcher function for monitoring text selection changes in menu nodes.
     *
     * Monitors accessibility events to detect when users click on
     * interactive menu elements. Triggers appropriate actions when
     * selections fall within defined clickable areas.
     */
    private val menuWatcher = oneArgFunction { arg ->
        val accessibilityNodeInfo = arg.touserdata() as AccessibilityNodeInfo

        val context = menuMap[accessibilityNodeInfo] ?: return@oneArgFunction NIL

        val text = accessibilityNodeInfo.text
        if (text == null || text.length != context.length) {
            menuMap.remove(accessibilityNodeInfo)
            removeWatcher()

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
            removeWatcher()

            part.action.call(arg, CoerceJavaToLua.coerce(context.query))
        }

        NIL
    }

    /**
     * Preference flag indicating whether selection change events are enabled.
     *
     * Lazily loaded from shared preferences to determine if the system
     * should monitor text selection changes for menu interactions.
     *
     * @see InlineService.RECEIVE_SELECTION_CHANGES
     */
    private val receiveSelectionChangedEvents by lazy {
        requireService().defaultSharedPreferences.getBoolean(RECEIVE_SELECTION_CHANGES, true)
    }

    /**
     * Removes the menu watcher when no active menus remain.
     *
     * Cleans up event listeners when all menus have been closed
     * to avoid unnecessary event processing.
     *
     * @see InlineService.allWatchers
     */
    private fun removeWatcher() {
        requireService().apply {
            if (menuMap.isEmpty())
                allWatchers.remove(menuWatcher)
        }
    }

    /**
     * Initializes the Lua library with text menu functions.
     *
     * Creates and populates a Lua table with menu creation functions
     * and provides access to the active menu map.
     *
     * @param name The name of the library (unused)
     * @param env The Lua environment to register the library in
     * @return LuaValue The created library table
     * @see LuaValue
     */
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        /**
         * Creates a text menu and replaces the command text with the result.
         *
         * Builds an interactive menu from the provided items table, combining
         * plain text and clickable elements. Monitors selection changes to
         * detect user interactions with menu elements.
         *
         * param arg1 The Query object representing the text field
         * param arg2 Table describing the menu structure (items)
         * param arg3 Optional cancel action function
         * @return Context|false The menu context if successful, false if selection events disabled
         */
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

            query.answer(result.toString(), cursorToEnd = true)

            if (receiveSelectionChangedEvents) {
                val context = Context(
                    query,
                    parts,
                    arg3,
                    query.text.length
                )

                menuMap[query.accessibilityNodeInfo] = context
                requireService().allWatchers[menuWatcher] = InlineService.TYPE_SELECTION_CHANGED
                CoerceJavaToLua.coerce(context)
            } else {
                FALSE
            }
        }

        /**
         * Provides access to the active menu map for debugging.
         *
         * Exposes the internal menuMap for inspection and debugging purposes.
         *
         * @see menuMap
         */
        library["map"] = CoerceJavaToLua.coerce(menuMap)

        env["menu"] = library
        env["package"]["loaded"]["menu"] = library

        return library
    }

    /**
     * Context data for an active text menu.
     *
     * Stores all necessary information for managing an active menu,
     * including the query object, clickable parts, cancel action,
     * and original text length for validation.
     *
     * @property query The Query object where the menu is displayed
     * @property parts Set of clickable menu parts with their positions and actions
     * @property cancelAction Function to call if menu is cancelled
     * @property length Original text length for validation
     * @see Query
     * @see Part
     */
    data class Context(
        val query: Query,
        val parts: Set<Part>,
        val cancelAction: LuaValue,
        val length: Int,
    )

    /**
     * Represents a clickable part of a text menu.
     *
     * Defines a text range that triggers an action when clicked,
     * including start/end positions and the associated action function.
     *
     * @property start Starting character position (inclusive)
     * @property end Ending character position (exclusive)
     * @property action Lua function to execute when this part is clicked
     * @see LuaValue
     */
    data class Part(
        val start: Int,
        val end: Int,
        val action: LuaValue,
    )
}


