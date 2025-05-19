package com.example.paydaylay.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.paydaylay.database.AppDatabase;
import com.example.paydaylay.database.TransactionEntity;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import com.example.paydaylay.widgets.BudgetWidgetDataHelper;

import android.content.Intent;
import java.util.Calendar;
import android.content.ComponentName;
import com.example.paydaylay.widgets.BudgetWidgetProvider;

/**
 * Klasa DatabaseManager zarządza operacjami na danych w Firebase Firestore oraz lokalnej bazie danych.
 * Oferuje metody do obsługi użytkowników, transakcji, kategorii i budżetów.
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";
    private final FirebaseFirestore db;
    private final String TRANSACTIONS_COLLECTION = "transactions";
    private final String CATEGORIES_COLLECTION = "categories";
    private final String BUDGETS_COLLECTION = "budgets";
    private final String USERS_COLLECTION = "users";

    /**
     * Konstruktor klasy DatabaseManager.
     * Inicjalizuje instancję FirebaseFirestore z włączoną obsługą offline.
     */
    public DatabaseManager() {
        db = FirebaseFirestore.getInstance();

        // Konfiguracja Firestore dla obsługi offline
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
    }

    /**
     * Inicjalizuje Firebase AppCheck.
     *
     * @param context Kontekst aplikacji.
     */
    public static void initAppCheck(Context context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        Log.d("DatabaseManager", "Firebase initialized");
    }

    /**
     * Tworzy lub aktualizuje profil użytkownika w Firestore.
     *
     * @param userId   Identyfikator użytkownika.
     * @param name     Imię użytkownika.
     * @param email    Adres e-mail użytkownika.
     * @param listener Interfejs zwrotny do obsługi wyniku operacji.
     */
    public void updateUserProfile(String userId, String name, String email, OnUserOperationListener listener) {
        if (userId == null) {
            listener.onError(new IllegalArgumentException("User ID cannot be null"));
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .set(createUserMap(name, email))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /**
     * Tworzy mapę danych użytkownika.
     *
     * @param name  Imię użytkownika.
     * @param email Adres e-mail użytkownika.
     * @return Mapa danych użytkownika.
     */
    private Map<String, Object> createUserMap(String name, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("createdAt", new Date());
        userMap.put("lastLogin", new Date());
        return userMap;
    }

    /**
     * Usuwa transakcje powiązane z daną kategorią.
     *
     * @param userId     Identyfikator użytkownika.
     * @param categoryId Identyfikator kategorii.
     * @param listener   Interfejs zwrotny do obsługi wyniku operacji.
     */
    public void deleteTransactionsForCategory(String userId, String categoryId, OnTransactionListener listener) {
        db.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onSuccess();
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Dodaje nowy budżet do Firestore.
     *
     * @param budget   Obiekt budżetu.
     * @param listener Interfejs zwrotny do obsługi wyniku operacji.
     */
    public void addBudget(Budget budget, OnBudgetOperationListener listener) {
        db.collection(BUDGETS_COLLECTION)
                .add(budget.toMap())
                .addOnSuccessListener(documentReference -> {
                    budget.setId(documentReference.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Pobiera budżet na podstawie jego identyfikatora.
     *
     * @param budgetId Identyfikator budżetu.
     * @param listener Interfejs zwrotny do obsługi wyniku operacji.
     */
    public void getBudgetById(String budgetId, OnBudgetLoadedListener listener) {
        db.collection(BUDGETS_COLLECTION).document(budgetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Budget budget = null;
                    if (documentSnapshot.exists()) {
                        budget = documentSnapshot.toObject(Budget.class);
                        budget.setId(documentSnapshot.getId());
                    }
                    listener.onBudgetLoaded(budget);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Zapisuje lub aktualizuje budżet w Firestore.
     *
     * @param budget   Obiekt budżetu.
     * @param listener Interfejs zwrotny do obsługi wyniku operacji.
     */
    public void saveBudget(Budget budget, OnBudgetSavedListener listener) {
        if (budget == null) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("Budget cannot be null"));
            }
            return;
        }

        if (budget.getId() == null || budget.getId().isEmpty()) {
            db.collection(BUDGETS_COLLECTION)
                    .add(budget.toMap())
                    .addOnSuccessListener(documentReference -> {
                        budget.setId(documentReference.getId());
                        if (listener != null) {
                            listener.onBudgetSaved(budget);
                        }
                    })
                    .addOnFailureListener(listener::onError);
        } else {
            db.collection(BUDGETS_COLLECTION)
                    .document(budget.getId())
                    .update(budget.toMap())
                    .addOnSuccessListener(aVoid -> {
                        if (listener != null) {
                            listener.onBudgetSaved(budget);
                        }
                    })
                    .addOnFailureListener(listener::onError);
        }
    }

    /**
     * Pobiera transakcje użytkownika z Firestore.
     *
     * @param userId   Identyfikator użytkownika.
     * @param listener Interfejs zwrotny do obsługi wyniku operacji.
     */
    public void getTransactions(String userId, OnTransactionsLoadedListener listener) {
        fetchTransactionsFromFirestore(userId, listener, null);
    }

    /**
     * Pobiera kategorie użytkownika z Firestore.
     *
     * @param userId   Identyfikator użytkownika.
     * @param listener Interfejs zwrotny do obsługi wyniku operacji.
     */
    public void getCategories(String userId, OnCategoriesLoadedListener listener) {
        db.collection(CATEGORIES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categories = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Category category = doc.toObject(Category.class);
                        if (category != null) {
                            category.setId(doc.getId());
                            categories.add(category);
                        }
                    }
                    listener.onCategoriesLoaded(categories);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Aktualizuje widżety budżetowe.
     *
     * @param context Kontekst aplikacji.
     */
    public void updateBudgetWidgets(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, can't update widgets");
            return;
        }

        AuthManager authManager = new AuthManager();
        String userId = authManager.getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "User ID is null, can't update widgets");
            return;
        }

        getBudgets(userId, new OnBudgetsLoadedListener() {
            @Override
            public void onBudgetsLoaded(List<Budget> budgets) {
                BudgetWidgetDataHelper dataHelper = new BudgetWidgetDataHelper(context);
                dataHelper.saveAllBudgets(budgets);

                AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                ComponentName widgetComponent = new ComponentName(context, BudgetWidgetProvider.class);
                int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);

                Intent updateIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.setComponent(widgetComponent);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                context.sendBroadcast(updateIntent);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading budgets for widgets", e);
            }
        });
    }
}
