package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.appcompat.widget.AppCompatSeekBar;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@SuppressLint("ViewConstructor")
public class SeekBar extends AppCompatSeekBar implements Preference {

    private String sharedKey;

    private LuaValue onStopTracking;
    private LuaValue onProgressChanged;

    private int defaultValue;

    @SuppressWarnings("unused")
    public SeekBar(Context context, String sharedKey, int max) {
        super(context);

        this.sharedKey = sharedKey;

        setMax(max);
    }

    @SuppressWarnings("unused")
    public SeekBar(Context context, LuaValue onStopTracking, int max) {
        super(context);

        this.onStopTracking = onStopTracking;

        setMax(max);
    }

    @SuppressWarnings("unused")
    public SeekBar setOnStopTracking(LuaValue onStopTracking) {
        this.onStopTracking = onStopTracking;

        return this;
    }

    @SuppressWarnings("unused")
    public SeekBar setOnProgressChanged(LuaValue onProgressChanged) {
        this.onProgressChanged = onProgressChanged;

        return this;
    }

    @SuppressWarnings("unused")
    public SeekBar setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;

        return this;
    }

    @SuppressWarnings("unused")
    public SeekBar setDefault(int defaultValue) {
        this.defaultValue = defaultValue;

        return this;
    }

    @SuppressWarnings("unused")
    public LuaValue getOnStopTracking() {
        return onStopTracking;
    }

    @SuppressWarnings("unused")
    public LuaValue getOnProgressChanged() {
        return onProgressChanged;
    }

    @SuppressWarnings("unused")
    public String getSharedKey() {
        return sharedKey;
    }

    @Override
    public View getView(SharedPreferences preferences) {
        setOnSeekBarChangeListener(null);

        if (sharedKey != null && preferences != null)
            setProgress(preferences.getInt(sharedKey, defaultValue));

        setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (onProgressChanged != null)
                    onProgressChanged.call(LuaValue.valueOf(progress), CoerceJavaToLua.coerce(SeekBar.this));
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                // nothing
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                if (sharedKey != null && preferences != null)
                    preferences
                            .edit()
                            .putInt(sharedKey, seekBar.getProgress())
                            .apply();

                if (onStopTracking != null)
                    onStopTracking.call(LuaValue.valueOf(seekBar.getProgress()), CoerceJavaToLua.coerce(SeekBar.this));
            }
        });

        return this;
    }
}
