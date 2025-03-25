package com.example.paydaylay.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.example.paydaylay.utils.BudgetAlarmScheduler;
import com.example.paydaylay.utils.NotificationUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BudgetNotificationService extends Service {
    private AuthManager authManager;
    private DatabaseManager databaseManager;
    private AtomicInteger pendingTasks = new AtomicInteger(0);

    @Override
    public void onCreate() {
        super.onCreate();
        authManager = new AuthManager();
        databaseManager = new DatabaseManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Load categories and budgets
        loadCategoriesAndCheckBudgets(userId);

        // Reschedule next check
        BudgetAlarmScheduler.scheduleBudgetCheck(this);

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void loadCategoriesAndCheckBudgets(String userId) {
        pendingTasks.incrementAndGet();

        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                Map<String, Category> categoryMap = new HashMap<>();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category);
                }

                loadBudgetsAndCheck(userId, categoryMap);
            }

            @Override
            public void onError(Exception e) {
                decrementAndCheckCompletion();
            }
        });
    }

    private void loadBudgetsAndCheck(String userId, Map<String, Category> categoryMap) {
        pendingTasks.incrementAndGet();

        databaseManager.getBudgets(userId, new DatabaseManager.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                for (Budget budget : budgets) {
                    checkBudget(budget, categoryMap);
                }
                decrementAndCheckCompletion();
            }

            @Override
            public void onError(Exception e) {
                decrementAndCheckCompletion();
            }
        });
    }

    private void checkBudget(Budget budget, Map<String, Category> categoryMap) {
        pendingTasks.incrementAndGet();

        // Calculate date range for this budget
        Date startDate = new Date(budget.getPeriodStartDate());
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(budget.getPeriodStartDate());

        switch(budget.getPeriodType()) {
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
        Date endDate = endCal.getTime();

        // Query transactions for this period
        databaseManager.getTransactionsForBudget(
                budget.getUserId(),
                budget.getCategoryId(),
                startDate,
                endDate,
                new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        processTransactionsForBudget(transactions, budget, categoryMap);
                        decrementAndCheckCompletion();
                    }

                    @Override
                    public void onError(Exception e) {
                        decrementAndCheckCompletion();
                    }
                });
    }

    private void processTransactionsForBudget(List<Transaction> transactions,
                                              Budget budget,
                                              Map<String, Category> categoryMap) {
        // Calculate total spending
        double totalSpending = 0;
        for (Transaction transaction : transactions) {
            if (transaction.isExpense()) {
                // Only count expenses towards budget
                totalSpending += transaction.getAmount();
            }
        }

        // Check if we exceeded or reached warning threshold (e.g., 90%)
        double warningThreshold = budget.getLimit() * 0.9;

        if (totalSpending >= budget.getLimit()) {
            // Budget exceeded, show notification
            NotificationUtils.showBudgetAlertNotification(this, budget, totalSpending, categoryMap);
        } else if (totalSpending >= warningThreshold) {
            // Approaching budget limit, show warning
            NotificationUtils.showBudgetWarningNotification(this, budget, totalSpending, categoryMap);
        }
    }

    private void decrementAndCheckCompletion() {
        if (pendingTasks.decrementAndGet() <= 0) {
            // All tasks completed, stop service
            stopSelf();
        }
    }
}