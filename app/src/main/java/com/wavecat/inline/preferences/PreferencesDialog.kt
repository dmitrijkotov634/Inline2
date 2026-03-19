package com.wavecat.inline.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wavecat.inline.databinding.PreferencesDialogBinding
import com.wavecat.inline.extensions.forEach
import com.wavecat.inline.extensions.varArgFunction
import com.wavecat.inline.extensions.zeroArgFunction
import com.wavecat.inline.service.InlineService.Companion.requireService
import org.luaj.vm2.lib.VarArgFunction.NIL
import org.luaj.vm2.lib.jse.CoerceJavaToLua

/**
 * A bottom sheet dialog for managing module preferences.
 *
 * @param context The context in which the dialog will be displayed.
 * @param onCancelListener A lambda function to be invoked when the dialog is canceled.
 */
class PreferencesDialog(
    private val context: Context,
    private val onCancelListener: () -> Unit,
) {
    private var dialog: BottomSheetDialog? = null

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
    }

    fun create(title: String, preferences: HashSet<PreferencesItem>) {
        val binding = PreferencesDialogBinding.inflate(LayoutInflater.from(context))

        for (preference in preferences) {
            try {
                val preferencesList =
                    preference.builder.call(builder, CoerceJavaToLua.coerce(this)).checktable()

                preferencesList.forEach { _, value ->
                    val view = castPreference(context, value).getView(preference.sharedPreferences) {}

                    if (view.parent != null)
                        (view.parent as ViewGroup).removeView(view)

                    binding.preferences.addView(view)
                }
            } catch (e: Exception) {
                Log.e("PreferencesDialog", "Error building preferences for $title", e)
                Toast.makeText(context, "Error in preferences: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        dialog = BottomSheetDialog(context).apply {
            setContentView(binding.root)
            setOnCancelListener { onCancelListener() }

            setOnShowListener {
                val bottomSheet = findViewById<FrameLayout>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

                bottomSheet?.let {
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    behavior.skipCollapsed = true
                    behavior.isDraggable = true
                }
            }

            behavior.isFitToContents = true

            show()
        }
    }
}
