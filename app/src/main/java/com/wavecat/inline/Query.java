package com.wavecat.inline;

import android.view.accessibility.AccessibilityNodeInfo;

@SuppressWarnings("unused")
public class Query {

    private final AccessibilityNodeInfo accessibilityNodeInfo;

    private final String currentText;
    private final String expression;
    private final String args;

    private String text;

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

        text = replaceExpression(reply);

        int position = accessibilityNodeInfo.getTextSelectionStart() - expression.length() + message.length();

        InlineService.setText(accessibilityNodeInfo, text);
        InlineService.setSelection(accessibilityNodeInfo, position, position);
    }
}
