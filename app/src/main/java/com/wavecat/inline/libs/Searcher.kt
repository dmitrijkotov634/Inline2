package com.wavecat.inline.libs

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.VarArgFunction


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