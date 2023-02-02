package com.wavecat.inline.libs;

import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.wavecat.inline.InlineService;
import com.wavecat.inline.Query;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public class menu extends TwoArgFunction {

    private final HashMap<AccessibilityNodeInfo, Context> current = new HashMap<>();

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue library = tableOf();

        library.set("create", new Create());
        library.set("current", CoerceJavaToLua.coerce(current));

        env.set("menu", library);
        env.get("package").get("loaded").set("menu", library);

        InlineService context = InlineService.getInstance();

        if (context == null)
            error("service is not active");

        context.getAllWatchers().put(new MenuWatcher(), InlineService.TYPE_SELECTION_CHANGED);

        return library;
    }

    class MenuWatcher extends OneArgFunction {
        public LuaValue call(LuaValue arg) {
            AccessibilityNodeInfo accessibilityNodeInfo = (AccessibilityNodeInfo) arg.checkuserdata(AccessibilityNodeInfo.class);

            Context context = current.get(accessibilityNodeInfo);

            if (context == null)
                return NIL;

            if (accessibilityNodeInfo.getText() == null || accessibilityNodeInfo.getText().length() != context.length()) {
                current.remove(accessibilityNodeInfo);

                if (context.getCancelAction().isnil())
                    context.getQuery().answer(null);
                else
                    context.getCancelAction().call(arg, CoerceJavaToLua.coerce(context.getQuery()));

                return NIL;
            }

            for (Part point : Objects.requireNonNull(context.getParts())) {
                if (accessibilityNodeInfo.getTextSelectionStart() > point.getStart() &&
                        accessibilityNodeInfo.getTextSelectionStart() < point.getEnd() &&
                        accessibilityNodeInfo.getTextSelectionEnd() > point.getStart() &&
                        accessibilityNodeInfo.getTextSelectionEnd() < point.getEnd()) {
                    current.remove(accessibilityNodeInfo);
                    point.getAction().call(arg, CoerceJavaToLua.coerce(context.getQuery()));
                    break;
                }
            }

            return NIL;
        }
    }

    static class Context {

        private final Query query;
        private final Set<Part> parts;
        private final LuaValue cancelAction;

        private final int length;

        public Context(Query query, Set<Part> parts, LuaValue cancelAction, int length) {
            this.query = query;
            this.parts = parts;
            this.cancelAction = cancelAction;
            this.length = length;
        }

        public Query getQuery() {
            return query;
        }

        public Set<Part> getParts() {
            return parts;
        }

        public int length() {
            return length;
        }

        public LuaValue getCancelAction() {
            return cancelAction;
        }

        @NonNull
        @Override
        public String toString() {
            return "Context{" +
                    "query=" + query +
                    ", parts=" + parts +
                    ", cancelAction=" + cancelAction +
                    ", length=" + length +
                    '}';
        }
    }

    static class Part {

        private final int start;
        private final int end;

        private final LuaValue action;

        public Part(int start, int end, LuaValue action) {
            this.start = start;
            this.end = end;
            this.action = action;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public LuaValue getAction() {
            return action;
        }

        @NonNull
        @Override
        public String toString() {
            return "Part{" +
                    "start=" + start +
                    ", end=" + end +
                    ", action=" + action +
                    '}';
        }
    }

    class Create extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
            StringBuilder result = new StringBuilder();

            Query query = (Query) arg1.checkuserdata(Query.class);
            HashSet<Part> parts = new HashSet<>();

            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = arg2.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                LuaValue v = n.arg(2);
                if (v instanceof LuaTable) {
                    String caption = v.get("caption").tojstring();
                    parts.add(new Part(
                            query.getStartPosition() + result.length(),
                            query.getStartPosition() + result.length() + caption.length(),
                            v.get("action")));
                    result.append(caption);
                } else {
                    result.append(v.tojstring());
                }
            }

            query.answer(result.toString());

            Context context = new Context(
                    query,
                    parts,
                    arg3,
                    query.getText().length());

            current.put(query.getAccessibilityNodeInfo(), context);
            return CoerceJavaToLua.coerce(context);
        }
    }
}


