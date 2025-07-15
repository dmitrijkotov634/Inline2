package com.wavecat.inline.service.commands

import android.view.accessibility.AccessibilityNodeInfo
import com.wavecat.inline.service.InlineService.Companion.setSelection
import com.wavecat.inline.service.InlineService.Companion.setText

/**
 * Represents a query made by the user in an input field.
 *
 * This class encapsulates information about the query, such as the input field's
 * AccessibilityNodeInfo, the current text in the field, the matched expression,
 * and any arguments provided with the query. It also provides methods to manipulate
 * the text in the input field and answer the query.
 *
 * @property accessibilityNodeInfo The AccessibilityNodeInfo of the input field.
 * @property currentText The current text in the input field when the query was made.
 * @property expression The specific part of the currentText that matched the query pattern.
 * @property args Any arguments provided with the query (text following the matched expression).
 */
open class Query(
    val accessibilityNodeInfo: AccessibilityNodeInfo,
    protected var currentText: String,
    val expression: String,
    val args: String,
) {
    var text: String
        protected set

    init {
        text = currentText
    }

    /**
     * Replaces the matched expression in the [currentText] with the provided [replacement] string.
     *
     * @param replacement The string to replace the matched expression with.
     * @return The string with the expression replaced.
     */
    fun replaceExpression(replacement: String): String {
        return currentText.replace(expression, replacement)
    }

    val startPosition: Int
        get() = currentText.indexOf(expression)

    /**
     * Replaces the matched expression in the text field with the given reply and sets the cursor position.
     *
     * @param reply The string to replace the expression with. If null, the expression is replaced with an empty string.
     * @param cursorToEnd If true, the cursor is moved to the end of the text field after the replacement.
     *                    Otherwise, the cursor is moved to the end of the replaced text.
     */
    open fun answer(reply: String?, cursorToEnd: Boolean = false) {
        val message = reply.orEmpty()
        text = replaceExpression(message)

        val position = if (cursorToEnd) text.length else
            accessibilityNodeInfo.textSelectionStart - expression.length + message.length

        setText(accessibilityNodeInfo, text)
        setSelection(accessibilityNodeInfo, position, position)
    }

    override fun toString(): String {
        return "Query(currentText=$currentText, expression=$expression, args=$args, text=$text)"
    }
}
