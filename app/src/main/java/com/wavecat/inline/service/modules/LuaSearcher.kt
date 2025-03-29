package com.wavecat.inline.service.modules

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.VarArgFunction

class LuaSearcher(private val globals: Globals) : VarArgFunction() {
    override fun invoke(args: Varargs): Varargs {
        val name = LIBRARIES[args.checkjstring(1)] ?: args.checkjstring(1)
        val classname = PackageLib.toClassname(name)
        return try {
            val clazz = Class.forName(classname)
            val instance = clazz.getDeclaredConstructor().newInstance() as LuaValue
            if (instance.isfunction()) (instance as LuaFunction).initupvalue1(globals)
            varargsOf(instance, globals)
        } catch (e: ClassNotFoundException) {
            valueOf("\n\tno class '$classname'")
        } catch (e: Exception) {
            valueOf("\n\tjava load failed on '$classname', $e")
        }
    }

    companion object {
        private val LIBRARIES = mapOf(
            "http" to "com.wavecat.inline.libs.http",
            "json" to "com.wavecat.inline.libs.json",
            "iutf8" to "com.wavecat.inline.libs.utf8",
            "utils" to "com.wavecat.inline.libs.utils",
            "menu" to "com.wavecat.inline.libs.menu",
            "colorama" to "com.wavecat.inline.libs.colorama"
        )
    }
}