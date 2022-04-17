package com.wavecat.inline;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Query {

    private final AccessibilityNodeInfo accessibilityNodeInfo;

    private final String currentText;
    private final String match;
    private final String args;

    private String text;

    public Query(AccessibilityNodeInfo accessibilityNodeInfo, String text, String match, String args) {
        this.accessibilityNodeInfo = accessibilityNodeInfo;

        this.text = currentText = text;
        this.match = match;
        this.args = args;
    }

    public String getText() {
        return text;
    }

    public String getMatch() {
        return match;
    }

    public String getArgs() {
        if (args == null)
            return "";

        return args;
    }

    public void answer(String reply) {
        text = currentText.replaceFirst(Pattern.quote(match), reply);

        BaseLib.setText(accessibilityNodeInfo, text);
        BaseLib.setSelection(accessibilityNodeInfo,
                accessibilityNodeInfo.getTextSelectionStart(),
                accessibilityNodeInfo.getTextSelectionEnd());
    }
}
