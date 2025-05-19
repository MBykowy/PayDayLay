package com.example.paydaylay.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.paydaylay.utils.AlarmPermissionHelper;

/**
 * Odbiornik odpowiedzialny za obsługę aktualizacji widżetów budżetowych.
 * Obsługuje zdarzenia aktualizacji i planuje kolejne aktualizacje.
 */
public class WidgetUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "WidgetUpdateReceiver";
    public static final String ACTION_UPDATE_WIDGETS = "com.example.paydaylay.ACTION_UPDATE_WIDGETS";

    /**
     * Metoda wywoływana po odebraniu zdarzenia.
     *
     * @param context Kontekst aplikacji.
     * @param intent  Intencja zawierająca szczegóły zdarzenia.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Aktualizuj widgety
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName provider = new ComponentName(context, BudgetWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(provider);

            if (appWidgetIds != null && appWidgetIds.length > 0) {
                // Użyj jawnego intentu z określoną klasą
                Intent updateIntent = new Intent(context, BudgetWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                context.sendBroadcast(updateIntent);
            }

            // Planuj kolejne aktualizacje
            try {
                if (AlarmPermissionHelper.checkAlarmPermission(context)) {
                    WidgetUpdateScheduler.scheduleUpdates(context);
                } else {
                    Log.w(TAG, "Brak uprawnień do dokładnych alarmów");
                    // Używamy zwykłego alarmu zamiast dokładnego
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager != null) {
                        Intent alarmIntent = new Intent(context, BudgetWidgetProvider.class);
                        alarmIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 3600000, pendingIntent);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Błąd podczas planowania aktualizacji: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas odbierania: " + e.getMessage());
        }
    }
}