@file:Suppress("unused")

package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.widget.AppCompatSeekBar
import com.wavecat.inline.preferences.Preference
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.CoerceJavaToLua

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

    override fun getView(preferences: SharedPreferences?): View {
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
                    preferences?.edit()
                        ?.putInt(it, seekBar.progress)
                        ?.apply()
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
