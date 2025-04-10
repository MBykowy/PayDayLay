package com.example.paydaylay.firebase;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.paydaylay.database.AppDatabase;
import com.example.paydaylay.database.TransactionEntity;
import com.example.paydaylay.models.Budget;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.example.paydaylay.widgets.BudgetWidgetProvider;
import com.google.firebase.BuildConfig;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String TAG = "DatabaseManager";
    private final FirebaseFirestore db;
    private final String TRANSACTIONS_COLLECTION = "transactions";
    private final String CATEGORIES_COLLECTION = "categories";
    private final String BUDGETS_COLLECTION = "budgets";
    private final String USERS_COLLECTION = "users";

    public DatabaseManager() {
        db = FirebaseFirestore.getInstance();

        // Configure Firestore for offline persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        db.setFirestoreSettings(settings);
    }

    // Interface for general completion callback
    public interface OnCompletionListener {
        void onSuccess();
        void onError(Exception e);
    }
    // Interface for budget saved operations
    public interface OnBudgetSavedListener {
        void onBudgetSaved(Budget budget);
        void onError(Exception e);
    }

    public static void initAppCheck(Context context) {
        // Just make sure Firebase is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }
        Log.d("DatabaseManager", "Firebase initialized");
    }
    // Interface for user operations
    public interface OnUserOperationListener {
        void onSuccess();
        void onError(Exception e);
    }

    // Interface for transaction operations
    public interface OnTransactionListener {
        void onSuccess();
        void onError(Exception e);
    }

    // Interface for loading transactions
    public interface OnTransactionsLoadedListener {
        void onTransactionsLoaded(List<Transaction> transactions);
        void onError(Exception e);
    }

    // Interface for adding transactions
    public interface OnTransactionAddedListener {
        void onTransactionAdded(Transaction transaction);
        void onError(Exception e);
    }

    // Interface for category operations
    public interface OnCategoryOperationListener {
        void onSuccess();
        void onError(Exception e);
    }

    // Interface for loading categories
    public interface OnCategoriesLoadedListener {
        void onCategoriesLoaded(List<Category> categories);
        void onError(Exception e);
    }

    // Interface for loading budgets
    public interface OnBudgetsLoadedListener {
        void onBudgetsLoaded(List<Budget> budgets);
        void onError(Exception e);
    }

    // Interface for budget operations
    public interface OnBudgetOperationListener {
        void onSuccess();
        void onError(Exception e);
    }

    // Interface for real-time updates
    public interface OnRealtimeTransactionsListener {
        void onTransactionsUpdated(List<Transaction> transactions);
        void onError(Exception e);
    }

    // Create or update user profile
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

    private Map<String, Object> createUserMap(String name, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("createdAt", new Date());
        userMap.put("lastLogin", new Date());
        return userMap;
    }

    // Batch operations for bulk updates/deletions
    public void deleteTransactionsForCategory(String userId, String categoryId, OnTransactionListener listener) {
        // Get all transactions for this category
        db.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onSuccess();
                        return;
                    }

                    // Create a batch operation
                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    // Add delete operations to batch
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    // BUDGET METHODS

    public void addBudget(Budget budget, OnBudgetOperationListener listener) {
        db.collection(BUDGETS_COLLECTION)
                .add(budget.toMap())
                .addOnSuccessListener(documentReference -> {
                    budget.setId(documentReference.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }


    public void getBudgetById(String budgetId, OnBudgetLoadedListener listener) {
        db.collection("budgets").document(budgetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Budget budget = null;
                    if (documentSnapshot.exists()) {
                        budget = documentSnapshot.toObject(Budget.class);
                        budget.setId(documentSnapshot.getId());
                    }
                    listener.onBudgetLoaded(budget);
                })
                .addOnFailureListener(e -> {
                    listener.onError(e);
                });
    }

    public interface OnBudgetLoadedListener {
        void onBudgetLoaded(Budget budget);
        void onError(Exception e);
    }
    public void saveBudget(Budget budget, OnBudgetSavedListener listener) {
        if (budget == null) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("Budget cannot be null"));
            }
            return;
        }

        // Check if this is a new budget or existing one
        if (budget.getId() == null || budget.getId().isEmpty()) {
            // Add new budget
            db.collection(BUDGETS_COLLECTION)
                    .add(budget.toMap())
                    .addOnSuccessListener(documentReference -> {
                        budget.setId(documentReference.getId());
                        if (listener != null) {
                            listener.onBudgetSaved(budget);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            listener.onError(e);
                        }
                    });
        } else {
            // Update existing budget
            db.collection(BUDGETS_COLLECTION)
                    .document(budget.getId())
                    .update(budget.toMap())
                    .addOnSuccessListener(aVoid -> {
                        if (listener != null) {
                            listener.onBudgetSaved(budget);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            listener.onError(e);
                        }
                    });
        }
    }

    public void updateBudget(Budget budget, OnBudgetOperationListener listener) {
        db.collection(BUDGETS_COLLECTION)
                .document(budget.getId())
                .update(budget.toMap())
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void deleteBudget(Budget budget, OnCompletionListener listener) {
        if (budget == null) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("Budget cannot be null"));
            }
            return;
        }

        String budgetId = budget.getId();
        if (budgetId == null || budgetId.isEmpty()) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("Budget ID cannot be null or empty"));
            }
            return;
        }

        db.collection(BUDGETS_COLLECTION)
                .document(budgetId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }
    public void getBudgets(String userId, OnBudgetsLoadedListener listener) {
        db.collection(BUDGETS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Budget> budgets = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Budget budget = doc.toObject(Budget.class);
                        if (budget != null) {
                            budget.setId(doc.getId());
                            budgets.add(budget);
                        }
                    }
                    listener.onBudgetsLoaded(budgets);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getBudgetsByCategory(String userId, String categoryId, OnBudgetsLoadedListener listener) {
        db.collection(BUDGETS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Budget> budgets = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Budget budget = doc.toObject(Budget.class);
                        if (budget != null) {
                            budget.setId(doc.getId());
                            budgets.add(budget);
                        }
                    }
                    listener.onBudgetsLoaded(budgets);
                })
                .addOnFailureListener(listener::onError);
    }

    // TRANSACTION METHODS

    public void addTransaction(Transaction transaction, OnTransactionListener listener) {
        addTransaction(transaction, listener, null);
    }

    public void addTransaction(Transaction transaction, OnTransactionListener listener, Context context) {
        db.collection(TRANSACTIONS_COLLECTION)
                .add(transaction.toMap())
                .addOnSuccessListener(documentReference -> {
                    transaction.setId(documentReference.getId());
                    listener.onSuccess();
                    if (context != null) {
                        updateBudgetWidgets(context);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void addTransaction(Transaction transaction, OnTransactionAddedListener listener) {
        addTransaction(transaction, listener, null);
    }

    public void addTransaction(Transaction transaction, OnTransactionAddedListener listener, Context context) {
        db.collection(TRANSACTIONS_COLLECTION)
                .add(transaction.toMap())
                .addOnSuccessListener(documentReference -> {
                    transaction.setId(documentReference.getId());
                    listener.onTransactionAdded(transaction);
                    if (context != null) {
                        updateBudgetWidgets(context);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateTransaction(Transaction transaction, OnTransactionListener listener) {
        updateTransaction(transaction, listener, null);
    }

    public void updateTransaction(Transaction transaction, OnTransactionListener listener, Context context) {
        db.collection(TRANSACTIONS_COLLECTION)
                .document(transaction.getId())
                .update(transaction.toMap())
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess();
                    if (context != null) {
                        updateBudgetWidgets(context);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void deleteTransaction(String transactionId, OnTransactionListener listener) {
        db.collection(TRANSACTIONS_COLLECTION)
                .document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void getTransactions(String userId, OnTransactionsLoadedListener listener) {
        fetchTransactionsFromFirestore(userId, listener, null);
    }

    public void getTransactions(String userId, OnTransactionsLoadedListener listener, Context context) {
        if (context == null) {
            getTransactions(userId, listener);
            return;
        }

        // First try to get cached data
        new Thread(() -> {
            try {
                List<TransactionEntity> cachedEntities = AppDatabase.getInstance(context)
                        .transactionDao().getTransactionsByUser(userId);

                if (!cachedEntities.isEmpty()) {
                    List<Transaction> transactions = new ArrayList<>();
                    for (TransactionEntity entity : cachedEntities) {
                        transactions.add(entity.toTransaction());
                    }

                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onTransactionsLoaded(transactions));
                }

                // Then fetch from Firestore to ensure up-to-date data
                fetchTransactionsFromFirestore(userId, listener, context);

            } catch (Exception e) {
                Log.e(TAG, "Error getting cached transactions", e);
                // Fall back to Firestore if local db fails
                fetchTransactionsFromFirestore(userId, listener, context);
            }
        }).start();
    }

    private void fetchTransactionsFromFirestore(String userId, OnTransactionsLoadedListener listener, Context context) {
        db.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }

                    // Update the cache if context is provided
                    if (context != null) {
                        updateTransactionCache(userId, transactions, context);
                    }

                    listener.onTransactionsLoaded(transactions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching transactions", e);
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }

    private void fetchTransactionsFromFirestore(String userId, OnTransactionsLoadedListener listener) {
        fetchTransactionsFromFirestore(userId, listener, null);
    }

    private void updateTransactionCache(String userId, List<Transaction> transactions, Context context) {
        new Thread(() -> {
            try {
                List<TransactionEntity> entities = new ArrayList<>();
                for (Transaction transaction : transactions) {
                    entities.add(TransactionEntity.fromTransaction(transaction));
                }

                AppDatabase db = AppDatabase.getInstance(context);
                db.transactionDao().deleteAllByUser(userId);
                db.transactionDao().insertAll(entities);
            } catch (Exception e) {
                Log.e(TAG, "Error updating cache", e);
            }
        }).start();
    }

    public void getTransactionsByDateRange(String userId, Date startDate, Date endDate,
                                           OnTransactionsLoadedListener listener) {
        db.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }
                    listener.onTransactionsLoaded(transactions);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getTransactionsForBudget(String userId, String categoryId, Date startDate, Date endDate,
                                         OnTransactionsLoadedListener listener) {
        db.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThan("date", endDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        if (transaction != null) {
                            transaction.setId(document.getId());
                            transactions.add(transaction);
                        }
                    }
                    listener.onTransactionsLoaded(transactions);
                })
                .addOnFailureListener(listener::onError);
    }

    public com.google.firebase.firestore.ListenerRegistration addTransactionsRealtimeListener(
            String userId, OnRealtimeTransactionsListener listener) {

        return db.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (value != null) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Transaction transaction = doc.toObject(Transaction.class);
                            if (transaction != null) {
                                transaction.setId(doc.getId());
                                transactions.add(transaction);
                            }
                        }
                        listener.onTransactionsUpdated(transactions);
                    }
                });
    }

    // CATEGORY METHODS

    public void addCategory(Category category, OnCategoryOperationListener listener) {
        db.collection(CATEGORIES_COLLECTION)
                .add(category.toMap())
                .addOnSuccessListener(documentReference -> {
                    category.setId(documentReference.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateCategory(Category category, OnCategoryOperationListener listener) {
        db.collection(CATEGORIES_COLLECTION)
                .document(category.getId())
                .update(category.toMap())
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void deleteCategory(String categoryId, OnCategoryOperationListener listener) {
        db.collection(CATEGORIES_COLLECTION)
                .document(categoryId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

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

    // WIDGET METHODS

    public void updateBudgetWidgets(Context context) {
        if (context == null) return;

        // Get AppWidgetManager instance
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Get all widget IDs for the BudgetWidgetProvider
        ComponentName thisWidget = new ComponentName(context, BudgetWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // If there are widgets to update, send broadcast
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            Intent updateIntent = new Intent(context, BudgetWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            context.sendBroadcast(updateIntent);
        }
    }
}