package com.wavecat.inline;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wavecat.inline.databinding.ActivityMainBinding;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String LOADER_PREF = "loader_module";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        binding.openAccessibilitySettings.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        binding.requestPermission.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.fromParts("package", getPackageName(), null)));
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        });

        binding.reloadService.setOnClickListener(view -> {
            InlineService service = InlineService.getInstance();

            if (service == null) binding.openAccessibilitySettings.callOnClick();
            else service.createEnvironment();
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        binding.loader.setChecked(preferences.getBoolean(LOADER_PREF, false));
        binding.loader.setOnCheckedChangeListener((view, isChecked) -> {
            if (isChecked) {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle(R.string.attention)
                        .setMessage(R.string.unknown_sources)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        })
                        .show();

                new File(getExternalFilesDirs(null)[0].getAbsolutePath() + "/modules").mkdirs();
            }

            preferences.edit()
                    .putBoolean(LOADER_PREF, isChecked)
                    .apply();

            binding.reloadService.callOnClick();
        });
    }
}