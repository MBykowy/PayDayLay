package com.example.paydaylay.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class WidgetUpdateScheduler {
    private static final String TAG = "WidgetUpdateScheduler";
    private static final long UPDATE_INTERVAL = 60 * 60 * 1000; // 1 godzina
    private static final int REQUEST_CODE = 1001;

    public static void scheduleUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BudgetWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Ustaw alarm na aktualizację co godzinę
        long intervalMillis = 60 * 60 * 1000; // 1 godzina
        long triggerTime = System.currentTimeMillis() + intervalMillis;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Sprawdź uprawnienie dla Androida 12+
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    // Brak uprawnień - używamy zwykłego alarmu
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    Log.w(TAG, "Brak uprawnień do dokładnych alarmów");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Zaplanowano aktualizację widgetu");
        } catch (SecurityException e) {
            Log.e(TAG, "Błąd uprawnień: " + e.getMessage());
            // Używamy zwykłego alarmu jako fallback
            alarmManager.set(AlarmManager.RTC, triggerTime, pendingIntent);
        } catch (Exception e) {
            Log.e(TAG, "Inny błąd: " + e.getMessage());
        }
    }

    public static void cancelUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, WidgetUpdateReceiver.class);
        intent.setAction(WidgetUpdateReceiver.ACTION_UPDATE_WIDGETS);

        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                    PendingIntent.FLAG_NO_CREATE);
        }

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Anulowano zaplanowane aktualizacje widgetów");
        }
    }
}