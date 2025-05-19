package com.example.paydaylay.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.MainActivity;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BudgetWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "BudgetWidgetProvider";
    private static final String PREFS_NAME = "com.example.paydaylay.widgets.BudgetWidget";
    private static final String PREF_PREFIX_KEY = "budgetwidget_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Aktualizuj każdy widget
        for (int appWidgetId : appWidgetIds) {
            // Pobierz dane budżetu dla tego widgetu
            BudgetWidgetDataHelper dataHelper = new BudgetWidgetDataHelper(context);
            Budget budget = dataHelper.getBudgetForWidget(appWidgetId);

            if (budget != null) {
                updateBudgetWidget(context, appWidgetManager, appWidgetId, budget);
            } else {
                // Brak konkretnego budżetu dla widgetu - pokaż ogólny stan
                updateWidgetWithEmptyState(context, appWidgetManager, appWidgetId);
            }
        }
    }

    // Metoda do aktualizacji widgetu z danymi budżetu
    private static void updateBudgetWidget(Context context, AppWidgetManager appWidgetManager,
                                           int appWidgetId, Budget budget) {
        // Utwórz widok
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.budget_widget);

        // Ustaw tytuł widgetu
        if (budget.getCategoryId() != null) {
            // Pobierz nazwę kategorii (opcjonalnie)
            DatabaseManager dbManager = new DatabaseManager();
            dbManager.getCategoryById(budget.getCategoryId(), new DatabaseManager.OnCategoryLoadedListener() {
                @Override
                public void onCategoryLoaded(Category category) {
                    if (category != null) {
                        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.budget_widget);
                        updateViews.setTextViewText(R.id.textViewWidgetCategory, category.getName());
                        appWidgetManager.updateAppWidget(appWidgetId, updateViews);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e("BudgetWidget", "Error loading category", e);
                }
            });
        } else {
            views.setTextViewText(R.id.textViewWidgetCategory, context.getString(R.string.overall_budget));
        }

        // Ustaw wartości budżetu
        views.setTextViewText(R.id.textViewWidgetTitle, context.getString(R.string.budget_status));

        // Formatuj kwoty budżetu
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        double spent = budget.getSpent();
        double limit = budget.getLimit();
        double remaining = limit - spent;

        views.setTextViewText(R.id.textViewWidgetAmount,
                currencyFormat.format(spent) + " / " + currencyFormat.format(limit));

        // Ustaw kwoty wydane i pozostałe
        views.setTextViewText(R.id.textViewWidgetSpent,
                context.getString(R.string.spent) + ": " + currencyFormat.format(spent));
        views.setTextViewText(R.id.textViewWidgetRemaining,
                context.getString(R.string.remaining) + ": " + currencyFormat.format(remaining));

        // Ustaw pasek postępu
        int progressPercent = (int) (limit > 0 ? (spent / limit * 100) : 0);
        progressPercent = Math.min(100, progressPercent); // Limit do 100%
        views.setProgressBar(R.id.progressBarBudget, 100, progressPercent, false);

        // Ustaw datę ostatniej aktualizacji
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String lastUpdated = context.getString(R.string.updated) + ": " + dateFormat.format(new Date());
        views.setTextViewText(R.id.textViewWidgetLastUpdated, lastUpdated);

        // Dodaj interakcję - kliknięcie otwiera aplikację
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_BUDGET_TAB", true);
        intent.putExtra("BUDGET_ID", budget.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // Aktualizuj widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // Log dla debugowania
        Log.d(TAG, "Widget zaktualizowany. ID: " + appWidgetId +
                ", Budżet: " + limit + ", Wydatki: " + spent +
                ", Procent: " + progressPercent + "%");
    }

    // Metoda do wyświetlenia pustego stanu widgetu
    private static void updateWidgetWithEmptyState(Context context, AppWidgetManager appWidgetManager,
                                                   int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.budget_widget);
        views.setTextViewText(R.id.textViewWidgetTitle, context.getString(R.string.budget_status));
        views.setTextViewText(R.id.textViewWidgetCategory, context.getString(R.string.no_budget_selected));
        views.setTextViewText(R.id.textViewWidgetAmount, "");
        views.setProgressBar(R.id.progressBarBudget, 100, 0, false);

        // Wyczyść pola z kwotami
        views.setTextViewText(R.id.textViewWidgetSpent, "");
        views.setTextViewText(R.id.textViewWidgetRemaining, "");

        // Ustaw datę ostatniej aktualizacji
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String lastUpdated = context.getString(R.string.updated) + ": " + dateFormat.format(new Date());
        views.setTextViewText(R.id.textViewWidgetLastUpdated, lastUpdated);

        // Dodaj interakcję - kliknięcie otwiera ekran konfiguracji widgetu
        Intent configIntent = new Intent(context, BudgetWidgetConfigActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Pobierz dane budżetu dla tego widgetu
        BudgetWidgetDataHelper dataHelper = new BudgetWidgetDataHelper(context);
        Budget budget = dataHelper.getBudgetForWidget(appWidgetId);

        if (budget != null) {
            // Mamy dane budżetu - aktualizujemy widget
            updateBudgetWidget(context, appWidgetManager, appWidgetId, budget);

            // Log dla celów debugowania
            Log.d(TAG, "Zaktualizowano budżet dla widgetu ID: " + appWidgetId);
        } else {
            // Brak danych - pokazujemy pusty stan lub komunikat ładowania
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.budget_widget);

            views.setTextViewText(R.id.textViewWidgetTitle, context.getString(R.string.budget_status));
            views.setTextViewText(R.id.textViewWidgetCategory, context.getString(R.string.loading));
            views.setProgressBar(R.id.progressBarBudget, 100, 0, false);

            // Wyczyść pola z kwotami
            views.setTextViewText(R.id.textViewWidgetSpent, "");
            views.setTextViewText(R.id.textViewWidgetRemaining, "");

            // Ustawia aktualną datę jako ostatnią aktualizację
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String lastUpdated = context.getString(R.string.updated) + ": " + dateFormat.format(new Date());
            views.setTextViewText(R.id.textViewWidgetLastUpdated, lastUpdated);

            // Dodaje intent do otwarcia aplikacji po kliknięciu w widget
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

            // Aktualizuje widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

            Log.d(TAG, "Brak danych budżetu dla widgetu ID: " + appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Wywoływane przy dodaniu pierwszego widgetu
        Log.d(TAG, "Widget enabled");
    }

    @Override
    public void onDisabled(Context context) {
        // Wywoływane gdy wszystkie widgety zostaną usunięte
        Log.d(TAG, "Widget disabled");
    }

    // Zapisuje ID budżetu dla danego widgetu
    public static void saveBudgetIdPref(Context context, int appWidgetId, String budgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, budgetId);
        prefs.apply();
    }

    // Odczytuje ID budżetu dla danego widgetu
    public static String loadBudgetIdPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
    }

    // Usuwa preferencje dla danego widgetu
    public static void deleteBudgetIdPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    public static void updateAppWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, BudgetWidgetProvider.class);
    }
}