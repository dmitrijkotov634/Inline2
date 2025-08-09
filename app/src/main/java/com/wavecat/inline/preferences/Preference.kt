package com.wavecat.inline.preferences

import android.content.SharedPreferences
import android.view.View

/**
 * Interface for preference UI components with SharedPreferences integration.
 *
 * Defines the contract for creating preference views that can interact
 * with SharedPreferences for data persistence and support focus management.
 * Implemented by all preference UI components like buttons, checkboxes, etc.
 */
interface Preference {

    /**
     * Creates and configures the view for this preference.
     *
     * Implementations should create their UI component, configure it
     * with any stored preference values, and set up listeners for
     * persistence and callbacks.
     *
     * @param preferences SharedPreferences instance for reading/writing values, may be null
     * @param requestFocus Callback function to request focus on floating window
     * @return View The configured preference view ready for display
     */
    fun getView(preferences: SharedPreferences?, requestFocus: () -> Unit): View
}