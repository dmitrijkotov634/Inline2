package com.wavecat.inline.service.modules

import android.content.SharedPreferences
import android.util.Log
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.PATH
import com.wavecat.inline.service.InlineService.Companion.TAG
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.File
import java.io.InputStream

const val DEFAULT_ASSETS_PATH: String = "modules"
const val UNLOADED: String = "unloaded"

val defaultUnloaded = setOf("loader.lua", "test.lua")


/**
 * Loads both internal and external modules into the Lua environment.
 *
 * This function orchestrates the loading of modules by calling [loadInternalModules]
 * and [loadExternalModules]. It uses [SharedPreferences] to determine which modules
 * are unloaded and which should be lazy-loaded.
 *
 * @param service The [InlineService] instance providing access to assets and preferences.
 * @param sharedPreferences The main [SharedPreferences] for accessing unloaded module settings.
 * @param defaultPath A set of default paths for external modules.
 * @param forceLazy If true, modules normally configured for lazy loading will be loaded eagerly,
 *                  and modules not configured for lazy loading will be skipped.
 *                  If false, modules will be loaded based on their individual lazy load settings.
 */
fun Globals.loadModules(
    service: InlineService,
    sharedPreferences: SharedPreferences,
    defaultPath: Set<String>,
    forceLazy: Boolean = false,
) {
    val unloaded = sharedPreferences.getStringSet(UNLOADED, defaultUnloaded) ?: defaultUnloaded
    val lazyPrefs = service.lazyLoadSharedPreferences

    loadInternalModules(service = service, lazyPrefs = lazyPrefs, unloaded = unloaded, forceLazy = forceLazy)

    loadExternalModules(
        service = service,
        sharedPreferences = sharedPreferences,
        unloaded = unloaded,
        defaultPath = defaultPath,
        lazyPrefs = lazyPrefs,
        forceLazy = forceLazy
    )
}

/**
 * Loads internal modules from the application's assets into the Lua environment.
 *
 * This function iterates through files in the [DEFAULT_ASSETS_PATH] within the app's assets.
 * For each file not present in the [unloaded] set, it determines whether to load the module
 * normally or lazily based on the [forceLazy] flag and the presence of lazy-load commands
 * in [lazyPrefs].
 *
 * It uses [loadModuleByStrategy] to perform the actual loading.
 *
 * @param service The [InlineService] instance, used to access application assets.
 * @param lazyPrefs [SharedPreferences] containing information about which commands
 *                  trigger lazy loading for specific modules.
 * @param unloaded A set of module file names that should not be loaded.
 * @param forceLazy If true, attempts to load all modules lazily, even if not explicitly
 *                  configured for lazy loading. If a module has no lazy commands defined
 *                  and `forceLazy` is true, it will be skipped.
 */
fun Globals.loadInternalModules(
    service: InlineService,
    lazyPrefs: SharedPreferences,
    unloaded: Set<String>,
    forceLazy: Boolean = false,
) {
    service.assets.list(DEFAULT_ASSETS_PATH)?.forEach { fileName ->
        if (fileName !in unloaded) {
            val path = "$DEFAULT_ASSETS_PATH/$fileName"
            val lazyCommands = lazyPrefs.getStringSet(fileName, emptySet())!!

            if (forceLazy && lazyCommands.isEmpty()) {
                Log.d(TAG, "Skip loaded module: $path")
            } else {
                loadModuleByStrategy(
                    isLazy = !(lazyCommands.isEmpty() || forceLazy),
                    service = service,
                    lazyCommands = lazyCommands,
                    lazyPrefs = lazyPrefs,
                    path = fileName,
                    isInternal = true
                ) {
                    service.assets.open(path).readScript()
                }
            }
        }
    }
}

/**
 * Loads external Lua modules into the Lua environment.
 *
 * This function iterates through a list of paths specified in [SharedPreferences]
 * (or [defaultPath] if not found) and attempts to load Lua script files from those
 * directories.
 *
 * It checks if a module is marked as "unloaded" in [sharedPreferences] and skips it
 * if so.
 *
 * It supports lazy loading of modules based on configurations in [lazyPrefs].
 *
 * @param service The [InlineService] instance.
 * @param sharedPreferences The main [SharedPreferences] used to retrieve the list
 *                          of external module paths and the set of unloaded modules.
 * @param unloaded A set of module names that should not be loaded.
 * @param defaultPath A set of default paths to search for external modules if
 *                    no paths are found in [sharedPreferences].
 * @param lazyPrefs [SharedPreferences] containing information about which commands
 *                  should trigger the lazy loading of specific modules.
 * @param forceLazy If true, modules normally configured for lazy loading will be loaded eagerly,
 *                  and modules not configured for lazy loading will be skipped.
 *                  If false, modules will be loaded based on their individual lazy load settings.
 */
