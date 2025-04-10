package com.wavecat.inline.ui

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.ENVIRONMENT_PERF
import com.wavecat.inline.service.modules.UNLOADED
import com.wavecat.inline.service.modules.defaultUnloaded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class MainViewModel(
    private val sharedPreferences: SharedPreferences,
    private val modulesPath: File,
    private val internalModules: List<String>,
) : ViewModel() {

    private val _modules = MutableStateFlow<List<ModuleItem>>(emptyList())
    val modules: StateFlow<List<ModuleItem>> = _modules.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _repositoryUrl = MutableStateFlow(
        sharedPreferences.getString(REPOSITORY_URL, REPOSITORY)
    )

    val repositoryUrl: StateFlow<String?> = _repositoryUrl.asStateFlow()

    private val client = OkHttpClient()

    private val unloaded: MutableSet<String> =
        HashSet(sharedPreferences.getStringSet(UNLOADED, defaultUnloaded)!!)

    private var applyChanges = false

    private fun updateModuleList(update: (ModuleItem) -> ModuleItem) {
        _modules.update { current ->
            current
                .map(update)
                .sortedWith(
                    compareByDescending<ModuleItem> {
                        (it as? ModuleItem.External)?.isInstalled ?: true
                    }.thenByDescending {
                        (it as? ModuleItem.Internal)?.isLoaded ?: false
                    }
                )
        }
    }

    fun loadModules() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("${_repositoryUrl.value}/index.tsv").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    postError("Loading error: HTTP ${response.code}")
                    return@use
                }

                val body = response.body!!

                val moduleList = body.byteStream().bufferedReader().useLines { lines ->
                    lines.drop(1)
                        .mapNotNull { it.parseModuleLine() }
                        .toList()
                }

                val internalModuleItems = internalModules.map { moduleName ->
                    ModuleItem.Internal(
                        name = moduleName,
                        description = sharedPreferences.getString(
                            "DESC$moduleName",
                            "Internal module"
                        )!!,
                        isLoaded = !unloaded.contains(moduleName)
                    )
                }

                val mergedModules = (moduleList + internalModuleItems).sortedWith(
                    compareByDescending<ModuleItem> {
                        (it as? ModuleItem.External)?.isInstalled ?: true
                    }.thenByDescending {
                        (it as? ModuleItem.Internal)?.isLoaded ?: false
                    }
                )

                _modules.value = mergedModules
                _errorMessage.value = null
            }
        }
            .onFailure {
                postError("An error occurred while loading modules: ${it.localizedMessage}")
            }
    }

    fun downloadModule(module: ModuleItem.External) = viewModelScope.launch(Dispatchers.IO) {
        val url = "${_repositoryUrl.value}/${module.name}"
        val outputFile = File(modulesPath, module.name)

        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    postError("Downloading error: HTTP ${response.code}")
                    return@use
                }

                response.body?.byteStream()?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                updateModuleList { item ->
                    if (item is ModuleItem.External && item.name == module.name)
                        item.copy(isInstalled = true)
                    else item
                }

                postError(null)
                createEnvironmentIfEfficient()
            }
        }
            .onFailure {
                postError("An error occurred while downloading module: ${it.localizedMessage}")
            }
    }

    fun enableModule(module: ModuleItem.Internal) {
        sharedPreferences.edit {
            unloaded.remove(module.name)
            putStringSet(UNLOADED, unloaded)
        }

        updateModuleList { item ->
            if (item is ModuleItem.Internal && item.name == module.name)
                item.copy(isLoaded = true)
            else item
        }
    }

    fun disableModule(module: ModuleItem.Internal) {
        sharedPreferences.edit {
            unloaded.add(module.name)
            putStringSet(UNLOADED, unloaded)
        }

        updateModuleList { item ->
            if (item is ModuleItem.Internal && item.name == module.name)
                item.copy(isLoaded = false)
            else item
        }
    }

    fun removeModule(module: ModuleItem.External) {
        val moduleFile = File(modulesPath, module.name)

        runCatching {
            if (moduleFile.exists()) {
                if (!moduleFile.delete())
                    return@runCatching
            }

            updateModuleList { item ->
                if (item is ModuleItem.External && item.name == module.name)
                    item.copy(isInstalled = false)
                else item
            }

            postError(null)
            createEnvironmentIfEfficient()
        }.onFailure {
            postError("An error occurred while removing module: ${it.localizedMessage}")
        }
    }

    fun createEnvironmentIfEfficient() {
        if (sharedPreferences.getLong(ENVIRONMENT_PERF, 0) < 500)
            InlineService.instance?.createEnvironment()
        else
            applyChanges = true
    }

    fun onPause() {
        if (applyChanges) {
            InlineService.instance?.createEnvironment()
            applyChanges = false
        }
    }

    private fun postError(message: String?) {
        _errorMessage.value = message
    }

    private fun String.parseModuleLine(): ModuleItem? {
        val installed = modulesPath.list()?.toSet().orEmpty()
        val parts = split("\t", limit = 3)
        return if (parts.size == 2) {
            ModuleItem.External(
                name = parts[0],
                description = parts[1],
                isInstalled = installed.contains(parts[0])
            )
        } else null
    }

    fun updateUrl(newUrl: String) {
        _repositoryUrl.value = newUrl

        sharedPreferences.edit {
            putString(REPOSITORY_URL, newUrl)
        }

        loadModules()
    }

    companion object {
        private const val REPOSITORY_URL = "repository_url"
        private const val REPOSITORY = "https://inlineapp.github.io/modules"
    }
}