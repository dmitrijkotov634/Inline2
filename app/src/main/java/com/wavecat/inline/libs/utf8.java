package com.wavecat.inline.libs;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

@SuppressWarnings("unused")
public class utf8 extends TwoArgFunction {

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue library = tableOf();

        library.set("len", new len());
        library.set("sub", new len());
        library.set("charpattern", "[\\0-\\x7F\\xC2-\\xF4][\\x80-\\xBF]*");


        env.set("utf8", library);
        env.get("package").get("loaded").set("utf8", library);

        return library;
    }

    static class sub extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue string, LuaValue start, LuaValue end) {
            return valueOf(string.checkjstring().substring(start.checkint(), end.checkint()));
        }
    }

    static class len extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue string) {
            return valueOf(string.checkjstring().length());
        }
    }
}
