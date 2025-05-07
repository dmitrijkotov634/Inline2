@file:Suppress("ClassName", "unused")

package com.wavecat.inline.libs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.Html
import androidx.core.text.htmlEncode
import com.wavecat.inline.extensions.oneArgFunction
import com.wavecat.inline.extensions.twoArgFunction
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.service.InlineService.Companion.paste
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.service.InlineService.Companion.setSelection
import com.wavecat.inline.service.commands.Query
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua


class colorama : TwoArgFunction() {
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        library["wrap"] = oneArgFunction { value ->
            value.checkfunction()

            twoArgFunction { input, query ->
                value.call(
                    input,
                    CoerceJavaToLua.coerce(ColoramaQuery(query.checkuserdata(Query::class.java) as Query))
                )
            }
        }

        library["of"] = oneArgFunction { query ->
            CoerceJavaToLua.coerce(ColoramaQuery(query.checkuserdata(Query::class.java) as Query))
        }

        library["quote"] = oneArgFunction { text ->
            valueOf(text.checkjstring().htmlEncode())
        }

        library["font"] = twoArgFunction { text, color ->
            val colorAttr = color.takeIf { !it.isnil() }?.tojstring()?.let { " color=\"$it\"" } ?: ""
            valueOf("<font$colorAttr>${text.tojstring().htmlEncode()}</font>")
        }

        library["text"] = varArgFunction { varargs ->
            if (varargs.narg() < 2)
                NIL
            else
                buildString {
                    val separator = varargs.arg1().checkjstring()
                    for (i in 2..varargs.narg()) {
                        if (i > 2) append(separator)
                        append(varargs.arg(i).checkjstring())
                    }
                }.let {
                    valueOf(it)
                }
        }

        library["bold"] = HtmlTag("b")
        library["italic"] = HtmlTag("i")
        library["small"] = HtmlTag("small")
        library["big"] = HtmlTag("big")
        library["strike"] = HtmlTag("strike")
        library["pre"] = HtmlTag("pre")
        library["subscript"] = HtmlTag("sub")
        library["superscript"] = HtmlTag("sup")

        library["h1"] = HeaderTag(1)
        library["h2"] = HeaderTag(2)
        library["h3"] = HeaderTag(3)
        library["h4"] = HeaderTag(4)
        library["h5"] = HeaderTag(5)
        library["h6"] = HeaderTag(6)

        library["newline"] = "<br>"

        env["colorama"] = library
        env["package"]["loaded"]["colorama"] = library

        return library
    }

    class HtmlTag(private val tag: String) : OneArgFunction() {
        override fun call(text: LuaValue): LuaValue {
            return valueOf("<$tag>${text.checkjstring()}</$tag>")
        }
    }

    class HeaderTag(private val size: Int) : OneArgFunction() {
        override fun call(text: LuaValue): LuaValue {
            return valueOf("<h$size>${text.checkjstring()}</h$size>")
        }
    }

    private class ColoramaQuery(query: Query) : Query(
        accessibilityNodeInfo = query.accessibilityNodeInfo,
        currentText = query.text,
        expression = query.expression,
        args = query.args
    ) {
        private val startExp = startPosition
        private var endExp = startPosition + expression.length

        override fun answer(reply: String?) {
            val raw = reply?.let { formatHtml(it) } ?: return answerRaw(null)

            if (!availability)
                return answerRaw(raw)

            for (attempt in 0..2) {
                clipboardManager.setPrimaryClip(ClipData.newHtmlText("colorama", raw, reply))

                setSelection(accessibilityNodeInfo, startExp, endExp)
                paste(accessibilityNodeInfo)

                text = replaceExpression(raw)
                endExp = startExp + raw.length

                accessibilityNodeInfo.refresh()
                if (accessibilityNodeInfo.text.length == text.length) {
                    setSelection(accessibilityNodeInfo, endExp, endExp)
                    break
                }

                answerRaw(raw)
            }
        }

        fun answerRaw(reply: String?) = super.answer(reply)

        override fun toString(): String {
            return "ColoramaQuery{currentText=$currentText, expression=$expression, args=$args, text=$text, startExp=$startExp, endExp=$endExp}"
        }
    }

    companion object {
        private const val DISABLE_HTML_PREF = "disable_html"

        private val service = requireService()

        val clipboardManager: ClipboardManager by lazy {
            service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }

        private var availability = !service.defaultSharedPreferences.getBoolean(DISABLE_HTML_PREF, false)

        fun formatHtml(reply: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(reply, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(reply)
        }.toString()
    }
}
