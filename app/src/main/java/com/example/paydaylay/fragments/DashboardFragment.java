package com.example.paydaylay.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.paydaylay.R;
import com.example.paydaylay.activities.TransactionActivity;
import com.example.paydaylay.adapters.TransactionAdapter;
import com.example.paydaylay.firebase.AuthManager;
import com.example.paydaylay.firebase.DatabaseManager;
import com.example.paydaylay.models.Category;
import com.example.paydaylay.models.Transaction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView textBalance;
    private RecyclerView recyclerRecentTransactions;
    private FloatingActionButton fabAddTransaction;
    private TransactionAdapter adapter;

    private List<Transaction> recentTransactions = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private DatabaseManager databaseManager;
    private AuthManager authManager;

    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Initialize views
        textBalance = view.findViewById(R.id.text_balance);
        recyclerRecentTransactions = view.findViewById(R.id.recycler_recent_transactions);
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction);

        // Initialize managers
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Setup RecyclerView
        recyclerRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(getContext(), recentTransactions, categories);
        recyclerRecentTransactions.setAdapter(adapter);

        // Setup FAB
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TransactionActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        if (getActivity() == null) return;

        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        // First load categories
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories = loadedCategories;

                // Then load transactions
                databaseManager.getTransactions(userId, new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> loadedTransactions) {
                        if (getActivity() == null) return;

                        // Calculate balance
                        double balance = calculateBalance(loadedTransactions);
                        textBalance.setText(currencyFormat.format(balance));

                        // Get recent transactions (up to 5)
                        recentTransactions = getRecentTransactions(loadedTransactions, 5);
                        adapter.updateData(recentTransactions, categories);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() == null) return;
                        showError("Error loading transactions: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                showError("Error loading categories: " + e.getMessage());
            }
        });
    }

    private double calculateBalance(List<Transaction> transactions) {
        double balance = 0;
        for (Transaction transaction : transactions) {
            if (transaction.isExpense()) {
                balance -= transaction.getAmount();
            } else {
                balance += transaction.getAmount();
            }
        }
        return balance;
    }

    private List<Transaction> getRecentTransactions(List<Transaction> allTransactions, int limit) {
        List<Transaction> sorted = new ArrayList<>(allTransactions);

        // Sort by date (newest first)
        Collections.sort(sorted, (t1, t2) -> t2.getDate().compareTo(t1.getDate()));

        // Return only the specified number of transactions
        if (sorted.size() <= limit) {
            return sorted;
        } else {
            return sorted.subList(0, limit);
        }
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}