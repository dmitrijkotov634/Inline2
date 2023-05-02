package com.wavecat.inline;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wavecat.inline.databinding.PreferencesDialogBinding;
import com.wavecat.inline.preferences.Button;
import com.wavecat.inline.preferences.CheckBox;
import com.wavecat.inline.preferences.Column;
import com.wavecat.inline.preferences.PreferencesItem;
import com.wavecat.inline.preferences.Row;
import com.wavecat.inline.preferences.SeekBar;
import com.wavecat.inline.preferences.Spinner;
import com.wavecat.inline.preferences.Text;
import com.wavecat.inline.preferences.TextInput;
import com.wavecat.inline.preferences.Utils;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.LinkedHashSet;

public class PreferencesDialog {

    private final Context context;
    private final LayoutInflater inflater;

    private Dialog instance;

    public LuaTable text;
    public LuaTable checkBox;
    public LuaTable textInput;
    public LuaTable spinner;
    public LuaTable button;
    public LuaTable row;
    public LuaTable column;
    public LuaTable seekBar;

    @SuppressWarnings("unused")
    public LuaFunction showPreferences = new VarArgFunction() {
        @Override
        public Varargs invoke(Varargs args) {
            LinkedHashSet<PreferencesItem> preferences = new LinkedHashSet<>();

            SharedPreferences sharedPreferences = InlineService.getInstance()
                    .getDefaultSharedPreferences();

            LuaValue k = LuaValue.NIL;
            for (int n = 3; n < args.narg() + 1; n++) {
                LuaValue value = args.arg(n);

                if (value.isuserdata(SharedPreferences.class)) {
                    sharedPreferences = (SharedPreferences) value.touserdata(SharedPreferences.class);
                    continue;
                }

                preferences.add(new PreferencesItem(sharedPreferences, value));
            }

            showPreferences(args.checkjstring(2), preferences);
            return NIL;
        }
    };

    public PreferencesDialog(Context context, LayoutInflater inflater) {
        this.context = context;
        this.inflater = inflater;

        text = Utils.getCallable(context, Text.class);
        checkBox = Utils.getCallable(context, CheckBox.class);
        textInput = Utils.getCallable(context, TextInput.class);
        spinner = Utils.getCallable(context, Spinner.class);
        button = Utils.getCallable(context, Button.class);
        row = Utils.getCallable(context, Row.class);
        column = Utils.getCallable(context, Column.class);
        seekBar = Utils.getCallable(context, SeekBar.class);
    }

    @SuppressWarnings("unused")
    public void showPreferences(String title, LinkedHashSet<PreferencesItem> preferences) {
        PreferencesDialogBinding binding = PreferencesDialogBinding.inflate(inflater);

        for (PreferencesItem preference : preferences) {
            LuaTable preferencesList = preference.getBuilder()
                    .call(CoerceJavaToLua.coerce(this)).checktable();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = preferencesList.next(k);

                if ((k = n.arg1()).isnil())
                    break;

                LuaValue value = n.arg(2);

                View view = Utils.castPreference(context, value)
                        .getView(preference.getSharedPreferences());

                if (view.getParent() != null)
                    ((ViewGroup) view.getParent()).removeView(view);

                view.setPadding(0, 16, 0, 16);

                binding.preferences.addView(view);
            }
        }

        instance = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(binding.getRoot())
                .show();
    }

    @SuppressWarnings("unused")
    public void cancel() {
        if (instance != null)
            instance.cancel();
    }
}
