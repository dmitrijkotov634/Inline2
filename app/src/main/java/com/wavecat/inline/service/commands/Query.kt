package com.wavecat.inline.service.commands

import android.view.accessibility.AccessibilityNodeInfo
import com.wavecat.inline.service.InlineService.Companion.setSelection
import com.wavecat.inline.service.InlineService.Companion.setText

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

    fun replaceExpression(replacement: String): String {
        return currentText.replace(expression, replacement)
    }

    val startPosition: Int
        get() = currentText.indexOf(expression)

    open fun answer(reply: String?) {
        val message = reply.orEmpty()
        text = replaceExpression(message)

        val position = accessibilityNodeInfo.textSelectionStart - expression.length + message.length

        setText(accessibilityNodeInfo, text)
        setSelection(accessibilityNodeInfo, position, position)
    }

    override fun toString(): String {
        return "Query(currentText=$currentText, expression=$expression, args=$args, text=$text)"
    }
}
