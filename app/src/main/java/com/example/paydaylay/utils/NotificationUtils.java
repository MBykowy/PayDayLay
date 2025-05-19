package com.example.paydaylay.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.MainActivity;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Klasa pomocnicza do zarządzania powiadomieniami w aplikacji.
 * Obsługuje tworzenie kanałów powiadomień, wyświetlanie powiadomień o budżetach oraz zarządzanie preferencjami powiadomień.
 */
public class NotificationUtils {

    public static final String CHANNEL_ID_BUDGET = "budget_alerts";
    public static final int NOTIFICATION_ID_BUDGET = 100;
    public static final int NOTIFICATION_ID_BUDGET_WARNING = 101;

    private static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String PREF_CUSTOM_SOUND = "custom_notification_sound";
    private static final String PREF_VIBRATION = "notification_vibration";

    /**
     * Tworzy kanały powiadomień dla urządzeń z Androidem O i nowszym.
     *
     * @param context Kontekst aplikacji.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            NotificationChannel budgetChannel = new NotificationChannel(
                    CHANNEL_ID_BUDGET,
                    context.getString(R.string.budget_alerts),
                    NotificationManager.IMPORTANCE_HIGH);

            budgetChannel.setDescription(context.getString(R.string.budget_alerts_description));

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            Uri soundUri = Uri.parse(prefs.getString(PREF_CUSTOM_SOUND,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()));

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            budgetChannel.setSound(soundUri, audioAttributes);

            if (prefs.getBoolean(PREF_VIBRATION, true)) {
                budgetChannel.enableVibration(true);
                budgetChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            }

            notificationManager.createNotificationChannel(budgetChannel);
        }
    }

    /**
     * Wyświetla powiadomienie o przekroczeniu budżetu.
     *
     * @param context         Kontekst aplikacji.
     * @param budget          Obiekt budżetu.
     * @param currentSpending Aktualne wydatki.
     * @param categoryMap     Mapa kategorii przypisanych do budżetów.
     */
    public static void showBudgetAlertNotification(Context context, Budget budget, double currentSpending,
                                                   Map<String, Category> categoryMap) {
        if (!areNotificationsEnabled(context)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("fragment", "dashboard");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String categoryName = context.getString(R.string.overall_budget);
        if (budget.getCategoryId() != null) {
            Category category = categoryMap.get(budget.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        String periodText = getPeriodText(context, budget);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
                .setSmallIcon(R.drawable.ic_budget_alert)
                .setContentTitle(context.getString(R.string.budget_alert_title))
                .setContentText(context.getString(R.string.budget_alert_text,
                        categoryName, String.format(Locale.getDefault(), "%.2f", currentSpending),
                        String.format(Locale.getDefault(), "%.2f", budget.getLimit()),
                        context.getString(R.string.currency_symbol)))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.budget_alert_big_text,
                                categoryName, periodText,
                                String.format(Locale.getDefault(), "%.2f", currentSpending),
                                String.format(Locale.getDefault(), "%.2f", budget.getLimit()),
                                context.getString(R.string.currency_symbol))))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && prefs.getBoolean(PREF_VIBRATION, true)) {
            builder.setVibrate(new long[]{0, 500, 200, 500});
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri soundUri = Uri.parse(prefs.getString(PREF_CUSTOM_SOUND,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()));
            builder.setSound(soundUri);
        }

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID_BUDGET, notification);
    }

    /**
     * Wyświetla powiadomienie ostrzegawcze o budżecie.
     *
     * @param context         Kontekst aplikacji.
     * @param budget          Obiekt budżetu.
     * @param currentSpending Aktualne wydatki.
     * @param categoryMap     Mapa kategorii przypisanych do budżetów.
     */
    public static void showBudgetWarningNotification(Context context, Budget budget,
                                                     double currentSpending,
                                                     Map<String, Category> categoryMap) {
        if (!areNotificationsEnabled(context)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("fragment", "budgets");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String categoryName = context.getString(R.string.overall_budget);
        if (budget.getCategoryId() != null) {
            Category category = categoryMap.get(budget.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        int percentage = (int) ((currentSpending / budget.getLimit()) * 100);

        String periodText = getPeriodText(context, budget);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
                .setSmallIcon(R.drawable.ic_budget_warning)
                .setContentTitle(context.getString(R.string.budget_warning_title))
                .setContentText(context.getString(R.string.budget_warning_text,
                        categoryName, percentage))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.budget_warning_big_text,
                                categoryName, periodText, percentage,
                                String.format(Locale.getDefault(), "%.2f", currentSpending),
                                String.format(Locale.getDefault(), "%.2f", budget.getLimit()),
                                context.getString(R.string.currency_symbol))))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(NOTIFICATION_ID_BUDGET_WARNING, builder.build());
    }

    /**
     * Sprawdza, czy powiadomienia są włączone w preferencjach użytkownika.
     *
     * @param context Kontekst aplikacji.
     * @return True, jeśli powiadomienia są włączone, false w przeciwnym razie.
     */
    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, true);
    }

    /**
     * Ustawia preferencję włączenia/wyłączenia powiadomień.
     *
     * @param context Kontekst aplikacji.
     * @param enabled True, aby włączyć powiadomienia, false, aby je wyłączyć.
     */
    public static void setNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    /**
     * Generuje tekst okresu budżetowego na podstawie typu okresu.
     *
     * @param context Kontekst aplikacji.
     * @param budget  Obiekt budżetu.
     * @return Tekst opisujący okres budżetowy.
     */
    private static String getPeriodText(Context context, Budget budget) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date startDate = new Date(budget.getPeriodStartDate());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(budget.getPeriodStartDate());

        switch (budget.getPeriodType()) {
            case Budget.PERIOD_DAILY:
                return context.getString(R.string.for_day, dateFormat.format(startDate));
            case Budget.PERIOD_WEEKLY:
                calendar.add(Calendar.DAY_OF_YEAR, 6);
                return context.getString(R.string.for_week,
                        dateFormat.format(startDate), dateFormat.format(calendar.getTime()));
            case Budget.PERIOD_MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                return context.getString(R.string.for_month,
                        dateFormat.format(startDate), dateFormat.format(calendar.getTime()));
            case Budget.PERIOD_YEARLY:
                calendar.add(Calendar.YEAR, 1);
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                return context.getString(R.string.for_year,
                        dateFormat.format(startDate), dateFormat.format(calendar.getTime()));
            default:
                return "";
        }
    }

    /**
     * Żąda uprawnień do wyświetlania powiadomień (dla Androida Tiramisu i nowszych).
     *
     * @param context Kontekst aplikacji.
     */
    public static void requestNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (context instanceof android.app.Activity) {
                    android.app.Activity activity = (android.app.Activity) context;
                    activity.requestPermissions(
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                            100);
                }
            }
        }
    }
}