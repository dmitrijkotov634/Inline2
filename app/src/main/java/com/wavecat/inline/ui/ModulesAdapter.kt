package com.wavecat.inline.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wavecat.inline.R
import com.wavecat.inline.databinding.ModuleItemBinding
import java.util.Locale

class ModulesAdapter(
    var modules: List<ModuleItem> = listOf(),
    private val onClick: (ModuleItem) -> Unit,
) : RecyclerView.Adapter<ModulesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = ModuleItemBinding.bind(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.module_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = modules[position]

        holder.binding.apply {
            moduleActionPositive.isEnabled = true
            moduleActionNegative.isEnabled = true

            when (item) {
                is ModuleItem.External -> {
                    moduleName.text = processFilename(item.name)
                    moduleDescription.text = item.description

                    val text = root.context
                        .getString(if (item.isInstalled) R.string.remove else R.string.download)

                    moduleActionNegative.visibility = if (item.isInstalled) View.VISIBLE else View.INVISIBLE
                    moduleActionPositive.visibility = if (!item.isInstalled) View.VISIBLE else View.INVISIBLE

                    moduleActionNegative.text = text
                    moduleActionPositive.text = text
                }

                is ModuleItem.Internal -> {
                    moduleName.text = processFilename(item.name)
                    moduleDescription.text = item.description

                    val text = root.context
                        .getString(if (item.isLoaded) R.string.disable else R.string.enable)

                    moduleActionNegative.visibility = if (item.isLoaded) View.VISIBLE else View.INVISIBLE
                    moduleActionPositive.visibility = if (!item.isLoaded) View.VISIBLE else View.INVISIBLE

                    moduleActionNegative.text = text
                    moduleActionPositive.text = text
                }
            }

            moduleActionPositive.setOnClickListener {
                moduleActionPositive.isEnabled = false
                onClick(item)
            }

            moduleActionNegative.setOnClickListener {
                moduleActionNegative.isEnabled = false
                onClick(item)
            }
        }
    }

    private fun processFilename(name: String) = name
        .removeSuffix(".lua")
        .replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    override fun getItemCount() = modules.size
}