package com.example.paydaylay.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Budget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Serwis odpowiedzialny za aktualizację danych widżetów budżetowych.
 * Pobiera dane budżetów z bazy danych i aktualizuje widżety na ekranie głównym.
 */
public class BudgetWidgetUpdateService extends JobIntentService {
    private static final String TAG = "WidgetUpdateService";
    private static final int JOB_ID = 1000;

    /**
     * Dodaje zadanie do kolejki pracy serwisu.
     *
     * @param context Kontekst aplikacji.
     * @param intent  Intencja zawierająca dane do przetworzenia.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, BudgetWidgetUpdateService.class, JOB_ID, intent);
    }

    /**
     * Obsługuje zadanie serwisu.
     *
     * @param intent Intencja zawierająca dane do przetworzenia.
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "Aktualizacja widgetów budżetowych rozpoczęta");
        updateWidgets();
    }

    /**
     * Aktualizuje dane widżetów budżetowych.
     * Pobiera budżety z bazy danych, zapisuje je w SharedPreferences i powiadamia widżety o zmianach.
     */
    private void updateWidgets() {
        AuthManager authManager = new AuthManager();
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            Log.d(TAG, "Użytkownik nie jest zalogowany, pomijam aktualizację widgetów");
            return;
        }

        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.getBudgets(userId, new DatabaseManager.OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                // Zapisz dane w SharedPreferences
                BudgetWidgetDataHelper dataHelper = new BudgetWidgetDataHelper(getApplicationContext());
                dataHelper.saveAllBudgets(budgets);
                dataHelper.saveLastUpdateTime(new Date().getTime());

                // Powiadom widgety o aktualizacji
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                        new ComponentName(getApplicationContext(), BudgetWidgetProvider.class));

                if (appWidgetIds.length > 0) {
                    // Aktualizuj widgety
                    Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                    sendBroadcast(updateIntent);

                    Log.d(TAG, "Zaktualizowano " + appWidgetIds.length + " widgetów budżetowych");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Błąd podczas aktualizacji widgetów", e);
            }
        });
    }

    /**
     * Metoda wywoływana przy bindowaniu serwisu.
     *
     * @param intent Intencja zawierająca dane do przetworzenia.
     * @return Zwraca null, ponieważ serwis nie obsługuje bindowania.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}