package com.wavecat.inline.preferences;

import android.content.SharedPreferences;
import android.view.View;

public interface Preference {
    View getView(SharedPreferences preferences);
}

