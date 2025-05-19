package com.example.paydaylay.utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.example.paydaylay.R;

/**
 * Klasa pomocnicza do zarządzania uprawnieniami związanymi z alarmami.
 * Obsługuje sprawdzanie i żądanie uprawnień do planowania dokładnych alarmów.
 */
public class AlarmPermissionHelper {
    private static final String TAG = "AlarmPermissionHelper";

    /**
     * Sprawdza, czy aplikacja ma uprawnienia do planowania dokładnych alarmów.
     *
     * @param context Kontekst aplikacji.
     * @return True, jeśli uprawnienia są przyznane, false w przeciwnym razie.
     */
    public static boolean checkAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Na starszych wersjach Androida uprawnienie jest automatycznie przyznane
    }

    /**
     * Wyświetla dialog z prośbą o przyznanie uprawnień do planowania dokładnych alarmów.
     *
     * @param activity Aktywność, w której ma zostać wyświetlony dialog.
     */
    public static void requestAlarmPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.alarm_permission_title)
                    .setMessage(R.string.alarm_permission_message)
                    .setPositiveButton(R.string.settings, (dialog, which) -> {
                        try {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Nie można otworzyć ustawień alarmów: " + e.getMessage());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }
}