package com.wavecat.inline.service.modules

import android.content.SharedPreferences
import android.util.Log
import com.wavecat.inline.service.InlineService
import com.wavecat.inline.service.InlineService.Companion.PATH
import com.wavecat.inline.service.InlineService.Companion.TAG
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.File

const val DEFAULT_ASSETS_PATH: String = "modules"
const val UNLOADED: String = "unloaded"

val defaultUnloaded = setOf("loader.lua", "test.lua")

fun Globals.loadModules(
    service: InlineService,
    sharedPreferences: SharedPreferences,
    defaultPath: Set<String>,
) {
    val unloaded = sharedPreferences.getStringSet(UNLOADED, defaultUnloaded) ?: defaultUnloaded
    loadInternalModules(service, unloaded)
    loadExternalModules(service, sharedPreferences, unloaded, defaultPath)
}

fun Globals.loadInternalModules(
    service: InlineService,
    unloaded: Set<String>,
) {
    service.assets.list(DEFAULT_ASSETS_PATH)?.forEach { fileName ->
        if (fileName !in unloaded) {
            val path = "$DEFAULT_ASSETS_PATH/$fileName"
            service.assets.open(path).use { inputStream ->
                val buffer = inputStream.readBytes()
                executeModule(service, String(buffer), path, isInternal = true)
            }
        }
    }
}

fun Globals.loadExternalModules(
    service: InlineService,
    sharedPreferences: SharedPreferences,
    unloaded: Set<String>,
    defaultPath: Set<String>,
) {
    sharedPreferences.getStringSet(PATH, defaultPath)?.forEach { path ->
        if (path !in unloaded) {
            File(path).listFiles()?.filter { it.isFile }?.forEach { file ->
                file.bufferedReader().use { reader ->
                    if (reader.markSupported()) {
                        reader.mark(1)
                        if (reader.read() != 65279) reader.reset()
                    }

                    executeModule(
                        service = service,
                        script = reader.readText(),
                        path = file.absolutePath,
                        isInternal = false
                    )
                }
            }
        }
    }
}

fun Globals.executeModule(
    service: InlineService,
    script: String,
    path: String,
    isInternal: Boolean,
) {
    val result = load(script, path).call()
    Log.d(TAG, "Loading module: $path")
    if (result.isfunction())
        result.call(CoerceJavaToLua.coerce(Module(service, path, isInternal)))
}

