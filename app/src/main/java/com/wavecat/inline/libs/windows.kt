@file:Suppress("ClassName", "NewApi", "unused")

package com.wavecat.inline.libs

import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.material.color.DynamicColors
import com.wavecat.inline.R
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.threeArgFunction
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.extensions.zeroArgFunction
import com.wavecat.inline.libs.windows.Companion.closeAll
import com.wavecat.inline.libs.windows.Companion.windows
import com.wavecat.inline.preferences.FloatingWindow
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.requireService
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Lua library for creating and managing floating windows.
 *
 * Allows displaying custom UI over other applications.
 * Used for interactive tools, quick access panels, and similar functionality.
 *
 * @see FloatingWindow
 * @see InlineService
 */
class windows : TwoArgFunction() {
    /**
     * Context with dynamic colors and theme for floating windows.
     *
     * Lazily initialized wrapped context that applies Material Design dynamic colors
     * and the application theme to floating windows.
     *
     * @see DynamicColors
     * @see ContextThemeWrapper
     */
    private val wrappedContext by lazy {
        DynamicColors.wrapContextIfAvailable(
            ContextThemeWrapper(requireService(), R.style.Theme_Inline)
        )
    }

    /**
     * Latest AccessibilityNodeInfo that user interacted with.
     *
     * Stores the most recent accessibility node that received user interaction,
     * used for text insertion operations.
     *
     * @see AccessibilityNodeInfo
     */
    private var latestAccessibilityNodeInfo: AccessibilityNodeInfo? = null

    /**
     * Flag indicating that focus is on the current application.
     *
     * When true, prevents text insertion operations to avoid interfering
     * with the application's own UI.
     */
    private var isFocusedOnSelf: Boolean = false


    /**
     * Watcher for AccessibilityNodeInfo changes.
     *
     * Tracks text changes and selection changes in accessibility nodes.
     * Updates [latestAccessibilityNodeInfo] and [isFocusedOnSelf] accordingly.
     */
    private val latestNodeWatcher = oneArgFunction { arg ->
        val accessibilityNodeInfo =
            arg.checkuserdata(AccessibilityNodeInfo::class.java) as AccessibilityNodeInfo

        isFocusedOnSelf = accessibilityNodeInfo.packageName == wrappedContext.packageName

        if (!isFocusedOnSelf)
            latestAccessibilityNodeInfo = accessibilityNodeInfo

        NIL
    }

    /**
     * Flag indicating whether text insertion is supported.
     *
     * Must be enabled via [supportInsert] before text insertion operations
     * can be performed.
     */
    private var supportsInsert = false

    /**
     * Enables the AccessibilityNodeInfo watcher.
     *
     * Registers the [latestNodeWatcher] to receive notifications about
     * text changes and selection changes.
     */
    private fun enableWatcher() {
        requireService().apply {
            allWatchers[latestNodeWatcher] =
                InlineService.TYPE_TEXT_CHANGED or InlineService.TYPE_SELECTION_CHANGED
        }
    }

    /**
     * Disables the AccessibilityNodeInfo watcher.
     *
     * Removes the [latestNodeWatcher] from receiving notifications.
     */
    private fun disableWatcher() {
        requireService().apply {
            allWatchers.remove(latestNodeWatcher)
        }
    }

    /**
     * Adds insert support to a FloatingWindow.
     *
     * Enables text insertion capabilities for the window and manages
     * the watcher lifecycle based on active windows.
     *
     * @receiver FloatingWindow The window to add insert support to
     * @see FloatingWindow.onClose
     */
    private fun FloatingWindow.supportInsert() {
        if (supportsInsert)
            enableWatcher()

        windows.add(this)

        onClose = {
            windows.remove(this)
            if (supportsInsert && windows.isEmpty())
                disableWatcher()
        }
    }

    companion object {
        /**
         * Set of all active floating windows.
         *
         * Tracks all currently open floating windows for management purposes.
         */
        private val windows = mutableSetOf<FloatingWindow>()

        /**
         * Closes all active floating windows.
         *
         * Iterates through all windows in [windows] and calls their close method.
         *
         * @see FloatingWindow.close
         */
        fun closeAll() = windows.forEach { it.close() }
    }

