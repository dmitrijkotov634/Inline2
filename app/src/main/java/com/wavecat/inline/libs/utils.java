package com.wavecat.inline.libs;

import com.wavecat.inline.Query;
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

        library.set("split", new Split());
        library.set("escape", new Escape());
        library.set("parseArgs", new ParseArgs());
        library.set("command", new Command());
        library.set("hasArgs", new HasArgs());

        env.set("utils", library);
        env.get("package").get("loaded").set("utils", library);

        return library;
    }

    static class Split extends ThreeArgFunction {
        public LuaValue call(LuaValue string, LuaValue regex, LuaValue limit) {
            if (limit.isnil())
                return CoerceJavaToLua.coerce(string.checkjstring().split(regex.checkjstring()));
            else
                return CoerceJavaToLua.coerce(string.checkjstring().split(regex.checkjstring(), limit.checkint()));
        }
    }

    static class Escape extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue string) {
            return valueOf(string.checkjstring().replaceAll("[().%+\\-\\*\\?\\[^$\\]]", "%$0"));
        }
    }

    static class ParseArgs extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue string) {
            return CoerceJavaToLua.coerce(ArgumentTokenizer.tokenize(string.checkjstring()).toArray());
        }
    }

    static class Command extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue value, LuaValue count, LuaValue errorValue) {
            value.checkfunction();
            count.checkint();
            return new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue input, LuaValue arg2) {
                    Query query = ((Query) arg2.checkuserdata(Query.class));

                    Object[] args = ArgumentTokenizer.tokenize(query.getArgs()).toArray();

                    if (args.length == count.toint()) {
                        return value.call(input, arg2, CoerceJavaToLua.coerce(args));
                    } else {
                        if (errorValue.isfunction()) {
                            return errorValue.call(input, arg2);
                        } else {
                            query.answer("Wrong arguments");
                            return NIL;
                        }
                    }
                }
            };
        }
    }

    static class HasArgs extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue value, LuaValue errorValue) {
            value.checkfunction();
            return new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue input, LuaValue arg2) {
                    Query query = ((Query) arg2.checkuserdata(Query.class));

                    if (query.getArgs().isEmpty()) {
                        if (errorValue.isfunction()) {
                            return errorValue.call(input, arg2);
                        } else {
                            query.answer("Empty argument");
                            return NIL;
                        }
                    } else {
                        return value.call(input, arg2);
                    }
                }
            };
        }
    }

}
