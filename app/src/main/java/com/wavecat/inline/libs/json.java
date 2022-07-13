package com.wavecat.inline.libs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("unused")
public class json extends TwoArgFunction {

    private static final LuaValue Null = new LuaUserdata(new Object());

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue library = tableOf();

        library.set("dump", new Dump());
        library.set("dumpObject", new DumpObject());
        library.set("load", new Load());
        library.set("loadObject", new LoadObject());
        library.set("emptyObject", new LuaUserdata(new JSONObject()));
        library.set("null", Null);

        env.set("json", library);
        env.get("package").get("loaded").set("json", library);

        return library;
    }

    private static Object castValue(LuaValue value, Set<LuaValue> stack) throws JSONException {
        if (value.isuserdata(JSONObject.class) || value.isuserdata(JSONArray.class))
            return value.touserdata(Object.class);
        else if (value.equals(Null))
            return JSONObject.NULL;
        else if (value instanceof LuaBoolean)
            return value.toboolean();
        else if (value instanceof LuaInteger)
            return value.toint();
        else if (value instanceof LuaString)
            return value.tojstring();
        else if (value instanceof LuaTable)
            return dumpTable(value, stack);
        else {
            error("unable to serialize " + value.typename());
            return null;
        }
    }

    private static Object dumpTable(LuaValue value, Set<LuaValue> stack) throws JSONException {
        if (stack.contains(value))
            error("circular reference");
        stack.add(value);
        LuaValue first = LuaValue.NIL;
        if ((first = value.next(first).arg1()).isnil()) {
            return new JSONArray();
        }
        if (first.isnumber()) {
            JSONArray jsonArray = new JSONArray();
            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = value.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                jsonArray.put(castValue(n.arg(2), stack));
            }
            return jsonArray;
        } else {
            JSONObject jsonObject = new JSONObject();
            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = value.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                jsonObject.put(k.checkjstring(), castValue(n.arg(2), stack));
            }
            return jsonObject;
        }
    }

    static class DumpObject extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue table) {
            try {
                return CoerceJavaToLua.coerce(dumpTable(table.checktable(), new HashSet<>()));
            } catch (JSONException e) {
                error(e.getMessage());
            }
            return null;
        }
    }

    static class Dump extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue table) {
            try {
                return valueOf(dumpTable(table.checktable(), new HashSet<>()).toString());
            } catch (JSONException e) {
                error(e.getMessage());
            }
            return null;
        }
    }

    private static LuaValue load(Object object) throws JSONException {
        LuaValue value;
        if (object instanceof JSONObject) {
            value = new LuaTable();
            JSONObject jsonObject = (JSONObject) object;
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String key = it.next();
                value.set(key, load(jsonObject.get(key)));
            }
        } else if (object instanceof JSONArray) {
            value = new LuaTable();
            JSONArray jsonArray = (JSONArray) object;
            for (int i = 0; i < jsonArray.length(); i++) {
                value.set(i + 1, load(jsonArray.get(i)));
            }
        } else {
            value = CoerceJavaToLua.coerce(object);
        }
        return value;
    }

    static class LoadObject extends OneArgFunction {
        public LuaValue call(LuaValue jsonObject) {
            try {
                return json.load(jsonObject.touserdata());
            } catch (JSONException e) {
                error(e.getMessage());
            }
            return null;
        }
    }

    static class Load extends OneArgFunction {
        public LuaValue call(LuaValue jsonString) {
            try {
                return json.load(new JSONTokener(jsonString.checkjstring()).nextValue());
            } catch (JSONException e) {
                error(e.getMessage());
            }
            return null;
        }
    }
}
