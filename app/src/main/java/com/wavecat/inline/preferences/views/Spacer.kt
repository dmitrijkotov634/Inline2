package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.utils.dp

@SuppressLint("ViewConstructor")
open class Spacer(context: Context, private val padding: Int) : View(context), Preference {

    private var isVerticalParent = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val parent = parent
        if (parent is LinearLayout)
            isVerticalParent = parent.orientation == LinearLayout.VERTICAL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val finalWidth = if (isVerticalParent) 0 else padding.dp
        val finalHeight = if (!isVerticalParent) 0 else padding.dp
        setMeasuredDimension(finalWidth, finalHeight)
    }

    override fun getView(preferences: SharedPreferences?): View = this
}
