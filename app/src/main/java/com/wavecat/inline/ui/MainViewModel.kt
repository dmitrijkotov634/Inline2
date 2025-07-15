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

/**
 * ViewModel managing module downloads, installations, and configuration.
 *
 * Handles the complete lifecycle of Inline modules including downloading
 * from remote repositories, enabling/disabling internal modules, managing
 * external module installations, and coordinating with the InlineService
 * for module loading based on performance preferences.
 *
 * @param sharedPreferences Application preferences for persistent storage
 * @param modulesPath Directory where external modules are stored
 * @param internalModules List of built-in module names
 * @see ViewModel
 * @see InlineService
 * @see ModuleItem
 */
class MainViewModel(
    private val sharedPreferences: SharedPreferences,
    private val modulesPath: File,
    internalModules: List<String>,
) : ViewModel() {

    /**
     * Mutable state flow for the current list of modules.
     *
     * Internal state that gets updated when modules are loaded,
     * installed, or their status changes.
     */
    private val _modules = MutableStateFlow<List<ModuleItem>>(emptyList())

    /**
     * Public read-only state flow exposing the current module list.
     *
     * Observes changes to module states including installation status,
     * load status, and availability from the remote repository.
     */
    val modules: StateFlow<List<ModuleItem>> = _modules.asStateFlow()

    /**
     * Mutable state flow for error messages.
     *
     * Internal state for tracking and displaying error conditions
     * during module operations.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Public read-only state flow exposing current error messages.
     *
     * Provides error feedback for failed download, installation,
     * or loading operations.
     */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Mutable state flow for the current repository URL.
     *
     * Internal state managing the module repository endpoint,
     * initialized from shared preferences.
     */
    private val _repositoryUrl = MutableStateFlow(
        sharedPreferences.getString(REPOSITORY_URL, REPOSITORY)
    )

    /**
     * Public read-only state flow exposing the repository URL.
     *
     * Tracks the current module repository endpoint for
     * downloading and listing available modules.
     */
    val repositoryUrl: StateFlow<String?> = _repositoryUrl.asStateFlow()

    /**
     * HTTP client for module repository communication.
     *
     * Used for downloading module lists and module files
     * from the remote repository.
     */
    private val client = OkHttpClient()

    /**
     * Set of module names that are currently unloaded.
     *
     * Tracks which internal modules should not be loaded,
     * persisted in shared preferences for state restoration.
     */
    private val unloaded: MutableSet<String> =
        HashSet(sharedPreferences.getStringSet(UNLOADED, defaultUnloaded)!!)

    /**
     * Flag indicating whether recent changes have been applied to the service.
     *
     * Tracks whether module state changes need to be synchronized
     * with the InlineService for performance optimization.
     */
    private var changesApplied = true

    /**
     * Flag indicating whether all lazy-loaded modules have been loaded.
     *
     * Prevents redundant loading operations when all modules
     * are already available in the service.
     */
    private var allLoaded = false

    /**
     * List of internal module items with their current load status.
     *
     * Pre-built list of internal modules with descriptions from
     * preferences and load status based on the unloaded set.
     */
    private val internalModuleItems = internalModules.map { moduleName ->
        ModuleItem.Internal(
            name = moduleName,
            description = sharedPreferences.getString(
                "DESC$moduleName",
                "Internal module"
            )!!,
            isLoaded = !unloaded.contains(moduleName)
        )
    }

    /**
     * Updates the module list with a transformation function.
     *
     * Applies the given update function to all modules and re-sorts
     * the list with installed/loaded modules appearing first.
     *
     * @param update Function to transform each ModuleItem
     */
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

    /**
     * Shows only internal modules in the module list.
     *
     * Fallback method used when external module loading fails
     * or network access is unavailable.
     */
    private fun showInternals() {
        _modules.value = internalModuleItems
    }

    /**
     * Downloads and parses the module list from the remote repository.
     *
     * Fetches the index.tsv file from the repository, parses it to create
     * external module items, merges with internal modules, and updates
     * the module list. Falls back to internal modules on failure.
     */
    fun loadModulesList() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("${_repositoryUrl.value}/index.tsv").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    showInternals()
                    postError("Loading error: HTTP ${response.code}")
                    return@use
                }

                val body = response.body

                val moduleList = body.byteStream().bufferedReader().useLines { lines ->
                    lines.drop(1)
                        .mapNotNull { it.parseModuleLine() }
                        .toList()
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
                showInternals()
                postError("An error occurred while loading modules: ${it.localizedMessage}")
            }
    }

    /**
     * Downloads and installs an external module from the repository.
     *
     * Fetches the module file from the repository URL, saves it to the
     * modules directory, updates the module's installation status, and
     * triggers module loading if performance permits.
     *
     * @param module The external module to download and install
     */
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

                response.body.byteStream().use { input ->
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
                loadModulesIfEfficient()
            }
        }
            .onFailure {
                postError("An error occurred while downloading module: ${it.localizedMessage}")
            }
    }

    /**
     * Enables an internal module by removing it from the unloaded set.
     *
     * Updates shared preferences to persist the change and updates
     * the module's load status in the UI.
     *
     * @param module The internal module to enable
     */
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


    /**
     * Disables an internal module by adding it to the unloaded set.
     *
     * Updates shared preferences to persist the change and updates
     * the module's load status in the UI.
     *
     * @param module The internal module to disable
     */
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

    /**
     * Removes an external module by deleting its file.
     *
     * Deletes the module file from the modules directory, updates
     * the module's installation status, and triggers module reloading
     * if performance permits.
     *
     * @param module The external module to remove
     */
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
            loadModulesIfEfficient()
        }.onFailure {
            postError("An error occurred while removing module: ${it.localizedMessage}")
        }
    }

    /**
     * Loads modules in the service if performance metrics allow.
     *
     * Checks the environment performance metric and only triggers
     * immediate module loading if performance is acceptable,
     * otherwise defers loading until later.
     */
    fun loadModulesIfEfficient() {
        allLoaded = false
        val perfValue = sharedPreferences.getLong(ENVIRONMENT_PERF, 0)

        if (perfValue < 500) {
            InlineService.instance?.loadModules()
        } else {
            changesApplied = false
        }
    }


    /**
     * Handles activity pause by applying pending changes.
     *
     * Called when the activity is paused to ensure any pending
     * module changes are applied to the service.
     */
    fun onPause() {
        if (!changesApplied) {
            InlineService.instance?.loadModules()
            changesApplied = true
        }
    }

    /**
     * Reloads the entire module environment.
     *
     * Recreates the Lua environment in the service, forcing
     * a complete reload of all modules and resetting state flags.
     */
    fun reload() = InlineService.instance?.apply {
        createEnvironment()
        changesApplied = true
        allLoaded = false
    }

    /**
     * Forces loading of all lazy-loaded modules.
     *
     * Triggers immediate loading of all modules that were deferred
     * for performance reasons, ensuring complete functionality.
     */
    fun loadAll() {
        if (allLoaded) return

        InlineService.instance?.forceLoadLazy()

        changesApplied = true
        allLoaded = true
    }

    /**
     * Posts an error message to the error state flow.
     *
     * @param message The error message to display, or null to clear errors
     */
    private fun postError(message: String?) {
        _errorMessage.value = message
    }

    /**
     * Parses a line from the module repository index file.
     *
     * Extension function that parses tab-separated values to create
     * external module items with installation status based on local files.
     *
     * @receiver String The line to parse from index.tsv
     * @return ModuleItem.External? The parsed module item, or null if invalid
     */
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

    /**
     * Updates the repository URL and reloads the module list.
     *
     * Persists the new URL in shared preferences and triggers
     * a fresh load of the module list from the new repository.
     *
     * @param newUrl The new repository URL to use
     */
    fun updateUrl(newUrl: String) {
        _repositoryUrl.value = newUrl

        sharedPreferences.edit {
            putString(REPOSITORY_URL, newUrl)
        }

        loadModulesList()
    }

    companion object {
        /**
         * SharedPreferences key for storing the repository URL.
         */
        private const val REPOSITORY_URL = "repository_url"

        /**
         * Default repository URL for Inline modules.
         */
        private const val REPOSITORY = "https://inlineapp.github.io/modules"
    }
}