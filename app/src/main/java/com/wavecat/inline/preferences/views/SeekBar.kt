@file:Suppress("unused")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.edit
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * Custom seek bar implementation for preferences with Lua scripting support.
 *
 * Extends AppCompatSeekBar to provide integration with SharedPreferences
 * for persistent storage and Lua callback functionality for progress tracking.
 * Supports both preference-backed and listener-based seek bar configurations
 * with separate callbacks for progress changes and tracking completion.
 *
 * @see AppCompatSeekBar
 * @see Preference
 * @see SharedPreferences
 * @see OnSeekBarChangeListener
 */
@SuppressLint("ViewConstructor")
class SeekBar : AppCompatSeekBar, Preference {
    var sharedKey: String? = null
        private set

    var onStopTracking: LuaValue? = null
        private set

    var onProgressChanged: LuaValue? = null
        private set

    private var defaultValue = 0

    constructor(context: Context, sharedKey: String?, max: Int) : super(context) {
        this.sharedKey = sharedKey
        setMax(max)
    }

    constructor(context: Context, onStopTracking: LuaValue?, max: Int) : super(context) {
        this.onStopTracking = onStopTracking
        setMax(max)
    }

    fun setOnStopTracking(onStopTracking: LuaValue?): SeekBar {
        this.onStopTracking = onStopTracking
        return this
    }

    fun setOnProgressChanged(onProgressChanged: LuaValue?): SeekBar {
        this.onProgressChanged = onProgressChanged
        return this
    }

    fun setSharedKey(sharedKey: String?): SeekBar {
        this.sharedKey = sharedKey
        return this
    }

    fun setDefault(defaultValue: Int): SeekBar {
        this.defaultValue = defaultValue
        return this
    }

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View {
        setOnSeekBarChangeListener(null)

        sharedKey?.let {
            if (preferences != null)
                progress = preferences.getInt(it, defaultValue)
        }

        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: android.widget.SeekBar,
                progress: Int,
                fromUser: Boolean,
            ) {
                onProgressChanged?.call(
                    LuaValue.valueOf(progress),
                    CoerceJavaToLua.coerce(this@SeekBar)
                )
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar) {}

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar) {
                sharedKey?.let {
                    preferences?.edit {
                        putInt(it, seekBar.progress)
                    }
                }

                onStopTracking?.call(
                    LuaValue.valueOf(seekBar.progress),
                    CoerceJavaToLua.coerce(this@SeekBar)
                )
            }
        })

        return this
    }
}
