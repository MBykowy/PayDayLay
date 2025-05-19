package com.example.paydaylay.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.paydaylay.models.Budget;
import com.example.paydaylay.services.BudgetCheckService;

import java.util.List;

/**
 * Klasa odpowiedzialna za planowanie i anulowanie alarmów związanych z budżetami użytkownika.
 * Umożliwia ustawianie powtarzających się alarmów dla poszczególnych budżetów oraz jednorazowych sprawdzeń.
 */
public class BudgetAlarmScheduler {
    private static final String TAG = "BudgetAlarmScheduler";
    private Context context;

    /**
     * Konstruktor domyślny.
     */
    public BudgetAlarmScheduler() {
    }

    /**
     * Konstruktor z kontekstem.
     *
     * @param context Kontekst aplikacji.
     */
    public BudgetAlarmScheduler(Context context) {
        this.context = context;
    }

    /**
     * Planuje alarmy dla podanych budżetów.
     *
     * @param budgets Lista budżetów, dla których mają zostać ustawione alarmy.
     */
    public void scheduleAlarms(List<Budget> budgets) {
        if (context == null || budgets == null || budgets.isEmpty()) {
            Log.e(TAG, "Cannot schedule alarms, context or budgets invalid");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (Budget budget : budgets) {
            Intent intent = new Intent(context, BudgetCheckService.class);
            intent.putExtra("BUDGET_ID", budget.getId());

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    budget.hashCode(),
                    intent,
                    flags
            );

            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000, // Pierwsze uruchomienie wkrótce
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    /**
     * Anuluje wszystkie alarmy związane z budżetami.
     */
    public void cancelAlarms() {
        if (context == null) {
            Log.e(TAG, "Cannot cancel alarms, context is null");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Log.d(TAG, "All budget alarms canceled");
    }

    /**
     * Planuje jednorazowe sprawdzenie budżetów.
     */
    public void scheduleBudgetCheck() {
        if (context == null) {
            Log.e(TAG, "Cannot schedule budget check, context is null");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service is not available");
            return;
        }

        Intent intent = new Intent(context, BudgetCheckService.class);
        intent.putExtra("CHECK_ALL_BUDGETS", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = "budget_general_check".hashCode();

        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                requestCode,
                intent,
                flags
        );

        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 5000, // Sprawdzenie za 5 sekund
                pendingIntent
        );

        Log.d(TAG, "Budget check scheduled for all budgets");
    }

    /**
     * Planuje alarmy dla wszystkich budżetów użytkownika.
     * Pobiera budżety z bazy danych i ustawia alarmy.
     */
    public void scheduleAlarms() {
        if (context == null) {
            Log.e(TAG, "Cannot schedule alarms, context is null");
            return;
        }

        com.example.paydaylay.firebase.DatabaseManager databaseManager =
                new com.example.paydaylay.firebase.DatabaseManager();

        String userId = new com.example.paydaylay.firebase.AuthManager().getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot schedule alarms, user is not logged in");
            return;
        }

        databaseManager.getBudgets(userId, new com.example.paydaylay.firebase.DatabaseManager.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                scheduleAlarms(budgets);
                Log.d(TAG, "Scheduled alarms for " + budgets.size() + " budgets");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading budgets for alarm scheduling", e);
            }
        });
    }

    /**
     * Statyczna metoda do planowania jednorazowego sprawdzenia budżetów.
     *
     * @param context Kontekst aplikacji.
     */
    public static void scheduleBudgetCheck(Context context) {
        BudgetAlarmScheduler scheduler = new BudgetAlarmScheduler(context);
        scheduler.scheduleBudgetCheck();
    }

    /**
     * Statyczna metoda do anulowania wszystkich alarmów związanych z budżetami.
     *
     * @param context Kontekst aplikacji.
     */
    public static void cancelBudgetCheck(Context context) {
        BudgetAlarmScheduler scheduler = new BudgetAlarmScheduler(context);
        scheduler.cancelAlarms();
    }
}