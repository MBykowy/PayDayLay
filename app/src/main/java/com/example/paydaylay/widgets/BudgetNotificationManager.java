package com.example.paydaylay.widgets;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.MainActivity;
import com.example.paydaylay.models.Budget;

import java.text.NumberFormat;
import java.util.Calendar;

public class BudgetNotificationManager {
    private static final String CHANNEL_ID = "budget_alerts";
    private static final String PREFS_NAME = "BudgetNotificationPrefs";
    private static final String LAST_NOTIFICATION_PREFIX = "last_notification_";
    private static final int NOTIFICATION_ID_PREFIX = 7000;

    private final Context context;
    private final SharedPreferences prefs;

    public BudgetNotificationManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Budget Alerts", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Powiadomienia o przekroczeniu budżetu");

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void checkAndShowNotification(Budget budget, int appWidgetId) {
        if (budget == null || budget.getLimit() <= 0) return;

        double percentSpent = budget.getSpent() / budget.getLimit() * 100;
        if (percentSpent >= 100 && shouldShowNotification(budget.getId())) {
            showBudgetOverLimitNotification(budget, appWidgetId);
        }
    }

    private boolean shouldShowNotification(String budgetId) {
        long lastNotificationTime = prefs.getLong(LAST_NOTIFICATION_PREFIX + budgetId, 0);
        if (lastNotificationTime == 0) return true;

        Calendar lastNotification = Calendar.getInstance();
        lastNotification.setTimeInMillis(lastNotificationTime);
        Calendar now = Calendar.getInstance();

        return lastNotification.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR) ||
                lastNotification.get(Calendar.YEAR) != now.get(Calendar.YEAR);
    }

    private void showBudgetOverLimitNotification(Budget budget, int appWidgetId) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        String title = "Przekroczony budżet";
        String message = "Budżet został przekroczony! Wydano " +
                currencyFormat.format(budget.getSpent()) +
                " z limitu " +
                currencyFormat.format(budget.getLimit()) + ".";

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_BUDGET_TAB", true);
        intent.putExtra("BUDGET_ID", budget.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Zmień na własną ikonę
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID_PREFIX + appWidgetId, builder.build());

            prefs.edit()
                    .putLong(LAST_NOTIFICATION_PREFIX + budget.getId(), System.currentTimeMillis())
                    .apply();
        } catch (SecurityException e) {
            // Brak uprawnień do powiadomień
        }
    }
}