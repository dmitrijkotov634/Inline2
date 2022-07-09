package com.wavecat.inline.libs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
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

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue library = tableOf();

        library.set("dump", new Dump());
        library.set("dumpObject", new DumpObject());
        library.set("load", new Load());
        library.set("loadObject", new LoadObject());
        library.set("emptyArray", new LuaUserdata(new EmptyArray()));
        library.set("emptyObject", new LuaUserdata(new EmptyObject()));

        env.set("json", library);
        env.get("package").get("loaded").set("json", library);

        return library;
    }

    static class EmptyObject {
    }

    static class EmptyArray {
    }

    private static Object castValue(LuaValue value, Set<LuaValue> stack) throws JSONException {
        if (value.isuserdata(EmptyArray.class)) {
            return new JSONArray();
        }
        if (value.isuserdata(EmptyObject.class)) {
            return new JSONObject();
        }
        switch (value.type()) {
            case TBOOLEAN:
                return value.toboolean();
            case TNUMBER:
                return value.tolong();
            case TSTRING:
                return value.tostring();
            case TNIL:
                return null;
            case TTABLE:
                return dumpTable(value, stack);
            default:
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
            return new JSONObject();
        }
        if (first.isnumber()) {
            JSONArray jsonArray = new JSONArray();
            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = value.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                LuaValue v = n.arg(2);
                jsonArray.put(castValue(v, stack));
            }
            return jsonArray;
        } else {
            JSONObject jsonObject = new JSONObject();
            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = value.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                LuaValue v = n.arg(2);
                jsonObject.put(k.checkjstring(), castValue(v, stack));
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
