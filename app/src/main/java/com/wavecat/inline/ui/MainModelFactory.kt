package com.wavecat.inline.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File

class MainModelFactory(
    private val sharedPreferences: SharedPreferences,
    private val modulesPath: File,
    private val internalModules: List<String>,
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass == MainViewModel::class.java)
            return MainViewModel(
                sharedPreferences = sharedPreferences,
                modulesPath = modulesPath,
                internalModules = internalModules
            ) as T

        throw IllegalArgumentException()
    }
}
