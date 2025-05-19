package com.example.paydaylay.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BudgetWidgetDataHelper {
    private static final String TAG = "BudgetWidgetDataHelper";
    private static final String PREFS_NAME = "BudgetWidgetPrefs";
    private static final String KEY_ALL_BUDGETS = "allBudgets";
    private static final String KEY_LAST_UPDATE = "lastUpdate";
    private static final String KEY_BUDGET_PREFIX = "budget_";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final DatabaseManager databaseManager;

    public BudgetWidgetDataHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.databaseManager = new DatabaseManager();
    }



    public void saveAllBudgets(List<Budget> budgets) {
        String json = gson.toJson(budgets);
        prefs.edit().putString(KEY_ALL_BUDGETS, json).apply();
    }

    public List<Budget> getAllBudgets() {
        String json = prefs.getString(KEY_ALL_BUDGETS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Budget>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void saveLastUpdateTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_UPDATE, timestamp).apply();
    }

    public long getLastUpdateTime() {
        return prefs.getLong(KEY_LAST_UPDATE, 0);
    }

    public void updateAllWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, BudgetWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

        if (appWidgetIds.length > 0) {
            Intent updateIntent = new Intent(context, BudgetWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            context.sendBroadcast(updateIntent);

            Log.d(TAG, "Zaktualizowano " + appWidgetIds.length + " widgetów");
        }
    }
    public void saveBudgetForWidget(int appWidgetId, Budget budget) {
        if (budget == null) {
            Log.e(TAG, "Nie można zapisać pustego budżetu");
            return;
        }

        // Konwertuj budżet na format JSON
        String budgetJson = gson.toJson(budget);

        // Zapisz budżet w SharedPreferences z kluczem zawierającym ID widgetu
        prefs.edit()
                .putString(KEY_BUDGET_PREFIX + appWidgetId, budgetJson)
                .apply();

        Log.d(TAG, "Zapisano budżet dla widgetu ID: " + appWidgetId);

        // Zaktualizuj widget
        updateWidget(appWidgetId);
    }

    public Budget getBudgetForWidget(int appWidgetId) {
        String budgetJson = prefs.getString(KEY_BUDGET_PREFIX + appWidgetId, null);
        if (budgetJson == null) {
            return null;
        }

        Budget budget = gson.fromJson(budgetJson, Budget.class);

        // Aktualizuj dane o wydatkach na podstawie transakcji
        if (budget != null) {
            loadTransactionsAndUpdateBudget(budget, appWidgetId);
        }

        return budget;
    }
    private void loadTransactionsAndUpdateBudget(Budget budget, int appWidgetId) {
        if (budget == null || budget.getUserId() == null) {
            return;
        }

        // Oblicz zakres dat dla budżetu
        Date startDate = new Date(budget.getPeriodStartDate());
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(budget.getPeriodStartDate());

        switch (budget.getPeriodType()) {
            case Budget.PERIOD_DAILY:
                endCal.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Budget.PERIOD_WEEKLY:
                endCal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case Budget.PERIOD_MONTHLY:
                endCal.add(Calendar.MONTH, 1);
                break;
            case Budget.PERIOD_YEARLY:
                endCal.add(Calendar.YEAR, 1);
                break;
        }

        databaseManager.getTransactionsForBudget(
                budget.getUserId(),
                budget.getCategoryId(),
                startDate,
                endCal.getTime(),
                new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        // Obliczanie wydatków - istniejący kod
                        double totalSpent = 0;
                        for (Transaction transaction : transactions) {
                            if (transaction.isExpense()) {
                                totalSpent += transaction.getAmount();
                            }
                        }

                        // Zapisz tylko jeśli kwota się zmieniła
                        if (Math.abs(budget.getSpent() - totalSpent) > 0.01) {
                            budget.setSpent(totalSpent);

                            // Zapisz zaktualizowany budżet bez wywoływania updateWidget
                            prefs.edit()
                                    .putString(KEY_BUDGET_PREFIX + appWidgetId, gson.toJson(budget))
                                    .apply();

                            Log.d(TAG, "Zaktualizowano wydatki: " + totalSpent + " zł");

                            // Sprawdź czy należy wyświetlić powiadomienie
                            BudgetNotificationManager notificationManager =
                                    new BudgetNotificationManager(context);
                            notificationManager.checkAndShowNotification(budget, appWidgetId);
                        } else {
                            Log.d(TAG, "Wydatki bez zmian: " + totalSpent + " zł");
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Błąd przy ładowaniu transakcji: " + e.getMessage());
                    }
                });
    }

    private void updateWidget(int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Intent updateIntent = new Intent(context, BudgetWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
        context.sendBroadcast(updateIntent);
    }
}