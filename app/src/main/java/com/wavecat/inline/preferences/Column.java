package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;

import org.luaj.vm2.LuaTable;

@SuppressLint("ViewConstructor")
public class Column extends LinearGroup {

    public Column(Context context, LuaTable views) {
        super(context, views);
        setOrientation(VERTICAL);
    }
}
