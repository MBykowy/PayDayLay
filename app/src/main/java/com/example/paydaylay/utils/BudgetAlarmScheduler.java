package com.example.paydaylay.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.paydaylay.services.BudgetCheckService;

public class BudgetAlarmScheduler {

    private static final int REQUEST_CODE = 1001;
    private static final long CHECK_INTERVAL = AlarmManager.INTERVAL_HOUR; // Check every hour

    public static void scheduleBudgetCheck(Context context) {
        // Skip if notifications are disabled
        if (!NotificationUtils.areNotificationsEnabled(context)) {
            cancelBudgetCheck(context);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BudgetCheckService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Cancel any existing alarms
        alarmManager.cancel(pendingIntent);

        // Schedule new alarm
        long triggerTime = System.currentTimeMillis() + CHECK_INTERVAL;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent);
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent);
        }

        // Also schedule repeating alarm as backup
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                CHECK_INTERVAL,
                pendingIntent);
    }

    public static void cancelBudgetCheck(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BudgetCheckService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
    }
}