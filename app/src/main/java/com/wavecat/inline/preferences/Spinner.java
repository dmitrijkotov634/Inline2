package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatSpinner;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("ViewConstructor")
public class Spinner extends AppCompatSpinner implements Preference {

    private String sharedKey;

    private LuaValue listener;

    private final List<String> choices;
    private final ArrayAdapter<String> adapter;

    private boolean userSelect = true;

    @SuppressWarnings("unused")
    public Spinner(Context context, String sharedKey, LuaTable choices) {
        this(context, choices);
        this.sharedKey = sharedKey;
    }

    @SuppressWarnings("unused")
    public Spinner(Context context, LuaTable choices, LuaValue listener) {
        this(context, choices);
        this.listener = listener;
    }

    public Spinner(Context context, LuaTable set) {
        super(context);

        choices = new ArrayList<>();

        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs n = set.next(k);

            if ((k = n.arg1()).isnil())
                break;

            choices.add(n.arg(2).tojstring());
        }

        adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(adapter);
    }

    @SuppressWarnings("unused")
    public Spinner setListener(LuaValue listener) {
        this.listener = listener;

        return this;
    }

    @SuppressWarnings("unused")
    public Spinner setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;

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
    public ArrayAdapter<String> getAdapter() {
        return adapter;
    }

    @Override
    public View getView(SharedPreferences preferences) {
        if (sharedKey != null && preferences != null) {
            userSelect = false;
            setSelection(choices.indexOf(preferences.getString(sharedKey, "")));
        }

        setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!userSelect) {
                    userSelect = true;
                    return;
                }

                if (sharedKey != null && preferences != null)
                    preferences
                            .edit()
                            .putString(sharedKey, adapter.getItem(position))
                            .apply();

                if (listener != null)
                    listener.call(LuaValue.valueOf(adapter.getItem(position)), CoerceJavaToLua.coerce(Spinner.this));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing
            }
        });

        return this;
    }
}
