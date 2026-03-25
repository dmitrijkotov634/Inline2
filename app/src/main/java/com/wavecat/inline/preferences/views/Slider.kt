@file:Suppress("unused")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.core.content.edit
import com.google.android.material.slider.Slider
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

@SuppressLint("ViewConstructor")
class Slider(context: Context) : Slider(context), Preference {

    var sharedKey: String? = null
        private set

    var onStopTracking: LuaValue? = null
        private set

    var onProgressChanged: LuaValue? = null
        private set

    private var defaultValue = 0f
    private var useInt: Boolean = false

    constructor(context: Context, sharedKey: String?, max: Float) : this(context) {
        this.sharedKey = sharedKey
        setup(0f, max)
    }

    constructor(context: Context, onStopTracking: LuaValue?, max: Float) : this(context) {
        this.onStopTracking = onStopTracking
        setup(0f, max)
    }

    constructor(context: Context, sharedKey: String?, min: Float, max: Float) : this(context) {
        this.sharedKey = sharedKey
        setup(min, max)
    }

    constructor(
        context: Context,
        onStopTracking: LuaValue?,
        min: Float,
        max: Float
    ) : this(context) {
        this.onStopTracking = onStopTracking
        setup(min, max)
    }

    constructor(context: Context, sharedKey: String?, min: Float, max: Float, step: Float) : this(
        context
    ) {
        this.sharedKey = sharedKey
        setup(min, max, step)
    }

    constructor(
        context: Context,
        onStopTracking: LuaValue?,
        min: Float,
        max: Float,
        step: Float
    ) : this(context) {
        this.onStopTracking = onStopTracking
        setup(min, max, step)
    }

    private fun setup(min: Float, max: Float, step: Float = 1f) {
        valueFrom = min
        valueTo = max
        stepSize = step
        value = min
    }

    fun setStep(step: Int): com.wavecat.inline.preferences.views.Slider {
        stepSize = step.toFloat()
        return this
    }

    fun setOnStopTracking(onStopTracking: LuaValue?): com.wavecat.inline.preferences.views.Slider {
        this.onStopTracking = onStopTracking
        return this
    }

    fun setOnProgressChanged(onProgressChanged: LuaValue?): com.wavecat.inline.preferences.views.Slider {
        this.onProgressChanged = onProgressChanged
        return this
    }

    fun setProgress(progress: Float) {
        value = progress
    }

    fun setProgress(progress: Int) {
        value = progress.toFloat()
    }

    fun setSharedKey(sharedKey: String?): com.wavecat.inline.preferences.views.Slider {
        this.sharedKey = sharedKey
        return this
    }

    fun setDefault(defaultValue: Int): com.wavecat.inline.preferences.views.Slider {
        this.defaultValue = defaultValue.toFloat()
        return this
    }

    fun setDefault(defaultValue: Float): com.wavecat.inline.preferences.views.Slider {
        this.defaultValue = defaultValue
        return this
    }

    fun useInt(): com.wavecat.inline.preferences.views.Slider {
        useInt = true
        return this
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        clearOnChangeListeners()
        clearOnSliderTouchListeners()

        sharedKey?.let {
            if (preferences != null) {
                value = if (useInt) {
                    preferences.getInt(it, defaultValue.toInt()).toFloat()
                } else {
                    preferences.getFloat(it, defaultValue)
                }.coerceIn(valueFrom, valueTo)
            }
        }

        addOnChangeListener { _, newValue, _ ->
            onProgressChanged?.call(
                if (useInt) LuaValue.valueOf(newValue.toInt()) else LuaValue.valueOf(newValue.toDouble()),
                CoerceJavaToLua.coerce(this@Slider)
            )
        }

        addOnSliderTouchListener(object : OnSliderTouchListener {
            override fun onStartTrackingTouch(s: Slider) {}

            override fun onStopTrackingTouch(s: Slider) {
                sharedKey?.let {
                    preferences?.edit {
                        if (useInt) {
                            putInt(it, s.value.toInt())
                        } else {
                            putFloat(it, s.value)
                        }
                    }
                }

                onStopTracking?.call(
                    if (useInt) LuaValue.valueOf(s.value.toInt()) else LuaValue.valueOf(s.value.toDouble()),
                    CoerceJavaToLua.coerce(this@Slider)
                )
            }
        })

        return this
    }
}