    /**
     * Initializes the Lua library with floating window functions.
     *
     * Creates and populates a Lua table with all available floating window
     * operations and utilities.
     *
     * @param name The name of the library (unused)
     * @param env The Lua environment to register the library in
     * @return LuaValue The created library table
     * @see LuaValue
     */
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library = tableOf()

        /**
         * Checks if floating windows are supported on this device.
         *
         * @return boolean True if API level is 22 or higher
         */
        library["isSupported"] = zeroArgFunction {
            valueOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
        }

        /**
         * Creates a new floating window.
         *
         * param config Configuration table for the window
         * param builder Function that returns the window's UI structure
         * @return FloatingWindow The created floating window
         * @see FloatingWindow.configure
         * @see FloatingWindow.create
         */
        library["create"] = twoArgFunction { config, builder ->
            CoerceJavaToLua.coerce(FloatingWindow(wrappedContext).apply {
                configure(config)
                create(builder)
                supportInsert()
            })
        }

        /**
         * Creates a floating window aligned to an AccessibilityNodeInfo.
         *
         * param accessibilityNodeInfo The node to align the window to
         * param config Configuration table for the window
         * param builder Function that returns the window's UI structure
         * @return FloatingWindow The created floating window aligned to the node
         * @see FloatingWindow.alignToNode
         * @see AccessibilityNodeInfo
         */
        library["createAligned"] = threeArgFunction { accessibilityNodeInfo, config, builder ->
            val node = accessibilityNodeInfo.checkuserdata() as AccessibilityNodeInfo
            CoerceJavaToLua.coerce(FloatingWindow(wrappedContext).apply {
                alignToNode(node, config)
                configure(config)
                create(builder)
                supportInsert()
            })
        }

        /**
         * Gets the screen bounds of an AccessibilityNodeInfo.
         *
         * param accessibilityNodeInfo The node to get bounds for
         * @return Rect The bounds rectangle in screen coordinates
         * @see AccessibilityNodeInfo.getBoundsInScreen
         */
        library["getBoundsInScreen"] = oneArgFunction { accessibilityNodeInfo ->
            CoerceJavaToLua.coerce(Rect().apply {
                val node = accessibilityNodeInfo.checkuserdata() as AccessibilityNodeInfo
                node.getBoundsInScreen(this)
            })
        }

        /**
         * Gets the screen width in pixels.
         *
         * @return int The screen width in pixels
         * @see Resources.getSystem
         */
        library["getScreenWidth"] = zeroArgFunction {
            valueOf(Resources.getSystem().displayMetrics.widthPixels)
        }

        /**
         * Gets the screen height in pixels.
         *
         * @return int The screen height in pixels
         * @see Resources.getSystem
         */
        library["getScreenHeight"] = zeroArgFunction {
            valueOf(Resources.getSystem().displayMetrics.heightPixels)
        }

        /**
         * Inserts text at the cursor position in the last interacted node.
         *
         * param text The text to insert
         * @return AccessibilityNodeInfo|false The node where text was inserted, or false if failed
         * @see AccessibilityNodeInfo.ACTION_FOCUS
         * @see InlineService.insertText
         */
        library["insertText"] = oneArgFunction { text ->
            val nodeInfo = latestAccessibilityNodeInfo

            if (nodeInfo == null || isFocusedOnSelf) {
                FALSE
            } else {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                InlineService.insertText(nodeInfo, text.checkjstring())
                CoerceJavaToLua.coerce(nodeInfo)
            }
        }

        /**
         * Enables text insertion support.
         *
         * Must be called once to enable text insertion capabilities.
         * This starts monitoring accessibility events for text insertion.
         *
         * @see enableWatcher
         */
        library["supportInsert"] = zeroArgFunction {
            supportsInsert = true
            NIL
        }

        /**
         * Closes all active floating windows.
         *
         * @see closeAll
         */
        library["closeAll"] = zeroArgFunction {
            closeAll()
            NIL
        }

        env["windows"] = library
        env["package"]["loaded"]["windows"] = library

        return library
    }
}
