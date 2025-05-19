package com.example.paydaylay.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.paydaylay.R;
import com.example.paydaylay.utils.BudgetAlarmScheduler;
import com.example.paydaylay.utils.LocaleHelper;
import com.example.paydaylay.utils.NotificationUtils;
import com.example.paydaylay.utils.ThemeUtils;

public class SettingsActivity extends BaseActivity {

    private RadioGroup radioGroupTheme, radioGroupLanguage;
    private RadioButton radioSystemTheme, radioLightTheme, radioDarkTheme;
    private RadioButton radioEnglish, radioPolish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }

        // Initialize theme selection controls
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        radioSystemTheme = findViewById(R.id.radioSystemTheme);
        radioLightTheme = findViewById(R.id.radioLightTheme);
        radioDarkTheme = findViewById(R.id.radioDarkTheme);

        // Initialize language selection controls
        radioGroupLanguage = findViewById(R.id.radioGroupLanguage);
        radioEnglish = findViewById(R.id.radioEnglish);
        radioPolish = findViewById(R.id.radioPolish);

        // Set the current theme selection
        setInitialThemeSelection();

        // Set the current language selection
        setInitialLanguageSelection();
        setupNotificationSettings();

        // Set listener for theme changes
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSystemTheme) {
                ThemeUtils.saveThemeMode(this, ThemeUtils.MODE_SYSTEM);
                ThemeUtils.applyTheme(this);
            } else if (checkedId == R.id.radioLightTheme) {
                ThemeUtils.saveThemeMode(this, ThemeUtils.MODE_LIGHT);
                ThemeUtils.applyTheme(this);
            } else if (checkedId == R.id.radioDarkTheme) {
                ThemeUtils.saveThemeMode(this, ThemeUtils.MODE_DARK);
                ThemeUtils.applyTheme(this);
            }
        });

        radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String languageCode;
            if (checkedId == R.id.radioPolish) {
                languageCode = "pl";
            } else {
                languageCode = "en";
            }

            // Check if language actually changed
            if (!languageCode.equals(LocaleHelper.getLanguage(this))) {
                LocaleHelper.setLocale(this, languageCode);
                // The app restart is now handled in the LocaleHelper
            }
        });
    }

    private void setInitialThemeSelection() {
        int currentThemeMode = ThemeUtils.getThemeMode(this);
        switch (currentThemeMode) {
            case ThemeUtils.MODE_SYSTEM:
                radioSystemTheme.setChecked(true);
                break;
            case ThemeUtils.MODE_LIGHT:
                radioLightTheme.setChecked(true);
                break;
            case ThemeUtils.MODE_DARK:
                radioDarkTheme.setChecked(true);
                break;
        }
    }

    private void setInitialLanguageSelection() {
        String currentLanguage = LocaleHelper.getLanguage(this);
        if ("pl".equals(currentLanguage)) {
            radioPolish.setChecked(true);
        } else {
            radioEnglish.setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // Add to SettingsActivity.java
    private Switch switchNotifications;
    private Button buttonCustomizeNotifications;

    private void setupNotificationSettings() {
        switchNotifications = findViewById(R.id.switchNotifications);
        buttonCustomizeNotifications = findViewById(R.id.buttonCustomizeNotifications);

        // Set initial state
        switchNotifications.setChecked(NotificationUtils.areNotificationsEnabled(this));

        // Set listener
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NotificationUtils.setNotificationsEnabled(this, isChecked);
            buttonCustomizeNotifications.setEnabled(isChecked);

            // Schedule or cancel budget check alarms
            if (isChecked) {
                BudgetAlarmScheduler.scheduleBudgetCheck(this);
            } else {
                BudgetAlarmScheduler.cancelBudgetCheck(this);
            }
        });

        // Setup customize button
        buttonCustomizeNotifications.setEnabled(switchNotifications.isChecked());
        buttonCustomizeNotifications.setOnClickListener(v -> {
            showNotificationCustomizationDialog();
        });
    }

    private void showNotificationCustomizationDialog() {
        // Create dialog for notification sound and vibration settings
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View customView = getLayoutInflater().inflate(R.layout.dialog_notification_settings, null);

        Switch switchVibration = customView.findViewById(R.id.switchVibration);
        Button buttonSelectSound = customView.findViewById(R.id.buttonSelectSound);

        // Set initial state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        switchVibration.setChecked(prefs.getBoolean("notification_vibration", true));

        // Setup sound selection
        buttonSelectSound.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_notification_sound));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Uri.parse(prefs.getString("custom_notification_sound",
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
            startActivityForResult(intent, 123);
        });

        builder.setTitle(R.string.customize_notifications)
                .setView(customView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    // Save preferences
                    prefs.edit()
                            .putBoolean("notification_vibration", switchVibration.isChecked())
                            .apply();

                    // Recreate notification channels for Android O+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationUtils.createNotificationChannels(this);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit()
                        .putString("custom_notification_sound", uri.toString())
                        .apply();

                // Recreate notification channels for Android O+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationUtils.createNotificationChannels(this);
                }
            }
        }
    }
}