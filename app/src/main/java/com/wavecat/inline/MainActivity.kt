package com.wavecat.inline

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wavecat.inline.service.InlineService.Companion.instance
import com.wavecat.inline.service.InlineService.Companion.requireService
import com.wavecat.inline.databinding.ActivityMainBinding
import com.wavecat.inline.preferences.PreferencesDialog
import com.wavecat.inline.service.InlineService
import java.io.File
import java.io.IOException

@Suppress("unused")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.openAccessibilitySettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.reloadService.setOnClickListener {
            val service = instance
            service?.createEnvironment() ?: binding.openAccessibilitySettings.callOnClick()
        }

        val unloaded: MutableSet<String> = HashSet(
            preferences.getStringSet(InlineService.UNLOADED, hashSetOf())!!
        )

        binding.reloadService.setOnLongClickListener {
            try {
                val internalModules = resources.assets.list(InlineService.DEFAULT_ASSETS_PATH)
                val enabled = BooleanArray(internalModules!!.size)

                for (index in internalModules.indices) enabled[index] =
                    !unloaded.contains(internalModules[index])

                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.internal_modules)
                    .setMultiChoiceItems(
                        internalModules,
                        enabled
                    ) { _: DialogInterface?, index: Int, value: Boolean ->
                        if (!value)
                            unloaded.add(internalModules[index])
                        else
                            unloaded.remove(internalModules[index])
                    }
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        preferences.edit()
                            .putStringSet(InlineService.UNLOADED, unloaded)
                            .apply()
                        binding.reloadService.callOnClick()
                    }
                    .show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            true
        }

        binding.loader.isChecked = preferences.getBoolean(LOADER_PREF, false)
        binding.loader.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.attention)
                    .setMessage(R.string.unknown_sources)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> }
                    .show()

                File(getExternalFilesDirs(null)[0].absolutePath + "/modules").mkdirs()
            }
            preferences.edit()
                .putBoolean(LOADER_PREF, isChecked)
                .apply()
            binding.reloadService.callOnClick()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
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
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showPreferencesDialog() {
        val items: Array<String?> = requireService().allPreferences.keys.toTypedArray()

        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle(R.string.preferences)
            .setItems(items) { _: DialogInterface?, which: Int ->
                requireService().allPreferences[items[which]]?.let {
                    PreferencesDialog(this@MainActivity).create(
                        items[which]!!,
                        it
                    )
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

    companion object {
        private const val LOADER_PREF = "loader_module"
    }
}