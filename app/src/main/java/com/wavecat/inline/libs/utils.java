package com.wavecat.inline.libs;

import com.wavecat.inline.utils.ArgumentTokenizer;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@SuppressWarnings("unused")
public class utils extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue library = tableOf();

        library.set("split", new split());
        library.set("escape", new escape());
        library.set("parseArgs", new parseArgs());

        env.set("utils", library);
        env.get("package").get("loaded").set("utils", library);

        return library;
    }

    static class split extends ThreeArgFunction {
        public LuaValue call(LuaValue string, LuaValue regex, LuaValue limit) {
            if (limit.isnil())
                return CoerceJavaToLua.coerce(string.checkjstring().split(regex.checkjstring()));
            else
                return CoerceJavaToLua.coerce(string.checkjstring().split(regex.checkjstring(), limit.checkint()));
        }
    }

    static class escape extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue string) {
            return valueOf(string.checkjstring().replaceAll("[$*+?.()\\[\\]%-]", "%$0"));
        }
    }

    static class parseArgs extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue string) {
            return CoerceJavaToLua.coerce(ArgumentTokenizer.tokenize(string.checkjstring()).toArray());
        }
    }
}
