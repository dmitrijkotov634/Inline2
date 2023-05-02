package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@SuppressLint("ViewConstructor")
public class TextInput extends TextInputLayout implements Preference {

    private String sharedKey;

    private LuaValue listener;

    private TextWatcher textWatcher;
    private String defaultValue;

    @SuppressWarnings("unused")
    public TextInput(Context context, String sharedKey, String hint) {
        this(context, hint);
        this.sharedKey = sharedKey;
    }

    @SuppressWarnings("unused")
    public TextInput(Context context, String hint, LuaValue listener) {
        this(context, hint);
        this.listener = listener;
    }

    @SuppressWarnings("unused")
    public TextInput(Context context, String hint) {
        this(context);
        setHint(hint);
    }

    public TextInput(Context context) {
        super(context);

        TextInputEditText editText = new TextInputEditText(getContext());
        addView(editText);
    }

    @SuppressWarnings("unused")
    public TextInput setListener(LuaValue listener) {
        this.listener = listener;

        return this;
    }

    @SuppressWarnings("unused")
    public TextInput setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;

        return this;
    }

    @SuppressWarnings("unused")
    public TextInput setDefault(String defaultValue) {
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

    public String getText() {
        if (getEditText() == null) return null;
        return getEditText().getText().toString();
    }

    public void setText(String text) {
        if (getEditText() == null) return;
        getEditText().setText(text);
    }

    @Override
    public View getView(SharedPreferences preferences) {

        if (getEditText() != null) {
            if (textWatcher != null)
                getEditText().removeTextChangedListener(textWatcher);

            if (sharedKey != null && preferences != null)
                getEditText().setText(preferences.getString(sharedKey, defaultValue));
        }

        textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (sharedKey != null && preferences != null)
                    preferences
                            .edit()
                            .putString(sharedKey, s.toString())
                            .apply();

                if (listener != null)
                    listener.call(LuaValue.valueOf(s.toString()), CoerceJavaToLua.coerce(TextInput.this));
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                // nothing
            }
        };

        if (getEditText() != null)
            getEditText().addTextChangedListener(textWatcher);

        return this;
    }
}