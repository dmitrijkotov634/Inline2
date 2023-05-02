package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

@SuppressLint("ViewConstructor")
public class LinearGroup extends LinearLayout implements Preference {

    private final LuaTable views;

    public LinearGroup(Context context, LuaTable views) {
        super(context);

        this.views = views;
    }

    @Override
    public View getView(SharedPreferences preferences) {
        removeAllViews();

        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs n = views.next(k);

            if ((k = n.arg1()).isnil())
                break;

            addView(Utils.castPreference(getContext(), n.arg(2)).getView(preferences));
        }

        return this;
    }
}
