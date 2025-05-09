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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wavecat.inline.R
import com.wavecat.inline.databinding.ActivityMainBinding
import com.wavecat.inline.preferences.PreferencesDialog
import com.wavecat.inline.service.InlineService.Companion.instance
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.service.modules.DEFAULT_ASSETS_PATH
import kotlinx.coroutines.launch
import java.io.File

@Suppress("unused")
class MainActivity : AppCompatActivity() {

    private val model by viewModels<MainViewModel> {
        MainModelFactory(
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this),
            modulesPath = File(application.getExternalFilesDirs(null)[0].absolutePath + "/modules").apply { mkdirs() },
            internalModules = assets.list(DEFAULT_ASSETS_PATH)!!.toList()
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
            setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.divider, null)!!)
        }

        binding.modules.addItemDecoration(itemDecoration)

        val adapter = ModulesAdapter() { module ->
            when (module) {
                is ModuleItem.Internal -> {
                    if (module.isLoaded) {
                        model.disableModule(module)
                    } else {
                        model.enableModule(module)
                    }

                    model.loadModulesIfEfficient()
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.errorMessage.collect {
                        binding.errorMessage.text = it
                    }
                }

                launch {
                    model.repositoryUrl.collect {
                        binding.repositoryUrl.editText?.setText(it)
                    }
                }

                launch {
                    model.modules.collect { list ->
                        adapter.modules = list
                        adapter.notifyDataSetChanged()
                    }
                }
            }
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

        model.loadModulesList()
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

    private fun reload() = model.reload() ?: openAccessibilitySettings()

    private fun showPreferencesDialog() {
        requireService().apply {
            model.loadAll()

            val items: Array<String?> = allPreferences.keys.toTypedArray()

            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.preferences)
                .setItems(items) { _: DialogInterface?, which: Int ->
                    requireService().allPreferences[items[which]]?.let {
                        PreferencesDialog(this@MainActivity) { invalidateOptionsMenu() }
                            .create(items[which]!!, it)
                    }
                }
                .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                }
                .show()
        }
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

    override fun onPause() {
        model.onPause()
        super.onPause()
    }
}