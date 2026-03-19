package com.wavecat.inline.ui

/**
 * Represents an item in a module list, which can be either external or internal.
 */
sealed interface ModuleItem {
    data class External(
        val name: String,
        val description: String,
        val isInstalled: Boolean = false,
        val hasPreferences: Boolean = false,
    ) : ModuleItem

    data class Internal(
        val name: String,
        val description: String,
        val isLoaded: Boolean = true,
        val hasPreferences: Boolean = false,
    ) : ModuleItem
}
