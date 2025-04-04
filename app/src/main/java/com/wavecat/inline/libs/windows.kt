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
import com.wavecat.inline.preferences.FloatingWindow
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.requireService
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua

class windows : TwoArgFunction() {
    private var latestAccessibilityNodeInfo: AccessibilityNodeInfo? = null
    private var isFocusedOnSelf: Boolean = false

    private val wrappedContext by lazy {
        DynamicColors.wrapContextIfAvailable(
            ContextThemeWrapper(requireService(), R.style.Theme_Inline)
        )
    }

    private val latestNodeWatcher = oneArgFunction { arg ->
        val accessibilityNodeInfo =
            arg.checkuserdata(AccessibilityNodeInfo::class.java) as AccessibilityNodeInfo

        isFocusedOnSelf = accessibilityNodeInfo.packageName == wrappedContext.packageName

        if (!isFocusedOnSelf)
            latestAccessibilityNodeInfo = accessibilityNodeInfo

        NIL
    }

    private var supportsInsert = false
    private val windows = mutableSetOf<FloatingWindow>()

    private fun enableWatcher() {
        requireService().apply {
            allWatchers[latestNodeWatcher] =
                InlineService.TYPE_TEXT_CHANGED or InlineService.TYPE_SELECTION_CHANGED
        }
    }

    private fun disableWatcher() {
        requireService().apply {
            allWatchers.remove(latestNodeWatcher)
        }
    }

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

    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library = tableOf()

        library["isSupported"] = zeroArgFunction {
            valueOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
        }

        library["create"] = twoArgFunction { config, builder ->
            CoerceJavaToLua.coerce(FloatingWindow(wrappedContext).apply {
                configure(config)
                create(builder)
                supportInsert()
            })
        }

        library["createAligned"] = threeArgFunction { accessibilityNodeInfo, config, builder ->
            val node = accessibilityNodeInfo.checkuserdata() as AccessibilityNodeInfo
            CoerceJavaToLua.coerce(FloatingWindow(wrappedContext).apply {
                alignToNode(node, config)
                configure(config)
                create(builder)
                supportInsert()
            })
        }

        library["getBoundsInScreen"] = oneArgFunction { accessibilityNodeInfo ->
            CoerceJavaToLua.coerce(Rect().apply {
                val node = accessibilityNodeInfo.checkuserdata() as AccessibilityNodeInfo
                node.getBoundsInScreen(this)
            })
        }

        library["getScreenWidth"] = zeroArgFunction {
            valueOf(Resources.getSystem().displayMetrics.widthPixels)
        }

        library["getScreenHeight"] = zeroArgFunction {
            valueOf(Resources.getSystem().displayMetrics.heightPixels)
        }

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

        library["supportInsert"] = zeroArgFunction {
            supportsInsert = true
            NIL
        }

        library["closeAll"] = zeroArgFunction {
            windows.forEach { it.close() }
            NIL
        }

        env["windows"] = library
        env["package"]["loaded"]["windows"] = library

        return library
    }
}
