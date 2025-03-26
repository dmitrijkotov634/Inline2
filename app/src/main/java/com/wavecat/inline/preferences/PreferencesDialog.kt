package com.wavecat.inline.preferences

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wavecat.inline.databinding.PreferencesDialogBinding
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.extensions.zeroArgFunction
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.utils.dp
import org.luaj.vm2.lib.VarArgFunction.NIL
import org.luaj.vm2.lib.jse.CoerceJavaToLua

class PreferencesDialog(private val context: Context) {
    private var dialog: Dialog? = null

    private val builder = Builder(context = context).apply {
        set("cancel", zeroArgFunction {
            dialog?.cancel()
            NIL
        })

        set("create", varArgFunction { args ->
            val preferences = LinkedHashSet<PreferencesItem>()
            var sharedPreferences = requireService().defaultSharedPreferences

            for (n in 3 until args.narg() + 1) {
                val value = args.arg(n)

                if (value.isuserdata(SharedPreferences::class.java)) {
                    sharedPreferences = value.touserdata(SharedPreferences::class.java) as SharedPreferences
                    continue
                }

                preferences.add(PreferencesItem(sharedPreferences, value))
            }

            create(args.checkjstring(2), preferences)
            NIL
        })

        set("paddingBottom", 8)
        set("paddingTop", 8)
        set("paddingLeft", 0)
        set("paddingRight", 0)
    }

    fun create(title: String, preferences: HashSet<PreferencesItem>) {
        val binding = PreferencesDialogBinding.inflate(LayoutInflater.from(context))

        for (preference in preferences) {
            val preferencesList =
                preference.builder.call(builder, CoerceJavaToLua.coerce(this)).checktable()

            preferencesList.forEach { _, value ->
                val view = castPreference(context, value).getView(preference.sharedPreferences)

                if (view.parent != null)
                    (view.parent as ViewGroup).removeView(view)

                view.setPadding(
                    /* left = */ builder.get("paddingLeft").optint(0).dp,
                    /* top = */ builder.get("paddingTop").optint(8).dp,
                    /* right = */ builder.get("paddingRight").optint(0).dp,
                    /* bottom = */ builder.get("paddingBottom").optint(8).dp,
                )

                binding.preferences.addView(view)
            }
        }

        dialog = MaterialAlertDialogBuilder(context).apply {
            setTitle(title)
            setView(binding.root)
        }
            .show()
    }
}
