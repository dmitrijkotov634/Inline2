package com.wavecat.inline.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;

@SuppressLint("ViewConstructor")
public class Text extends AppCompatTextView implements Preference {

    public Text(Context context, String text) {
        super(context);
        setText(text);
    }

    @Override
    public View getView(SharedPreferences preferences) {
        return this;
    }
}
