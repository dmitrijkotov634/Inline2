@file:Suppress("unused", "MemberVisibilityCanBePrivate", "ClickableViewAccessibility")

package com.wavecat.inline.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.zeroArgFunction
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.utils.dp
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.valueOf
import org.luaj.vm2.lib.jse.CoerceJavaToLua


/**
 * Represents a floating window that can be displayed on top of other applications.
 * This class provides functionality to create, configure, and manage a floating window
 * that can contain various preference items.
 *
 * The window's appearance and behavior can be customized, including:
 * - **Appearance:** Corner radius, padding, background color.
 * - **Positioning:** Initial X/Y coordinates, gravity.
 * - **Behavior:** Autofocus, ability to move by touch, layout limits, background visibility.
 *
 * It integrates with Lua scripting for defining the content and behavior of the window.
 *
 * @property context The application context.
 * @property sharedPreferences The [SharedPreferences] instance used to store and retrieve preference values.
 * Defaults to the service's default SharedPreferences.
 * @property cornerRadius The radius of the window's corners in dp. Default is 16.
 * @property padding An array of integers representing the padding for left, top, right, and bottom in dp.
 * Default is `intArrayOf(16, 16, 16, 16)`.
 * @property backgroundColor The background color of the window. Defaults to the window background color
 * defined in the current theme, or [Color.WHITE] if not found.
 * @property positionX The initial X position of the window on the screen. Default is 0.
 * @property positionY The initial Y position of the window on the screen. Default is 0.
 * @property windowGravity The gravity of the window, determining its initial placement.
 * See [android.view.Gravity]. Default is [Gravity.CENTER].
 * @property autoFocus If true, the window will attempt to gain focus when interacted with and lose focus
 * when touched outside. Default is true.
 * @property noLimits If true, the window layout will not be constrained by screen edges.
 * See [WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS]. Default is false.
 * @property noBackground If true, the window will not have a background drawable. Default is false.
 * @property allowTouchMove If true, the window can be moved by dragging it. Default is true.
 * @property mWindowManager The [WindowManager] service instance.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class FloatingWindow(private val context: Context) {
    var sharedPreferences = requireService().defaultSharedPreferences
    var cornerRadius = 16
    var padding = intArrayOf(16, 16, 16, 16)
    var backgroundColor: Int = Color.WHITE

    var positionX = 0
    var positionY = 0

    var windowGravity = Gravity.CENTER

    init {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
            backgroundColor = typedValue.data
        }
    }

    var autoFocus = true
    var noLimits = false
    var noBackground = false
    var allowTouchMove = true

    val mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var mLayout: LinearLayout? = null

    var onClose: (() -> Unit)? = null

    private val builder = Builder(context = context).apply {
        set("windowManager", CoerceJavaToLua.coerce(mWindowManager))
        set("isFocused", zeroArgFunction { valueOf(!isNotFocused()) })
        set("close", zeroArgFunction {
            close()
            LuaValue.NIL
        })

        set("onMove", oneArgFunction { LuaValue.NIL })
        set("onFocusChanged", oneArgFunction { LuaValue.NIL })
        set("onClose", zeroArgFunction { LuaValue.NIL })
    }

    /**
     * Aligns the floating window relative to an [AccessibilityNodeInfo].
     * This function calculates the `positionX`, `positionY`, and `windowGravity`
     * of the floating window based on the bounds of the provided node and
     * configuration options.
     *
     * The configuration options are read from a Lua table (`config`) and include:
     * - `offsetX`: (Integer, optional, default: 0) Horizontal offset in dp from the node's edge.
     * - `offsetY`: (Integer, optional, default: 0) Vertical offset in dp from the node's edge.
     * - `alignment`: (String, optional, default: "left") Horizontal alignment relative to the node.
     *   Can be "left" or "right".
     * - `position`: (String, optional, default: "above") Vertical position relative to the node.
     *   Can be "above" or "below".
     *
     * @param accessibilityNodeInfo The [AccessibilityNodeInfo] to align to.
     * @param config A [LuaValue] (table) containing alignment and offset configurations.
     */
    @SuppressLint("RtlHardcoded")
    fun alignToNode(accessibilityNodeInfo: AccessibilityNodeInfo, config: LuaValue) {
        val nodeRect = Rect().apply { accessibilityNodeInfo.getBoundsInScreen(this) }

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        val offsetX = config.get("offsetX").optint(0).dp
        val offsetY = config.get("offsetY").optint(0).dp
        val alignment = config.get("alignment").optjstring("left")
        val position = config.get("position").optjstring("above")

        positionX = if (alignment == "right") {
            screenWidth - nodeRect.left - offsetX
        } else {
            nodeRect.left - offsetX
        }

        positionY = if (position == "above") {
            screenHeight - nodeRect.top + offsetY
        } else {
            nodeRect.bottom - offsetY
        }

        windowGravity = when {
            alignment == "right" && position == "above" -> Gravity.BOTTOM or Gravity.RIGHT
            alignment == "right" && position == "below" -> Gravity.TOP or Gravity.RIGHT
            alignment == "left" && position == "above" -> Gravity.BOTTOM or Gravity.LEFT
            else -> Gravity.TOP or Gravity.LEFT
        }
    }

    fun configure(config: LuaValue) {
        with(config) {
            cornerRadius = get("cornerRadius").optint(cornerRadius)

            padding = intArrayOf(
                get("paddingLeft").optint(padding[0]),
                get("paddingTop").optint(padding[1]),
                get("paddingRight").optint(padding[2]),
                get("paddingBottom").optint(padding[3])
            )

            backgroundColor = get("backgroundColor").optint(backgroundColor)
            autoFocus = get("autoFocus").optboolean(autoFocus)
            allowTouchMove = get("allowTouchMove").optboolean(allowTouchMove)
            noLimits = get("noLimits").optboolean(noLimits)
            noBackground = get("noBackground").optboolean(noBackground)
            sharedPreferences = get("sharedPreferences").optuserdata(sharedPreferences) as SharedPreferences

            positionX = get("positionX").optint(positionX)
            positionY = get("positionY").optint(positionY)
            windowGravity = get("gravity").optint(windowGravity)

            arrayOf("onMove", "onFocusChanged", "onClose").forEach { key ->
                config.get(key).takeIf { !it.isnil() }?.let { builder.set(key, it) }
            }
        }
    }

    fun create(init: LuaValue) {
        if (mLayout != null) return

        mLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding[0].dp, padding[1].dp, padding[2].dp, padding[3].dp)
            if (!noBackground) background = createBackgroundDrawable()
        }

        builder.set("layout", CoerceJavaToLua.coerce(mLayout))

        val lp = createLayoutParams()

        mLayout?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            @SuppressLint("ClickableViewAccessibility", "RtlHardcoded")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = lp.x
                        initialY = lp.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (allowTouchMove) {
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()

                            when {
                                lp.gravity and Gravity.RIGHT == Gravity.RIGHT -> {
                                    lp.x = initialX - deltaX
                                    lp.y = initialY + deltaY
                                }

                                lp.gravity and Gravity.BOTTOM == Gravity.BOTTOM -> {
                                    lp.x = initialX + deltaX
                                    lp.y = initialY - deltaY
                                }

                                else -> {
                                    lp.x = initialX + deltaX
                                    lp.y = initialY + deltaY
                                }
                            }

                            mWindowManager.updateViewLayout(mLayout, lp)
                        }

                        builder.get("onMove").takeIf { !it.isnil() }?.call(valueOf(lp.x), valueOf(lp.y))
                        return true
                    }

                    MotionEvent.ACTION_OUTSIDE -> {
                        if (autoFocus) {
                            mLayout?.let { view ->
                                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                mWindowManager.updateViewLayout(view, lp)
                                builder.get("onFocusChanged").takeIf { !it.isnil() }?.call(valueOf(false))
                            }
                        }

                        return true
                    }
                }
                return false
            }
        })

        val preferencesList = init.call(CoerceJavaToLua.coerce(builder)).checktable()

        preferencesList.forEach { _, value ->
            addPreferenceToLayout(value, lp)
        }

        mWindowManager.addView(mLayout, lp)
    }

    private fun addPreferenceToLayout(value: LuaValue, lp: WindowManager.LayoutParams) {
        val item = castPreference(context, value)

        val view = item.getView(sharedPreferences) {
            if (autoFocus) {
                lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                mWindowManager.updateViewLayout(mLayout, lp)
                builder.get("onFocusChanged").takeIf { !it.isnil() }?.call(valueOf(true))
            }
        }
            .apply {
                if (parent != null) (parent as ViewGroup).removeView(this)
            }

        mLayout?.addView(view)
    }

    fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (noLimits) flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            x = positionX
            y = positionY
            gravity = windowGravity
        }
    }

    private fun createBackgroundDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(backgroundColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cornerRadius = this@FloatingWindow.cornerRadius.dp.toFloat()
            }
        }
    }

    fun isNotFocused(): Boolean {
        val flags = (mLayout?.layoutParams as? WindowManager.LayoutParams)?.flags
        return flags?.and(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0
    }

    fun close() {
        mLayout?.let { layout ->
            onClose?.invoke()
            builder.get("onClose").takeIf { !it.isnil() }?.call()
            builder.set("layout", LuaValue.NIL)
            mWindowManager.removeView(layout)
            mLayout = null
        }
    }
}