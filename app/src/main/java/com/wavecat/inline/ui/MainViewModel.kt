package com.wavecat.inline.ui

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.modules.DEFAULT_ASSETS_PATH
import com.wavecat.inline.service.modules.UNLOADED
import com.wavecat.inline.service.modules.defaultUnloaded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _modules = MutableLiveData<List<ModuleItem>>()
    val modules: LiveData<List<ModuleItem>> = _modules

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _repositoryUrl =
        MutableLiveData<String?>(sharedPreferences.getString(REPOSITORY_URL, REPOSITORY))

    val repositoryUrl: LiveData<String?> = _repositoryUrl

    private val client = OkHttpClient()

    private val modulesPath = File(application.getExternalFilesDirs(null)[0].absolutePath + "/modules")
        .apply { mkdirs() }

    private val internalModules = application.assets.list(DEFAULT_ASSETS_PATH) ?: arrayOf()

    private val unloaded: MutableSet<String> =
        HashSet(sharedPreferences.getStringSet(UNLOADED, defaultUnloaded)!!)

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
                        description = "Internal module",
                        isLoaded = !unloaded.contains(moduleName)
                    )
                }

                val mergedModules = moduleList + internalModuleItems

                _modules.postValue(mergedModules)
                _errorMessage.postValue(null)
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

                val updatedModule = module.copy(isInstalled = true)

                _modules.postValue(_modules.value?.map {
                    if (it is ModuleItem.External && it.name == module.name) updatedModule else it
                })

                postError(null)
                InlineService.instance?.createEnvironment()
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
            apply()
        }

        val enabledModule = module.copy(isLoaded = true)
        _modules.value = _modules.value?.map {
            if (it is ModuleItem.Internal && it.name == module.name) enabledModule else it
        }
    }

    fun disableModule(module: ModuleItem.Internal) {
        sharedPreferences.edit {
            unloaded.add(module.name)
            putStringSet(UNLOADED, unloaded)
            apply()
        }

        val disabledModule = module.copy(isLoaded = false)
        _modules.value = _modules.value?.map {
            if (it is ModuleItem.Internal && it.name == module.name) disabledModule else it
        }
    }

    fun removeModule(module: ModuleItem.External) {
        val moduleFile = File(modulesPath, module.name)

        runCatching {
            if (moduleFile.exists()) {
                if (!moduleFile.delete())
                    return@runCatching
            }

            val removedModule = module.copy(isInstalled = false)
            _modules.value = _modules.value?.map {
                if (it is ModuleItem.External && it.name == module.name) removedModule else it
            }

            postError(null)
            InlineService.instance?.createEnvironment()
        }.onFailure {
            postError("An error occurred while removing module: ${it.localizedMessage}")
        }
    }

    private fun postError(message: String?) {
        _errorMessage.postValue(message)
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
            apply()
        }

        loadModules()
    }

    companion object {
        private const val REPOSITORY_URL = "repository_url"
        private const val REPOSITORY =
            "https://raw.githubusercontent.com/ImSkaiden/inline_modules/refs/heads/main"
    }
}