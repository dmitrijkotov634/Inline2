package com.wavecat.inline.preferences.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import com.wavecat.inline.preferences.Preference
import com.wavecat.inline.utils.dp

/**
 * A custom view that acts as a spacer in a layout.
 * It can be used to add padding between other views.
 * The spacer can be either horizontal or vertical, depending on the orientation of its parent LinearLayout.
 *
 * @param context The context in which the spacer is created.
 * @param padding The amount of padding to add, in dp.
 */
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

    override fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View = this
}
