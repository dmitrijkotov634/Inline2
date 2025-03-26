package com.wavecat.inline.preferences

import android.content.SharedPreferences
import android.view.View

interface Preference {
    fun getView(preferences: SharedPreferences?): View

    fun setWindowFocusListener(requestFocus: () -> Unit) {}
}

