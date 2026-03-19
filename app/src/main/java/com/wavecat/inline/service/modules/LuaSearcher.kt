package com.wavecat.inline.service.modules

import com.wavecat.inline.extensions.oneArgFunction
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.JavaClass

/**
 * A Lua searcher that attempts to load Lua modules as Java classes.
 *
 * This class is used by the `require` function in Lua to find and load modules.
 * It first checks a predefined map of library names to fully qualified class names.
 * If the module name is not found in the map, it attempts to load the module
 * as a Java class using the module name as the class name.
 *
 * If the class is found and successfully instantiated, and it is a LuaFunction,
 * its first upvalue is initialized with the global environment.
 *
 * @property globals The global Lua environment.
 */
class LuaSearcher(private val globals: Globals) : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs {
        val name = LIBRARIES[args.checkjstring(1)] ?: args.checkjstring(1)
        val classname = PackageLib.toClassname(name)
        return try {
            val clazz = Class.forName(classname)
            val instance = clazz.getDeclaredConstructor().newInstance()
            if (instance is LuaValue) {
                if (instance is LuaFunction) instance.initupvalue1(globals)
                varargsOf(instance, globals)
            } else {
                bindJavaClass(clazz, name)
            }
        } catch (_: ClassNotFoundException) {
            tryBindJavaClass(name) ?: valueOf("\n\tno class '$classname'")
        } catch (e: Exception) {
            tryBindJavaClass(name) ?: valueOf("\n\tjava load failed on '$classname', $e")
        }
    }

    private fun tryBindJavaClass(name: String): Varargs? =
        try {
            bindJavaClass(Class.forName(name), name)
        } catch (_: ClassNotFoundException) {
            null
        }

    private fun bindJavaClass(clazz: Class<*>, name: String): Varargs {
        val loader = oneArgFunction { JavaClass.forClass(clazz) }
        return varargsOf(loader, valueOf(name))
    }

    companion object {
        /**
         * Base package for custom Lua libraries.
         */
        private const val PACKAGE = "com.wavecat.inline.libs"

        /**
         * A map of library names to their corresponding package names.
         * This allows Lua scripts to `require` libraries using short names
         * (e.g., `require "http"`) instead of fully qualified names.
         */
        private val LIBRARIES = mapOf(
            "http" to "$PACKAGE.http",
            "json" to "$PACKAGE.json",
            "iutf8" to "$PACKAGE.utf8",
            "utils" to "$PACKAGE.utils",
            "menu" to "$PACKAGE.menu",
            "colorama" to "$PACKAGE.colorama",
            "windows" to "$PACKAGE.windows"
        )
    }
}