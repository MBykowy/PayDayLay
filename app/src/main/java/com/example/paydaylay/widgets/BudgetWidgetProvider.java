package com.example.paydaylay.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.MainActivity;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.example.paydaylay.utils.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "com.example.paydaylay.ACTION_UPDATE_WIDGET";
    private static final String PREFS_WIDGET_CATEGORY = "widget_category_";
    private static final String PREFS_WIDGET_BUDGET_TYPE = "widget_budget_type_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, false);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateWidget(context, appWidgetManager, appWidgetId, false);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, BudgetWidgetProvider.class));
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, true);
            }
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean forceDataRefresh) {
        AuthManager authManager = new AuthManager();
        String userId = authManager.getCurrentUserId();

        if (userId == null) {
            // User not logged in
            showNotLoggedInState(context, appWidgetManager, appWidgetId);
            return;
        }

        RemoteViews views = getRemoteViews(context);

        // Set click action for the widget
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("fragment", "dashboard");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.textViewWidgetTitle, pendingIntent);

        // Show loading state
        views.setTextViewText(R.id.textViewWidgetCategory, context.getString(R.string.loading));
        views.setProgressBar(R.id.progressBarBudget, 100, 0, false);
        views.setTextViewText(R.id.textViewWidgetSpent, "");
        views.setTextViewText(R.id.textViewWidgetRemaining, "");
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Get saved preferences for this widget
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String categoryId = prefs.getString(PREFS_WIDGET_CATEGORY + appWidgetId, null);
        int budgetType = prefs.getInt(PREFS_WIDGET_BUDGET_TYPE + appWidgetId, Budget.PERIOD_MONTHLY);

        // Load data
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                Map<String, Category> categoryMap = new HashMap<>();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category);
                }

                // Find the budget for the selected category
                databaseManager.getBudgets(userId, new DatabaseManager.OnBudgetsLoadedListener() {
                    @Override
                    public void onBudgetsLoaded(List<Budget> budgets) {

                        final Budget[] selectedBudgetArray = new Budget[1];
                        for (Budget budget : budgets) {
                            if ((categoryId == null && budget.getCategoryId() == null) ||
                                    (categoryId != null && categoryId.equals(budget.getCategoryId())) &&
                                            budget.getPeriodType() == budgetType) {
                                selectedBudgetArray[0] = budget;
                                break;
                            }
                        }

                        if (selectedBudgetArray[0] == null) {
                            // No budget found for this category
                            showNoBudgetState(context, appWidgetManager, appWidgetId, categoryMap.get(categoryId));
                            return;
                        }

                        // Calculate date range
                        Date startDate = new Date(selectedBudgetArray[0].getPeriodStartDate());
                        Calendar endCal = Calendar.getInstance();
                        endCal.setTimeInMillis(selectedBudgetArray[0].getPeriodStartDate());

                        switch (selectedBudgetArray[0].getPeriodType()) {
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

                        // Get transactions
                        databaseManager.getTransactionsForBudget(userId, selectedBudgetArray[0].getCategoryId(),
                                startDate, endDate, new DatabaseManager.OnTransactionsLoadedListener() {
                                    @Override
                                    public void onTransactionsLoaded(List<Transaction> transactions) {
                                        // Calculate total spending
                                        double totalSpending = 0;
                                        for (Transaction transaction : transactions) {
                                            if (transaction.isExpense()) {
                                                totalSpending += transaction.getAmount();
                                            }
                                        }

                                        // Update widget with data
                                        updateWidgetWithBudgetData(context, appWidgetManager, appWidgetId,
                                                selectedBudgetArray[0], totalSpending, categoryMap.get(categoryId));
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        showErrorState(context, appWidgetManager, appWidgetId);
                                    }
                                });
                    }

                    @Override
                    public void onError(Exception e) {
                        showErrorState(context, appWidgetManager, appWidgetId);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                showErrorState(context, appWidgetManager, appWidgetId);
            }
        });
    }

    private void updateWidgetWithBudgetData(Context context, AppWidgetManager appWidgetManager,
                                            int appWidgetId, Budget budget, double spent, Category category) {
        RemoteViews views = getRemoteViews(context);

        String currencySymbol = context.getString(R.string.currency_symbol);

        // Category name
        String categoryName = category != null ? category.getName() : context.getString(R.string.overall_budget);
        views.setTextViewText(R.id.textViewWidgetCategory, categoryName);

        // Calculate progress
        double limit = budget.getLimit();
        int progressPercentage = spent >= limit ? 100 : (int)((spent / limit) * 100);

        // Spent and remaining amounts
        String spentText = String.format(Locale.getDefault(), context.getString(R.string.spent_format),
                spent, currencySymbol);
        String remainingText = String.format(Locale.getDefault(), context.getString(R.string.remaining_format),
                Math.max(0, limit - spent), currencySymbol);

        views.setProgressBar(R.id.progressBarBudget, 100, progressPercentage, false);
        views.setTextViewText(R.id.textViewWidgetSpent, spentText);
        views.setTextViewText(R.id.textViewWidgetRemaining, remainingText);

        // Set progress color based on percentage
        if (progressPercentage >= 90) {
            views.setInt(R.id.progressBarBudget, "setProgressTint", 0xFFFF5252); // Red
        } else if (progressPercentage >= 75) {
            views.setInt(R.id.progressBarBudget, "setProgressTint", 0xFFFFB74D); // Orange
        } else {
            views.setInt(R.id.progressBarBudget, "setProgressTint", 0xFF4CAF50); // Green
        }

        // Last updated time
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String lastUpdated = context.getString(R.string.last_updated, dateFormat.format(new Date()));
        views.setTextViewText(R.id.textViewWidgetLastUpdated, lastUpdated);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void showNotLoggedInState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = getRemoteViews(context);
        views.setTextViewText(R.id.textViewWidgetCategory, context.getString(R.string.not_logged_in));
        views.setViewVisibility(R.id.progressContainer, View.GONE);

        // Set click action to open login screen
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.textViewWidgetTitle, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void showNoBudgetState(Context context, AppWidgetManager appWidgetManager,
                                   int appWidgetId, Category category) {
        RemoteViews views = getRemoteViews(context);

        String categoryName = category != null ? category.getName() : context.getString(R.string.overall_budget);
        views.setTextViewText(R.id.textViewWidgetTitle, context.getString(R.string.budget_status));
        views.setTextViewText(R.id.textViewWidgetCategory, categoryName);
        views.setTextViewText(R.id.textViewWidgetSpent, context.getString(R.string.no_budget_set));
        views.setViewVisibility(R.id.progressBarBudget, View.GONE);
        views.setViewVisibility(R.id.textViewWidgetRemaining, View.GONE);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void showErrorState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = getRemoteViews(context);
        views.setTextViewText(R.id.textViewWidgetCategory, context.getString(R.string.error_loading_data));
        views.setViewVisibility(R.id.progressContainer, View.GONE);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private RemoteViews getRemoteViews(Context context) {
        // Check if dark theme is active
        boolean isDarkTheme = ThemeUtils.isNightMode(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.budget_widget);

        // Set background based on theme
        if (isDarkTheme) {
            views.setInt(R.id.textViewWidgetTitle, "setTextColor", 0xFFFFFFFF);
            views.setInt(R.id.textViewWidgetCategory, "setTextColor", 0xFFFFFFFF);
            views.setInt(R.id.textViewWidgetSpent, "setTextColor", 0xFFFFFFFF);
            views.setInt(R.id.textViewWidgetRemaining, "setTextColor", 0xFFFFFFFF);
            views.setInt(R.id.textViewWidgetLastUpdated, "setTextColor", 0xFFCCCCCC);
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_dark);
        } else {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
        }
        return views;
    }
}