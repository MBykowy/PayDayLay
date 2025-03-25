package com.example.paydaylay.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.example.paydaylay.utils.NotificationUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetCheckService extends Service {

    private AuthManager authManager;
    private DatabaseManager databaseManager;

    @Override
    public void onCreate() {
        super.onCreate();
        authManager = new AuthManager();
        databaseManager = new DatabaseManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkBudgets();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkBudgets() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            stopSelf();
            return;
        }

        // First, load all categories for this user
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                // Create a map for quick lookup
                Map<String, Category> categoryMap = new HashMap<>();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category);
                }

                // Then, load all budgets
                databaseManager.getBudgets(userId, new DatabaseManager.OnBudgetsLoadedListener() {
                    @Override
                    public void onBudgetsLoaded(List<Budget> budgets) {
                        // For each budget, load relevant transactions
                        for (Budget budget : budgets) {
                            loadTransactionsForBudget(budget, categoryMap);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        stopSelf();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                stopSelf();
            }
        });
    }

    private void loadTransactionsForBudget(Budget budget, Map<String, Category> categoryMap) {
        // Calculate date range for this budget period
        Date startDate = new Date(budget.getPeriodStartDate());
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(budget.getPeriodStartDate());

        switch (budget.getPeriodType()) {
            case Budget.PERIOD_DAILY:
                endCalendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case Budget.PERIOD_WEEKLY:
                endCalendar.add(Calendar.DAY_OF_YEAR, 7);
                break;
            case Budget.PERIOD_MONTHLY:
                endCalendar.add(Calendar.MONTH, 1);
                break;
            case Budget.PERIOD_YEARLY:
                endCalendar.add(Calendar.YEAR, 1);
                break;
        }
        Date endDate = endCalendar.getTime();

        // Query transactions for this budget
        databaseManager.getTransactionsForBudget(
                authManager.getCurrentUserId(),
                budget.getCategoryId(),
                startDate,
                endDate,
                new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        // Calculate total spending for this period
                        double totalSpending = 0;
                        for (Transaction transaction : transactions) {
                            if (transaction.isExpense()) {
                                totalSpending += transaction.getAmount();
                            }
                        }

                        // Check if budget limit is exceeded
                        if (totalSpending > budget.getLimit()) {
                            NotificationUtils.showBudgetAlertNotification(
                                    BudgetCheckService.this,
                                    budget,
                                    totalSpending,
                                    categoryMap);
                        }

                        // Check if this was the last budget to process
                        checkIfAllBudgetsProcessed();
                    }

                    @Override
                    public void onError(Exception e) {
                        // Continue with other budgets even if one fails
                        checkIfAllBudgetsProcessed();
                    }
                });
    }

    private void checkIfAllBudgetsProcessed() {
        // This would be implemented to track when all budgets are processed
        // For simplicity, we're not implementing the full tracking mechanism here
        // In a real app, you would use a counter or similar approach
    }
}