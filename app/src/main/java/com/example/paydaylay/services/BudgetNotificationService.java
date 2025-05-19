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

/**
 * Usługa odpowiedzialna za powiadamianie użytkownika o stanie budżetów.
 * Sprawdza, czy budżety zostały przekroczone lub zbliżają się do limitu, i wyświetla odpowiednie powiadomienia.
 */
public class BudgetNotificationService extends Service {
    private AuthManager authManager;
    private DatabaseManager databaseManager;
    private AtomicInteger pendingTasks = new AtomicInteger(0);

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
     * Rozpoczyna proces sprawdzania budżetów i planuje kolejne sprawdzenie.
     *
     * @param intent  Obiekt Intent przekazany do usługi.
     * @param flags   Flagi określające sposób uruchomienia usługi.
     * @param startId Identyfikator startu usługi.
     * @return Kod określający sposób ponownego uruchamiania usługi.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Ładuje kategorie i budżety
        loadCategoriesAndCheckBudgets(userId);

        // Planowanie kolejnego sprawdzenia
        BudgetAlarmScheduler.scheduleBudgetCheck(this);

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
     * Ładuje kategorie użytkownika i sprawdza budżety.
     *
     * @param userId ID użytkownika.
     */
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

    /**
     * Ładuje budżety użytkownika i sprawdza ich stan.
     *
     * @param userId      ID użytkownika.
     * @param categoryMap Mapa kategorii użytkownika.
     */
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

    /**
     * Sprawdza stan danego budżetu i wyświetla powiadomienia, jeśli to konieczne.
     *
     * @param budget      Obiekt budżetu.
     * @param categoryMap Mapa kategorii użytkownika.
     */
    private void checkBudget(Budget budget, Map<String, Category> categoryMap) {
        pendingTasks.incrementAndGet();

        // Oblicza zakres dat dla budżetu
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

        // Pobiera transakcje dla tego okresu
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

    /**
     * Przetwarza transakcje dla danego budżetu i wyświetla odpowiednie powiadomienia.
     *
     * @param transactions Lista transakcji.
     * @param budget       Obiekt budżetu.
     * @param categoryMap  Mapa kategorii użytkownika.
     */
    private void processTransactionsForBudget(List<Transaction> transactions,
                                              Budget budget,
                                              Map<String, Category> categoryMap) {
        // Oblicza całkowite wydatki
        double totalSpending = 0;
        for (Transaction transaction : transactions) {
            if (transaction.isExpense()) {
                // Uwzględnia tylko wydatki
                totalSpending += transaction.getAmount();
            }
        }

        // Sprawdza, czy przekroczono limit lub osiągnięto próg ostrzeżenia (np. 90%)
        double warningThreshold = budget.getLimit() * 0.9;

        if (totalSpending >= budget.getLimit()) {
            // Przekroczono budżet, wyświetla powiadomienie
            NotificationUtils.showBudgetAlertNotification(this, budget, totalSpending, categoryMap);
        } else if (totalSpending >= warningThreshold) {
            // Zbliżono się do limitu budżetu, wyświetla ostrzeżenie
            NotificationUtils.showBudgetWarningNotification(this, budget, totalSpending, categoryMap);
        }
    }

    /**
     * Zmniejsza licznik zadań i sprawdza, czy wszystkie zostały zakończone.
     */
    private void decrementAndCheckCompletion() {
        if (pendingTasks.decrementAndGet() <= 0) {
            // Wszystkie zadania zakończone, zatrzymuje usługę
            stopSelf();
        }
    }
}