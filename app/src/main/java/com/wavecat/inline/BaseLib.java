package com.wavecat.inline;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

@SuppressWarnings("unused")
public class BaseLib {
    private final Context context;

    public BaseLib(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public SharedPreferences getSharedPreferences(String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public static boolean setText(AccessibilityNodeInfo accessibilityNodeInfo, String text) {
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo
                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    public static boolean setSelection(AccessibilityNodeInfo accessibilityNodeInfo, int start, int end) {
        Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start);
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end);
        return accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments);
    }

    public static boolean cut(AccessibilityNodeInfo accessibilityNodeInfo) {
        return accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CUT);
    }

    public static boolean copy(AccessibilityNodeInfo accessibilityNodeInfo) {
        return accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_COPY);
    }

    public static boolean paste(AccessibilityNodeInfo accessibilityNodeInfo) {
        return accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }

    public void toast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }
}