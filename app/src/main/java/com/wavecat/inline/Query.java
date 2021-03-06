package com.wavecat.inline;

import android.view.accessibility.AccessibilityNodeInfo;

@SuppressWarnings("unused")
public class Query {

    protected final AccessibilityNodeInfo accessibilityNodeInfo;

    protected final String currentText;
    protected final String expression;
    protected final String args;

    protected String text;

    public Query(AccessibilityNodeInfo accessibilityNodeInfo, String text, String expression, String args) {
        this.accessibilityNodeInfo = accessibilityNodeInfo;

        this.text = currentText = text;
        this.expression = expression;
        this.args = args;
    }

    public AccessibilityNodeInfo getAccessibilityNodeInfo() {
        return accessibilityNodeInfo;
    }

    public String getText() {
        return text;
    }

    public String getExpression() {
        return expression;
    }

    public String getArgs() {
        return args == null ? "" : args;
    }

    public String replaceExpression(String replacement) {
        return currentText.replace(expression, replacement);
    }

    public void answer(String reply) {
        String message = reply == null ? "" : reply;

        text = replaceExpression(message);

        int position = accessibilityNodeInfo.getTextSelectionStart() - expression.length() + message.length();

        InlineService.setText(accessibilityNodeInfo, text);
        InlineService.setSelection(accessibilityNodeInfo, position, position);
    }
}
