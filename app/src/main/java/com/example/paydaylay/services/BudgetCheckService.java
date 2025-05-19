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

/**
 * Usługa odpowiedzialna za sprawdzanie budżetów użytkownika.
 * Analizuje transakcje w ramach budżetów i powiadamia użytkownika, jeśli limit budżetu został przekroczony.
 */
public class BudgetCheckService extends Service {

    private AuthManager authManager;
    private DatabaseManager databaseManager;

    /**
     * Wywoływane podczas tworzenia usługi.
     * Inicjalizuje menedżery Firebase.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        authManager = new AuthManager();
        databaseManager = new DatabaseManager();
    }

    /**
     * Wywoływane po uruchomieniu usługi.
     * Rozpoczyna proces sprawdzania budżetów.
     *
     * @param intent  Obiekt Intent przekazany do usługi.
     * @param flags   Flagi określające sposób uruchomienia usługi.
     * @param startId Identyfikator startu usługi.
     * @return Kod określający sposób ponownego uruchamiania usługi.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkBudgets();
        return START_NOT_STICKY;
    }

    /**
     * Wywoływane, gdy usługa jest powiązana z komponentem.
     *
     * @param intent Obiekt Intent przekazany do usługi.
     * @return Obiekt IBinder do komunikacji z usługą.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Sprawdza budżety użytkownika i analizuje transakcje w ich ramach.
     */
    private void checkBudgets() {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            stopSelf();
            return;
        }

        // Pobiera kategorie użytkownika
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> categories) {
                Map<String, Category> categoryMap = new HashMap<>();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category);
                }

                // Pobiera budżety użytkownika
                databaseManager.getBudgets(userId, new DatabaseManager.OnBudgetsLoadedListener() {
                    @Override
                    public void onBudgetsLoaded(List<Budget> budgets) {
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

    /**
     * Ładuje transakcje dla danego budżetu i sprawdza, czy limit został przekroczony.
     *
     * @param budget      Obiekt budżetu.
     * @param categoryMap Mapa kategorii użytkownika.
     */
    private void loadTransactionsForBudget(Budget budget, Map<String, Category> categoryMap) {
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

        databaseManager.getTransactionsForBudget(
                authManager.getCurrentUserId(),
                budget.getCategoryId(),
                startDate,
                endDate,
                new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> transactions) {
                        double totalSpending = 0;
                        for (Transaction transaction : transactions) {
                            if (transaction.isExpense()) {
                                totalSpending += transaction.getAmount();
                            }
                        }

                        if (totalSpending > budget.getLimit()) {
                            NotificationUtils.showBudgetAlertNotification(
                                    BudgetCheckService.this,
                                    budget,
                                    totalSpending,
                                    categoryMap);
                        }

                        checkIfAllBudgetsProcessed();
                    }

                    @Override
                    public void onError(Exception e) {
                        checkIfAllBudgetsProcessed();
                    }
                });
    }

    /**
     * Sprawdza, czy wszystkie budżety zostały przetworzone.
     * (Do implementacji w pełnej wersji aplikacji).
     */
    private void checkIfAllBudgetsProcessed() {
        // Mechanizm śledzenia przetworzonych budżetów
    }
}