package com.wavecat.inline.ui

/**
 * Represents an item in a module list, which can be either external or internal.
 *
 * This sealed interface defines the common properties and behavior of module items.
 * Concrete implementations, `External` and `Internal`, provide specific details for each type.
 */
sealed interface ModuleItem {
    data class External(
        val name: String,
        val description: String,
        val isInstalled: Boolean = false,
    ) : ModuleItem

    data class Internal(
        val name: String,
        val description: String,
        val isLoaded: Boolean = true,
    ) : ModuleItem
}
