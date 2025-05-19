package com.example.paydaylay.fragments;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
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

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.opencsv.CSVWriter;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment odpowiedzialny za zarządzanie transakcjami użytkownika.
 * Wyświetla listę transakcji, umożliwia ich dodawanie oraz eksportowanie do pliku CSV.
 */
public class TransactionsFragment extends Fragment {

    private RecyclerView recyclerViewTransactions;
    private LinearLayout emptyView;
    private FloatingActionButton fabAddTransaction;
    private TransactionAdapter adapter;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private List<Transaction> transactions;
    private List<Category> categories;
    private ProgressBar progressBar;

    /**
     * Wywoływane podczas tworzenia fragmentu.
     * Ustawia, że fragment ma własne menu opcji.
     *
     * @param savedInstanceState Zapisany stan fragmentu.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

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
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        // Inicjalizacja widoków
        recyclerViewTransactions = view.findViewById(R.id.recyclerViewTransactions);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction);
        progressBar = view.findViewById(R.id.progressBar);

        // Inicjalizacja menedżerów
        databaseManager = new DatabaseManager();
        authManager = new AuthManager();

        // Inicjalizacja list
        transactions = new ArrayList<>();
        categories = new ArrayList<>();

        // Konfiguracja RecyclerView
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(getContext(), transactions, categories);
        recyclerViewTransactions.setAdapter(adapter);

        // Konfiguracja FAB
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TransactionActivity.class);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Wywoływane po wznowieniu fragmentu.
     * Ładuje dane transakcji i kategorii.
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

                        transactions = loadedTransactions;
                        adapter.updateData(transactions, categories);

                        // Wyświetla widok pusty, jeśli brak transakcji
                        if (transactions.isEmpty()) {
                            recyclerViewTransactions.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerViewTransactions.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() == null) return;
                        Toast.makeText(getActivity(),
                                "Error loading transactions: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                Toast.makeText(getActivity(),
                        "Error loading categories: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Tworzy menu opcji dla fragmentu.
     *
     * @param menu     Obiekt menu.
     * @param inflater Obiekt inflatera menu.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_transactions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Obsługuje wybór elementu z menu opcji.
     *
     * @param item Wybrany element menu.
     * @return True, jeśli element został obsłużony, w przeciwnym razie false.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export_csv) {
            showExportCsvDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Wyświetla dialog eksportu transakcji do pliku CSV.
     */
    private void showExportCsvDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export Transactions")
                .setMessage("How would you like to export your transactions?")
                .setPositiveButton("Share", (dialog, which) -> {
                    exportTransactionsToCsv(true);
                })
                .setNegativeButton("Save to Downloads", (dialog, which) -> {
                    exportTransactionsToCsv(false);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    /**
     * Eksportuje transakcje do pliku CSV.
     *
     * @param share True, jeśli plik ma być udostępniony, false, jeśli zapisany lokalnie.
     */
    private void exportTransactionsToCsv(boolean share) {
        if (transactions.isEmpty()) {
            showMessage("No transactions to export");
            return;
        }

        showLoading(true);

        // Pobiera katalog docelowy
        File directory;
        if (share) {
            directory = new File(requireContext().getExternalFilesDir(null), "PayDayLay");
        } else {
            directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PayDayLay");
        }

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "transactions_" + System.currentTimeMillis() + ".csv";
        File file = new File(directory, fileName);

        new Thread(() -> {
            try {
                // Tworzy plik CSV z nagłówkiem i danymi
                FileWriter fileWriter = new FileWriter(file);
                CSVWriter csvWriter = new CSVWriter(fileWriter);

                // Nagłówek
                String[] header = {"Date", "Amount", "Category", "Description", "Type"};
                csvWriter.writeNext(header);

                // Dane transakcji
                Map<String, String> categoryMap = new HashMap<>();
                for (Category category : categories) {
                    categoryMap.put(category.getId(), category.getName());
                }

                for (Transaction transaction : transactions) {
                    String categoryName = categoryMap.getOrDefault(transaction.getCategoryId(), "Unknown");
                    String[] data = {
                            new SimpleDateFormat("yyyy-MM-dd").format(transaction.getDate()),
                            String.valueOf(transaction.getAmount()),
                            categoryName,
                            transaction.getDescription(),
                            transaction.isExpense() ? "Expense" : "Income"
                    };
                    csvWriter.writeNext(data);
                }

                csvWriter.close();

                // Udostępnia lub powiadamia o zapisanym pliku
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    if (share) {
                        shareFile(file);
                    } else {
                        MediaScannerConnection.scanFile(requireContext(),
                                new String[]{file.getAbsolutePath()}, null, null);
                        showMessage(getString(R.string.csv_exported_successfully, file.getAbsolutePath()));
                    }
                });
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showError("Export failed: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Udostępnia plik CSV.
     *
     * @param file Plik do udostępnienia.
     */
    private void shareFile(File file) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                requireContext(),
                "com.example.paydaylay.provider",
                file));
        shareIntent.setType("text/csv");
        startActivity(Intent.createChooser(shareIntent, "Share CSV file"));
    }

    /**
     * Wyświetla komunikat o błędzie.
     *
     * @param message Treść komunikatu błędu.
     */
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Wyświetla komunikat.
     *
     * @param message Treść komunikatu.
     */
    private void showMessage(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Wyświetla lub ukrywa wskaźnik ładowania.
     *
     * @param isLoading True, jeśli wskaźnik ma być widoczny, false w przeciwnym razie.
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }
}