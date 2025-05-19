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
import java.util.List;
import java.util.Locale;

/**
 * Fragment odpowiedzialny za wyświetlanie pulpitu użytkownika.
 * Wyświetla aktualny bilans oraz listę ostatnich transakcji.
 * Umożliwia dodawanie nowych transakcji.
 */
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

    /**
     * Tworzy widok fragmentu.
     *
     * @param inflater  Obiekt LayoutInflater do tworzenia widoków.
     * @param container Kontener, w którym znajduje się fragment.
     * @param savedInstanceState Zapisany stan fragmentu.
     * @return Widok fragmentu.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Inicjalizacja widoków
        textBalance = view.findViewById(R.id.text_balance);
        recyclerRecentTransactions = view.findViewById(R.id.recycler_recent_transactions);
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction);

        // Inicjalizacja menedżerów
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Konfiguracja RecyclerView
        recyclerRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(getContext(), recentTransactions, categories);
        recyclerRecentTransactions.setAdapter(adapter);

        // Konfiguracja FAB
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TransactionActivity.class);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Wywoływane po wznowieniu fragmentu.
     * Ładuje dane do wyświetlenia na pulpicie.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    /**
     * Ładuje dane użytkownika, w tym kategorie i transakcje.
     */
    private void loadData() {
        if (getActivity() == null) return;

        String userId = authManager.getCurrentUserId();
        if (userId == null) return;

        // Najpierw ładuje kategorie
        databaseManager.getCategories(userId, new DatabaseManager.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Category> loadedCategories) {
                categories = loadedCategories;

                // Następnie ładuje transakcje
                databaseManager.getTransactions(userId, new DatabaseManager.OnTransactionsLoadedListener() {
                    @Override
                    public void onTransactionsLoaded(List<Transaction> loadedTransactions) {
                        if (getActivity() == null) return;

                        // Oblicza bilans
                        double balance = calculateBalance(loadedTransactions);
                        textBalance.setText(currencyFormat.format(balance));

                        // Pobiera ostatnie transakcje (do 5)
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

    /**
     * Oblicza bilans na podstawie listy transakcji.
     *
     * @param transactions Lista transakcji.
     * @return Obliczony bilans.
     */
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

    /**
     * Pobiera ostatnie transakcje z listy.
     *
     * @param allTransactions Lista wszystkich transakcji.
     * @param limit Maksymalna liczba transakcji do zwrócenia.
     * @return Lista ostatnich transakcji.
     */
    private List<Transaction> getRecentTransactions(List<Transaction> allTransactions, int limit) {
        List<Transaction> sorted = new ArrayList<>(allTransactions);

        // Sortuje transakcje według daty (od najnowszych)
        Collections.sort(sorted, (t1, t2) -> t2.getDate().compareTo(t1.getDate()));

        // Zwraca tylko określoną liczbę transakcji
        if (sorted.size() <= limit) {
            return sorted;
        } else {
            return sorted.subList(0, limit);
        }
    }

    /**
     * Wyświetla komunikat o błędzie.
     *
     * @param message Treść komunikatu błędu.
     */
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}