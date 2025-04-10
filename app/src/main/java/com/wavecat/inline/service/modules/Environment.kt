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


fun Globals.loadModules(
    service: InlineService,
    sharedPreferences: SharedPreferences,
    defaultPath: Set<String>,
) {
    val unloaded = sharedPreferences.getStringSet(UNLOADED, defaultUnloaded) ?: defaultUnloaded
    val lazyPrefs = service.getSharedPreferences(LAZYLOAD)

    loadInternalModules(service, lazyPrefs, unloaded)
    loadExternalModules(service, sharedPreferences, unloaded, defaultPath, lazyPrefs)
}

fun Globals.loadInternalModules(
    service: InlineService,
    lazyPrefs: SharedPreferences,
    unloaded: Set<String>,
) {
    service.assets.list(DEFAULT_ASSETS_PATH)?.forEach { fileName ->
        if (fileName !in unloaded) {
            val path = "$DEFAULT_ASSETS_PATH/$fileName"
            val lazyCommands = lazyPrefs.getStringSet(fileName, emptySet())!!

            loadModuleByStrategy(
                isLazy = lazyCommands.isNotEmpty(),
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

fun Globals.loadExternalModules(
    service: InlineService,
    sharedPreferences: SharedPreferences,
    unloaded: Set<String>,
    defaultPath: Set<String>,
    lazyPrefs: SharedPreferences,
) {
    val paths = sharedPreferences.getStringSet(PATH, defaultPath) ?: return

    paths.filter { it !in unloaded }.forEach { dirPath ->
        File(dirPath).listFiles()?.filter { it.isFile }?.forEach { file ->
            val path = file.absolutePath
            val lazyCommands = lazyPrefs.getStringSet(path, emptySet())!!

            loadModuleByStrategy(
                isLazy = lazyCommands.isNotEmpty(),
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

private fun InputStream.readScript(): String = use { it.readBytes().toString(Charsets.UTF_8) }

private fun File.readScript(): String = bufferedReader().use { reader ->
    if (reader.markSupported()) {
        reader.mark(1)
        if (reader.read() != 65279) reader.reset()
    }
    reader.readText()
}

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