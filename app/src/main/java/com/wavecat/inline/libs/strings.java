package com.wavecat.inline.libs;

import com.wavecat.inline.ArgumentTokenizer;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@SuppressWarnings("unused")
public class strings extends TwoArgFunction {

    private static class StringsLib {
        public static String[] split(String string, String regex) {
            return string.split(regex);
        }

        public static String[] split(String string, String regex, int limit) {
            return string.split(regex, limit);
        }

        public static int length(String string) {
            return string.length();
        }

        public static String escape(String string) {
            return string.replaceAll("[$*+?.()\\[\\]%-]", "%$0");
        }

        public static Object[] parseArgs(String string) {
            return ArgumentTokenizer.tokenize(string).toArray();
        }
    }

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue lib = CoerceJavaToLua.coerce(StringsLib.class);

        env.set("strings", lib);
        env.get("package").get("loaded").set("strings", lib);

        return lib;
    }
}
