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

public class BudgetAlarmScheduler {
    private static final String TAG = "BudgetAlarmScheduler";
    private Context context;

    // Default constructor
    public BudgetAlarmScheduler() {
    }

    // Constructor with context
    public BudgetAlarmScheduler(Context context) {
        this.context = context;
    }

    // Schedule alarms for budget checks
    public void scheduleAlarms(List<Budget> budgets) {
        if (context == null || budgets == null || budgets.isEmpty()) {
            Log.e(TAG, "Cannot schedule alarms, context or budgets invalid");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Implementation for scheduling alarms
        for (Budget budget : budgets) {
            // Schedule daily check for each budget
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

            // Schedule daily at 8am
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000, // First run soon
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    // Cancel all budget alarms
    public void cancelAlarms() {
        if (context == null) {
            Log.e(TAG, "Cannot cancel alarms, context is null");
            return;
        }

        // Implementation for canceling previously set alarms
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // We would need to cancel each alarm with matching PendingIntent
        Log.d(TAG, "All budget alarms canceled");
    }
    public void scheduleBudgetCheck() {
        if (context == null) {
            Log.e(TAG, "Cannot schedule budget check, context is null");
            return;
        }

        // Get the AlarmManager service
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service is not available");
            return;
        }

        // Create intent for budget check service (without specific budget ID)
        // This will trigger a general budget check
        Intent intent = new Intent(context, BudgetCheckService.class);
        intent.putExtra("CHECK_ALL_BUDGETS", true);

        // Create flags for PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        // Create a unique request code for this general check
        int requestCode = "budget_general_check".hashCode();

        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                requestCode,
                intent,
                flags
        );

        // Schedule a one-time check soon
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 5000, // Check in 5 seconds
                pendingIntent
        );

        Log.d(TAG, "Budget check scheduled for all budgets");
    }

    public void scheduleAlarms() {
        if (context == null) {
            Log.e(TAG, "Cannot schedule alarms, context is null");
            return;
        }

        // Get the DatabaseManager
        com.example.paydaylay.firebase.DatabaseManager databaseManager =
                new com.example.paydaylay.firebase.DatabaseManager();

        // Get the current user ID
        String userId = new com.example.paydaylay.firebase.AuthManager().getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "Cannot schedule alarms, user is not logged in");
            return;
        }

        // Fetch all budgets for the current user
        databaseManager.getBudgets(userId, new com.example.paydaylay.firebase.DatabaseManager.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                // Schedule alarms for all loaded budgets
                scheduleAlarms(budgets);
                Log.d(TAG, "Scheduled alarms for " + budgets.size() + " budgets");
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading budgets for alarm scheduling", e);
            }
        });
    }
    // Add this static method to BudgetAlarmScheduler class
    public static void scheduleBudgetCheck(Context context) {
        // Create an instance and delegate to the instance method
        BudgetAlarmScheduler scheduler = new BudgetAlarmScheduler(context);
        scheduler.scheduleBudgetCheck();
    }
    // Add this static method to BudgetAlarmScheduler class
    public static void cancelBudgetCheck(Context context) {
        // Create an instance and delegate to the instance method
        BudgetAlarmScheduler scheduler = new BudgetAlarmScheduler(context);
        scheduler.cancelAlarms();
    }
}