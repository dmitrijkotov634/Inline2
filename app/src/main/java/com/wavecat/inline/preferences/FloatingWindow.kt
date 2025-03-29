@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.wavecat.inline.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.zeroArgFunction
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.utils.dp
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.valueOf
import org.luaj.vm2.lib.jse.CoerceJavaToLua


@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class FloatingWindow(private val context: Context) {

    private var sharedPreferences = requireService().defaultSharedPreferences
    private var cornerRadius = 16
    private var padding = intArrayOf(16, 16, 16, 16)
    private var backgroundColor: Int = Color.WHITE

    init {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
            backgroundColor = typedValue.data
        }
    }

    private var autoFocus = true
    private var noLimits = false
    private var transparent = false

    private val mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var mLayout: LinearLayout? = null

    private val builder = Builder(context = context).apply {
        set("windowManager", CoerceJavaToLua.coerce(mWindowManager))
        set("isFocused", zeroArgFunction { valueOf(!isNotFocused()) })
        set("close", zeroArgFunction {
            close()
            LuaValue.NIL
        })
    }

    fun configure(config: LuaValue) {
        with(config) {
            cornerRadius = get("cornerRadius").optint(cornerRadius)
            padding[0] = get("paddingLeft").optint(padding[0])
            padding[1] = get("paddingTop").optint(padding[1])
            padding[2] = get("paddingRight").optint(padding[2])
            padding[3] = get("paddingBottom").optint(padding[3])
            backgroundColor = get("backgroundColor").optint(backgroundColor)
            autoFocus = get("autoFocus").optboolean(autoFocus)
            noLimits = get("noLimits").optboolean(noLimits)
            transparent = get("transparent").optboolean(transparent)
            sharedPreferences = get("sharedPreferences").optuserdata(sharedPreferences) as SharedPreferences
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun create(init: LuaValue) {
        if (mLayout != null) return

        mLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding[0].dp, padding[1].dp, padding[2].dp, padding[3].dp)
            if (!transparent) background = createBackgroundDrawable()
        }

        builder.set("layout", CoerceJavaToLua.coerce(mLayout))

        val lp = createLayoutParams()

        mLayout?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = lp.x
                        initialY = lp.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY

                        if (autoFocus) {
                            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                            mWindowManager.updateViewLayout(mLayout, lp)
                        }

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        lp.x = initialX + (event.rawX - initialTouchX).toInt()
                        lp.y = initialY + (event.rawY - initialTouchY).toInt()
                        mWindowManager.updateViewLayout(mLayout, lp)
                        return true
                    }

                    MotionEvent.ACTION_OUTSIDE -> {
                        if (autoFocus) {
                            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            mWindowManager.updateViewLayout(mLayout, lp)
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

        if (autoFocus) {
            item.setWindowFocusListener {
                lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                mWindowManager.updateViewLayout(mLayout, lp)
            }
        }

        val view = item.getView(sharedPreferences).apply {
            if (parent != null) (parent as ViewGroup).removeView(this)
        }

        mLayout?.addView(view)
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
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
        mLayout?.let {
            builder.set("layout", LuaValue.NIL)
            mWindowManager.removeView(it)
            mLayout = null
        }
    }
}