fun Globals.loadExternalModules(
    service: InlineService,
    sharedPreferences: SharedPreferences,
    unloaded: Set<String>,
    defaultPath: Set<String>,
    lazyPrefs: SharedPreferences,
    forceLazy: Boolean = false,
) {
    val paths = sharedPreferences.getStringSet(PATH, defaultPath) ?: return

    paths.filter { it !in unloaded }.forEach { dirPath ->
        File(dirPath).listFiles()?.filter { it.isFile }?.forEach { file ->
            val path = file.absolutePath
            val lazyCommands = lazyPrefs.getStringSet(path, emptySet())!!

            if (forceLazy && lazyCommands.isEmpty()) {
                Log.d(TAG, "Skip loaded module: $path")
            } else {
                loadModuleByStrategy(
                    isLazy = !(lazyCommands.isEmpty() || forceLazy),
                    service = service,
                    lazyCommands = lazyCommands,
                    lazyPrefs = lazyPrefs,
                    path = path,
                    isInternal = false
                ) {
                    file.readScript()
                }
            }
        }
    }
}

/**
 * Loads a module into the Lua environment either eagerly or lazily.
 *
 * This function decides whether to load a module immediately or to set up
 * lazy loading stubs based on the `isLazy` parameter.
 *
 * If `isLazy` is true, it calls [loadLazyStubs] to create placeholder functions
 * for the module's commands. These stubs will trigger the actual loading of the module
 * (by calling [executeModule]) when one of the associated commands is invoked.
 *
 * If `isLazy` is false, it directly calls [executeModule] to load and execute
 * the module script immediately.
 *
 * @param isLazy If true, the module will be loaded lazily; otherwise, it will be loaded eagerly.
 * @param service The [InlineService] instance, providing context and access to resources.
 * @param lazyCommands A set of command names that, when invoked, should trigger the
 *                     lazy loading of this module. This is relevant only if `isLazy` is true.
 * @param lazyPrefs [SharedPreferences] used by the lazy loading mechanism, primarily
 *                  to manage the state of lazy-loaded modules. This is relevant only if `isLazy` is true.
 * @param path The path or identifier of the module being loaded. This is used for logging
 *             and potentially by the module script itself.
 * @param isInternal A boolean flag indicating whether the module is an internal (asset-based)
 *                   module or an external (file-system-based) module.
 * @param scriptProvider A function that, when called, returns the Lua script content as a String.
 *                       This allows for deferred reading of the script file until it's actually needed,
 *                       which is particularly useful for lazy loading.
 */
private fun Globals.loadModuleByStrategy(
    isLazy: Boolean,
    service: InlineService,
    lazyCommands: Set<String>,
    lazyPrefs: SharedPreferences,
    path: String,
    isInternal: Boolean,
    scriptProvider: () -> String,
) {
    when {
        isLazy -> loadLazyStubs(service.allCommands, lazyCommands, lazyPrefs) {
            executeModule(service, scriptProvider(), path, isInternal)
        }

        else -> executeModule(service, scriptProvider(), path, isInternal)
    }
}

/**
 * Reads the content of an [InputStream] as a UTF-8 encoded string.
 * This is an extension function for [InputStream]. It ensures the stream is closed
 * after reading.
 *
 * @return The content of the stream as a [String].
 */
private fun InputStream.readScript(): String = use { it.readBytes().toString(Charsets.UTF_8) }

/**
 * Reads the content of a Lua script file, handling potential Byte Order Marks (BOM).
 *
 * This function reads the entire content of the file into a string.
 * It checks for and skips the UTF-8 BOM (Byte Order Mark) if present at the beginning of the file.
 *
 * @return The content of the script file as a String.
 */
private fun File.readScript(): String = bufferedReader().use { reader ->
    if (reader.markSupported()) {
        reader.mark(1)
        if (reader.read() != 65279) reader.reset()
    }
    reader.readText()
}

/**
 * Executes a Lua script within the global Lua environment.
 *
 * This function loads the provided [script] string, using the given [path] as the chunk name
 * for debugging and error reporting. It then calls the loaded script. If the script returns
 * a function, that function is called with a [Module] instance, providing the script with
 * access to the [InlineService] and information about its own path and origin (internal/external).
 *
 * @param service The [InlineService] instance, passed to the module if it's a function.
 * @param script The Lua script code to execute as a String.
 * @param path The path or name of the script, used as the chunk name when loading.
 * @param isInternal A boolean indicating whether the module is internal (from assets)
 *                   or external (from the file system). This is passed to the [Module] instance.
 */
fun Globals.executeModule(
    service: InlineService,
    script: String,
    path: String,
    isInternal: Boolean,
) {
    val result = load(script, path).call()
    Log.d(TAG, "Loading module: $path")
    if (result.isfunction()) {
        result.call(CoerceJavaToLua.coerce(Module(service, path, isInternal)))
    }
}