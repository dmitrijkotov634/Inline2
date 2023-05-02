package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.google.android.material.button.MaterialButton;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@SuppressLint("ViewConstructor")
public class Button extends MaterialButton implements Preference {

    @SuppressWarnings("unused")
    public Button(Context context, String text, LuaValue listener) {
        super(context);
        setText(text);
        setOnClickListener(v -> listener.call(CoerceJavaToLua.coerce(this)));
    }

    @SuppressWarnings("unused")
    public Button(Context context, String text) {
        super(context);
        setText(text);
    }

    @Override
    public View getView(SharedPreferences preferences) {
        return this;
    }
}
