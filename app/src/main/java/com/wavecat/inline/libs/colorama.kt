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

/**
 * Lua library for text formatting using HTML-like tags.
 *
 * Provides functionality to format text with HTML markup, including
 * bold, italic, colors, headers, and other styling options. Uses the
 * system clipboard to insert formatted HTML content into text fields.
 */
class colorama : TwoArgFunction() {

    companion object {
        /**
         * Preference key for disabling HTML formatting.
         *
         * Used to check if HTML formatting should be disabled
         * in user preferences.
         */
        private const val DISABLE_HTML_PREF = "disable_html"

        /**
         * Lazy-initialized reference to the Inline service.
         *
         * @see com.wavecat.inline.service.InlineService
         */
        private val service by lazy { requireService() }

        /**
         * System clipboard manager for HTML content operations.
         *
         * Used to copy formatted HTML content to clipboard for
         * pasting into text fields with formatting preservation.
         *
         * @see ClipboardManager
         */
        val clipboardManager: ClipboardManager by lazy {
            service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }

        /**
         * Flag indicating whether HTML formatting is available.
         *
         * Determined by user preferences - when disabled, only
         * plain text formatting is used.
         */
        private val availability by lazy {
            !service.defaultSharedPreferences.getBoolean(DISABLE_HTML_PREF, false)
        }

        /**
         * Converts HTML string to plain text using Android's Html parser.
         *
         * Strips HTML formatting and returns the plain text content,
         * handling API level differences for Html.fromHtml().
         *
         * @param reply The HTML string to convert
         * @return String Plain text representation of the HTML
         * @see Html.fromHtml
         */
        fun formatHtml(reply: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(reply, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(reply)
        }.toString()
    }

    /**
     * Initializes the Lua library with text formatting functions.
     *
     * Creates and populates a Lua table with all available HTML
     * formatting functions, tag wrappers, and utility functions.
     *
     * @param name The name of the library (unused)
     * @param env The Lua environment to register the library in
     * @return LuaValue The created library table
     * @see LuaValue
     */
    override fun call(name: LuaValue, env: LuaValue): LuaValue {
        val library: LuaValue = tableOf()

        /**
         * Wraps a command function to support formatted output via ColoramaQuery.
         *
         * Creates a wrapper that replaces the standard Query object with
         * a ColoramaQuery that supports HTML formatting in responses.
         *
         * param value The command function to wrap
         * @return function Wrapped function that receives ColoramaQuery
         * @see ColoramaQuery
         */
        library["wrap"] = oneArgFunction { value ->
            value.checkfunction()

            twoArgFunction { input, query ->
                value.call(
                    input,
                    CoerceJavaToLua.coerce(ColoramaQuery(query.checkuserdata(Query::class.java) as Query))
                )
            }
        }


        /**
         * Creates a ColoramaQuery from a regular Query object.
         *
         * Converts a standard Query to one that supports HTML formatting
         * for manual use in command functions.
         *
         * param query The Query object to convert
         * @return ColoramaQuery Query object with HTML formatting support
         * @see ColoramaQuery
         */
        library["of"] = oneArgFunction { query ->
            CoerceJavaToLua.coerce(ColoramaQuery(query.checkuserdata(Query::class.java) as Query))
        }

        /**
         * Escapes special HTML characters in text.
         *
         * Converts characters with special meaning in HTML to their
         * encoded equivalents for safe inclusion in HTML markup.
         *
         * param text The text to escape
         * @return string HTML-escaped text
         * @see String.htmlEncode
         */
        library["quote"] = oneArgFunction { text ->
            valueOf(text.checkjstring().htmlEncode())
        }

        /**
         * Wraps text in a font tag with optional color attribute.
         *
         * Creates HTML font markup with optional color styling.
         * Text content is automatically HTML-escaped.
         *
         * param text The text content
         * @param color Optional color value (nil for no color)
         * @return string HTML font tag with content
         */
        library["font"] = twoArgFunction { text, color ->
            val colorAttr = color.takeIf { !it.isnil() }?.tojstring()?.let { " color=\"$it\"" } ?: ""
            valueOf("<font$colorAttr>${text.tojstring().htmlEncode()}</font>")
        }

        /**
         * Joins multiple text arguments with a separator.
         *
         * Concatenates variable number of text arguments using
         * the specified separator string.
         *
         * param varargs First argument is separator, rest are text to join
         * @return string Joined text or NIL if fewer than 2 arguments
         */
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


        /**
         * Bold text formatting function.
         *
         * @see HtmlTag
         */
        library["bold"] = HtmlTag("b")

        /**
         * Italic text formatting function.
         *
         * @see HtmlTag
         */
        library["italic"] = HtmlTag("i")

        /**
         * Small text formatting function.
         *
         * @see HtmlTag
         */
        library["small"] = HtmlTag("small")

        /**
         * Big text formatting function.
         *
         * @see HtmlTag
         */
        library["big"] = HtmlTag("big")

        /**
         * Strikethrough text formatting function.
         *
         * @see HtmlTag
         */
        library["strike"] = HtmlTag("strike")

        /**
         * Preformatted text formatting function.
         *
         * @see HtmlTag
         */
        library["pre"] = HtmlTag("pre")

        /**
         * Subscript text formatting function.
         *
         * @see HtmlTag
         */
        library["subscript"] = HtmlTag("sub")

        /**
         * Superscript text formatting function.
         *
         * @see HtmlTag
         */
        library["superscript"] = HtmlTag("sup")

        /**
         * Header level 1 formatting function.
         *
         * @see HeaderTag
         */
        library["h1"] = HeaderTag(1)

        /**
         * Header level 2 formatting function.
         *
         * @see HeaderTag
         */
        library["h2"] = HeaderTag(2)

        /**
         * Header level 3 formatting function.
         *
         * @see HeaderTag
         */
        library["h3"] = HeaderTag(3)

        /**
         * Header level 4 formatting function.
         *
         * @see HeaderTag
         */
        library["h4"] = HeaderTag(4)

        /**
         * Header level 5 formatting function.
         *
         * @see HeaderTag
         */
        library["h5"] = HeaderTag(5)

        /**
         * Header level 6 formatting function.
         *
         * @see HeaderTag
         */
        library["h6"] = HeaderTag(6)

        /**
         * HTML line break constant.
         *
         * String containing HTML line break tag for text formatting.
         */
        library["newline"] = "<br>"

        env["colorama"] = library
        env["package"]["loaded"]["colorama"] = library

        return library
    }

    /**
     * Lua function for wrapping text in HTML tags.
     *
     * Generic function class that creates HTML markup by wrapping
     * text content in opening and closing tags.
     *
     * @property tag The HTML tag name to use
     */
    class HtmlTag(private val tag: String) : OneArgFunction() {
        /**
         * Wraps the input text in HTML tags.
         *
         * @param text The text to wrap
         * @return LuaValue HTML markup with text wrapped in tags
         */
        override fun call(text: LuaValue): LuaValue {
            return valueOf("<$tag>${text.checkjstring()}</$tag>")
        }
    }

    /**
     * Lua function for creating HTML header tags.
     *
     * Specialized function class that creates HTML header markup
     * with the specified header level (h1-h6).
     *
     * @property size The header level (1-6)
     */
    class HeaderTag(private val size: Int) : OneArgFunction() {
        /**
         * Wraps the input text in header tags.
         *
         * @param text The text to wrap
         * @return LuaValue HTML header markup
         */
        override fun call(text: LuaValue): LuaValue {
            return valueOf("<h$size>${text.checkjstring()}</h$size>")
        }
    }

    /**
     * Enhanced Query class with HTML formatting support.
     *
     * Extends the standard Query functionality to support HTML-formatted
     * responses that are inserted via clipboard with formatting preservation.
     *
     * @param query The base Query object to enhance
     */
    private class ColoramaQuery(query: Query) : Query(
        accessibilityNodeInfo = query.accessibilityNodeInfo,
        currentText = query.text,
        expression = query.expression,
        args = query.args
    ) {
        /**
         * Starting position of the expression in the text.
         */
        private val startExp = startPosition

        /**
         * Ending position of the expression in the text.
         *
         * Updated as content is replaced to track the current
         * bounds of the formatted content.
         */
        private var endExp = startPosition + expression.length

        /**
         * Answers with HTML-formatted content via clipboard insertion.
         *
         * Converts HTML to plain text if formatting is disabled, otherwise
         * uses clipboard to insert formatted HTML content with multiple
         * retry attempts for reliability.
         *
         * @param reply The HTML-formatted response text
         * @param cursorToEnd Whether to position cursor at end after insertion
         */
        override fun answer(reply: String?, cursorToEnd: Boolean) {
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
                    if (cursorToEnd)
                        endExp = text.length

                    setSelection(accessibilityNodeInfo, endExp, endExp)
                    break
                }

                answerRaw(raw)
            }
        }

        /**
         * Answers with plain text content bypassing HTML formatting.
         *
         * Fallback method for plain text responses when HTML
         * formatting fails or is disabled.
         *
         * @param reply The plain text response
         */
        fun answerRaw(reply: String?) = super.answer(reply, false)

        /**
         * Provides detailed string representation for debugging.
         *
         * @return String Debug information about the query state
         */
        override fun toString(): String {
            return "ColoramaQuery{currentText=$currentText, expression=$expression, args=$args, text=$text, startExp=$startExp, endExp=$endExp}"
        }
    }
}
