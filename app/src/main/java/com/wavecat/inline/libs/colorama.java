package com.wavecat.inline.libs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.text.TextUtils;

import com.wavecat.inline.InlineService;
import com.wavecat.inline.Query;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

@SuppressWarnings("unused")
public class colorama extends TwoArgFunction {

    private static ClipboardManager clipboardManager;
    private static boolean availability = true;

    @Override
    public LuaValue call(LuaValue name, LuaValue env) {
        LuaValue library = tableOf();

        library.set("init", new init());
        library.set("wrap", new wrap());
        library.set("of", new of());
        library.set("quote", new quote());
        library.set("font", new font());
        library.set("text", new text());

        library.set("bold", new HtmlTag("b"));
        library.set("italic", new HtmlTag("i"));
        library.set("small", new HtmlTag("small"));
        library.set("big", new HtmlTag("big"));
        library.set("strike", new HtmlTag("strike"));
        library.set("subscript", new HtmlTag("sub"));
        library.set("superscript", new HtmlTag("sup"));
        library.set("h1", new HTag(1));
        library.set("h2", new HTag(2));
        library.set("h3", new HTag(3));
        library.set("h4", new HTag(4));
        library.set("h5", new HTag(5));
        library.set("h6", new HTag(6));

        library.set("newline", "<br>");
        library.set("space", " ");

        env.set("colorama", library);
        env.get("package").get("loaded").set("colorama", library);

        return library;
    }

    static class init extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue context, LuaValue availability) {
            if (!context.isnil())
                clipboardManager = (ClipboardManager) ((Context) context.checkuserdata(Context.class)).getSystemService(Context.CLIPBOARD_SERVICE);

            if (!availability.isnil())
                colorama.availability = availability.checkboolean();

            return NIL;
        }
    }

    static class wrap extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue value) {
            return new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue input, LuaValue query) {
                    value.call(input, CoerceJavaToLua.coerce(new ColoramaQuery((Query) query.checkuserdata(Query.class))));

                    return NIL;
                }
            };
        }
    }

    static class of extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue query) {
            return CoerceJavaToLua.coerce(new ColoramaQuery((Query) query.checkuserdata(Query.class)));
        }
    }

    static class HtmlTag extends OneArgFunction {
        private final String htmlTag;

        public HtmlTag(String htmlTag) {
            this.htmlTag = htmlTag;
        }

        @Override
        public LuaValue call(LuaValue text) {
            return valueOf("<" + htmlTag + ">" + text.checkjstring() + "</" + htmlTag + ">");
        }
    }

    static class HTag extends OneArgFunction {
        private final int size;

        public HTag(int size) {
            this.size = size;
        }

        @Override
        public LuaValue call(LuaValue text) {
            return valueOf("<h" + size + ">" + text.checkjstring() + "</h" + size + ">");
        }
    }

    static class font extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue text, LuaValue color) {
            return valueOf("<font"
                    + (color.isnil() ? "" : " color=\"" + color.tojstring() + "\"")
                    + ">" + TextUtils.htmlEncode(text.tojstring()) + "</font>");
        }
    }

    static class quote extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue text) {
            return valueOf(TextUtils.htmlEncode(text.checkjstring()));
        }
    }

    static class text extends VarArgFunction {
        @Override
        public LuaValue invoke(Varargs varargs) {
            StringBuilder result = new StringBuilder();

            for (int n = 2; n < varargs.narg() + 1; n++) {
                result.append(varargs.arg(n).checkjstring());
                result.append(varargs.arg1().checkjstring());
            }

            result.delete(result.length() - varargs.arg1().checkjstring().length(), result.length());

            return valueOf(result.toString());
        }
    }

    private static class ColoramaQuery extends Query {

        private final int startExp;
        private int endExp;

        public ColoramaQuery(Query query) {
            super(query.getAccessibilityNodeInfo(), query.getText(), query.getExpression(), query.getArgs());

            int index = query.getText().indexOf(query.getExpression());

            startExp = index;
            endExp = index + query.getExpression().length();
        }

        public void answer(String html) {
            if (html == null || html.isEmpty()) {
                answerRaw(html);
                return;
            }

            String text = Html.fromHtml(html).toString();

            if (!availability) {
                super.answer(text);
                return;
            }

            clipboardManager.setPrimaryClip(ClipData.newHtmlText("colorama", "colorama answer", html));

            InlineService.setSelection(getAccessibilityNodeInfo(), startExp, endExp);
            InlineService.paste(getAccessibilityNodeInfo());

            InlineService.setSelection(getAccessibilityNodeInfo(), endExp = startExp + text.length(), endExp);
        }

        public void answerRaw(String reply) {
            super.answer(reply);
        }
    }
}
