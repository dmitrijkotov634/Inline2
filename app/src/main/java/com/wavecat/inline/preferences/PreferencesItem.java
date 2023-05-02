package com.wavecat.inline.preferences;

import android.content.SharedPreferences;

import org.luaj.vm2.LuaValue;

public class PreferencesItem {

    private final SharedPreferences sharedPreferences;
    private final LuaValue builder;

    public PreferencesItem(SharedPreferences sharedPreferences, LuaValue builder) {
        this.sharedPreferences = sharedPreferences;
        this.builder = builder;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public LuaValue getBuilder() {
        return builder;
    }
}
