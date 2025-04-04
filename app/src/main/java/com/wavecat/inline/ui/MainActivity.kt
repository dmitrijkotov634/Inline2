package com.wavecat.inline.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wavecat.inline.R
import com.wavecat.inline.databinding.ActivityMainBinding
import com.wavecat.inline.preferences.PreferencesDialog
import com.wavecat.inline.service.InlineService.Companion.instance
import com.wavecat.inline.service.InlineService.Companion.requireService

@Suppress("unused")
class MainActivity : AppCompatActivity() {

    private val model by viewModels<MainViewModel>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        model.errorMessage.observe(this) { binding.errorMessage.text = it }

        val adapter = ModulesAdapter() { module ->
            when (module) {
                is ModuleItem.Internal -> {
                    if (module.isLoaded) {
                        model.disableModule(module)
                    } else {
                        model.enableModule(module)
                    }

                    instance?.createEnvironment()
                }

                is ModuleItem.External -> {
                    if (module.isInstalled) {
                        model.removeModule(module)
                    } else {
                        model.downloadModule(module)
                    }
                }
            }
        }

        binding.modules.adapter = adapter

        model.modules.observe(this) { list ->
            adapter.modules = list
            adapter.notifyDataSetChanged()
        }

        model.repositoryUrl.observe(this) {
            binding.repositoryUrl.editText?.setText(it)
        }

        binding.repositoryUrl.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                val url = binding.repositoryUrl.editText?.text.toString()
                model.updateUrl(url)
                true
            } else {
                false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )

        model.loadModules()
    }

    override fun onResume() {
        invalidateOptionsMenu()
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        if (instance == null || instance!!.allPreferences.isEmpty())
            menu.removeItem(R.id.preferences)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.storage_permission -> showExternalStorageSettings()
            R.id.preferences -> showPreferencesDialog()
            R.id.turn_on -> openAccessibilitySettings()
            R.id.reload -> reload()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun openAccessibilitySettings() =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(this)
        }

    private fun reload() = instance?.createEnvironment() ?: openAccessibilitySettings()

    private fun showPreferencesDialog() {
        val items: Array<String?> = requireService().allPreferences.keys.toTypedArray()

        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle(R.string.preferences)
            .setItems(items) { _: DialogInterface?, which: Int ->
                requireService().allPreferences[items[which]]?.let {
                    PreferencesDialog(this@MainActivity).create(items[which]!!, it)
                }
            }
            .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                dialog.cancel()
            }
            .show()
    }

    private fun showExternalStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.fromParts("package", packageName, null)
                )
            )
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1
            )
        }
    }
}