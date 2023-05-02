package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.material.checkbox.MaterialCheckBox;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@SuppressLint("ViewConstructor")
public class CheckBox extends MaterialCheckBox implements Preference {

    private String sharedKey;

    private LuaValue listener;

    private boolean defaultValue;

    @SuppressWarnings("unused")
    public CheckBox(Context context, String sharedKey, String text) {
        super(context);
        this.sharedKey = sharedKey;

        setText(text);
    }

    @SuppressWarnings("unused")
    public CheckBox(Context context, String text, LuaValue listener) {
        super(context);
        this.listener = listener;

        setText(text);
    }

    @SuppressWarnings("unused")
    public CheckBox(Context context, String text) {
        super(context);

        setText(text);
    }

    @SuppressWarnings("unused")
    public CheckBox setListener(LuaValue listener) {
        this.listener = listener;

        return this;
    }

    @SuppressWarnings("unused")
    public CheckBox setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;

        return this;
    }

    @SuppressWarnings("unused")
    public CheckBox setDefault(boolean defaultValue) {
        this.defaultValue = defaultValue;

        return this;
    }

    @SuppressWarnings("unused")
    public LuaValue getListener() {
        return listener;
    }

    @SuppressWarnings("unused")
    public String getSharedKey() {
        return sharedKey;
    }

    @Override
    public View getView(SharedPreferences preferences) {
        setOnCheckedChangeListener(null);

        if (sharedKey != null && preferences != null)
            setChecked(preferences.getBoolean(sharedKey, defaultValue));

        setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (sharedKey != null && preferences != null)
                preferences
                        .edit()
                        .putBoolean(sharedKey, isChecked)
                        .apply();

            if (listener != null)
                listener.call(LuaValue.valueOf(isChecked), CoerceJavaToLua.coerce(CheckBox.this));
        });

        return this;
    }
}
