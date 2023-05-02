package com.wavecat.inline.preferences;

import android.content.Context;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class Utils {
    public static LuaTable getCallable(Context context, Class<?> klass) {
        LuaTable metatable = new LuaTable();
        metatable.set(LuaValue.CALL, new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return CoerceJavaToLua.coerce(klass)
                        .get("new")
                        .invoke(varargsOf(CoerceJavaToLua.coerce(context), args.subargs(2)));
            }
        });
        LuaTable table = new LuaTable();
        table.setmetatable(metatable);
        return table;
    }

    public static Preference castPreference(Context context, LuaValue value) {
        if (value instanceof LuaUserdata)
            return (Preference) value.checkuserdata(Preference.class);

        if (value instanceof LuaString)
            return new Text(context, value.tojstring());

        throw new IllegalArgumentException();
    }
}
