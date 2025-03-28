package com.wavecat.inline.service

import android.content.SharedPreferences
import android.util.Log
import com.wavecat.inline.service.InlineService.Companion.DEFAULT_ASSETS_PATH
import com.wavecat.inline.service.InlineService.Companion.PATH
import com.wavecat.inline.service.InlineService.Companion.TAG
import com.wavecat.inline.service.InlineService.Companion.UNLOADED
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

fun Globals.loadModules(
    service: InlineService,
    sharedPreferences: SharedPreferences,
    defaultPath: HashSet<String>,
) {
    val unloaded = sharedPreferences.getStringSet(UNLOADED, HashSet())!!

    service.assets.list(DEFAULT_ASSETS_PATH)?.forEach { fileName ->
        if (fileName !in unloaded) {
            val path = "$DEFAULT_ASSETS_PATH/$fileName"

            val buffer = service.assets.open(path).use { it.readBytes() }
            val result = load(String(buffer), path).call()

            Log.d(TAG, "Loading internal module: $path")

            if (result.isfunction())
                result.call(
                    CoerceJavaToLua.coerce(
                        Module(
                            service = service,
                            filepath = path,
                            isInternal = true
                        )
                    )
                )
        }
    }

    sharedPreferences.getStringSet(PATH, defaultPath)?.forEach { path ->
        if (path !in unloaded) {
            File(path).listFiles()?.filter { it.isFile }?.forEach { file ->
                val reader = BufferedReader(FileReader(file)).apply { mark(1) }

                if (reader.read() != 65279)
                    reader.reset()

                val result = load(reader, file.absolutePath).call()

                Log.d(TAG, "Loading module: ${file.path}")

                if (result.isfunction())
                    result.call(
                        CoerceJavaToLua.coerce(
                            Module(
                                service = service,
                                filepath = file.path,
                                isInternal = false
                            )
                        )
                    )
            }
        }
    }
}

fun Globals.setupSearcher() {
    get("package").get("searchers").set(3, Searcher(this))
}

class Searcher(val globals: Globals) : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs {
        val name = libraries[args.checkjstring(1)] ?: args.checkjstring(1)
        val classname = PackageLib.toClassname(name)
        try {
            val c = Class.forName(classname)
            val v = c.getDeclaredConstructor().newInstance() as LuaValue
            if (v.isfunction())
                (v as LuaFunction?)!!.initupvalue1(globals)
            return LuaValue.varargsOf(v, globals)
        } catch (cnfe: ClassNotFoundException) {
            return LuaValue.valueOf("\n\tno class '$classname'")
        } catch (e: java.lang.Exception) {
            return LuaValue.valueOf("\n\tjava load failed on '$classname', $e")
        }
    }

    private val libraries = mapOf(
        "http" to "com.wavecat.inline.libs.http",
        "json" to "com.wavecat.inline.libs.json",
        "iutf8" to "com.wavecat.inline.libs.utf8",
        "utils" to "com.wavecat.inline.libs.utils",
        "menu" to "com.wavecat.inline.libs.menu",
        "colorama" to "com.wavecat.inline.libs.colorama",
        "utils" to "com.wavecat.inline.libs.utils"
    )
}

