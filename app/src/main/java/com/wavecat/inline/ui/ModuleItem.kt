package com.wavecat.inline.ui

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
